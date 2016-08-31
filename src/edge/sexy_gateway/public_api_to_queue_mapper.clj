(ns edge.sexy-gateway.public-api-to-queue-mapper
  (:require
   [bidi.bidi :as bidi]
   [clojure.tools.logging :refer :all]
   [schema.core :as s]
   [yada.swagger :as swagger]
   [yada.yada :as yada]
   [edge.sexy-gateway.restmq :as restmq]))

(defn transform-keywords-in-body-spec-to-primitives [body-spec]
  (reduce-kv (fn [mp k v]
    (if (= v String)
      (assoc mp v schema.core/Str)))
        {} body-spec))

(defn add-get-method [config resource-map method-map]
  (if (:GET (:allowed-operations resource-map))
    (assoc
      method-map
      :get
      {:response
        (fn [ctx]
          (let [id (get-in ctx [:parameters :path :id])]
            (restmq/enqueue-get-message!
              config
              (:resource resource-map)
              id)))})
     method-map))

(defn add-delete-method [config resource-map method-map]
  (if (:DELETE (:allowed-operations resource-map))
    (assoc
      method-map
      :delete
      {:response
        (fn [ctx]
          (let [id (get-in ctx [:parameters :path :id])]
            (restmq/enqueue-get-message!
              config
              (:resource resource-map)
              id)))})
    method-map))

(defn add-put-method [config resource-map method-map]
  (if (:PUT (:allowed-operations resource-map))
    (assoc
      method-map
      :put
      {:parameters {:body (transform-keywords-in-body-spec-to-primitives (:body resource-map))
                    :path {:id Long}}
       :consumes [{:media-type #{"application/json" "application/edn"}}]
       :response
          (fn [ctx]
            (let [id (get-in ctx [:parameters :path :id])]
              (restmq/enqueue-delete-message!
                config
                (:resource resource-map)
                id)))})
     method-map))

(defn add-post-method [config resource-map method-map]
  (if (:POST (:allowed-operations resource-map))
    (assoc
      method-map
      :post
        {:parameters {:body (transform-keywords-in-body-spec-to-primitives (:body resource-map))
                      :path {:id Long}}
            :consumes [{:media-type #{"application/json" "application/edn"}}]
            :response
              (fn [ctx]
                (restmq/enqueue-post-message!
                  config
                  (:resource resource-map)
                  (:body ctx)))})
          method-map))


(defn create-resource [config resource-map]
   (yada/resource
     {:id (keyword (:resource resource-map))
      :swagger/tags [(:resource resource-map)]
      :description (:description resource-map)
      :parameters {:path {:id Long}}
      :produces [{:media-type #{"application/json" "application/edn"}}]
      :methods
        (add-post-method config resource-map
          (add-put-method config resource-map
            (add-delete-method config resource-map
              (add-get-method config resource-map {}))))
    }))



(defn create-routes-for-resource [config resource-map]
   [ (str "/" (:resource resource-map)) (create-resource config resource-map) ])
