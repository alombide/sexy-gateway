(ns edge.sexy-gateway.public-api-to-queue-mapper
  (:require
   [bidi.bidi :as bidi]
   [clojure.tools.logging :refer :all]
   [schema.core :as s]
   [yada.swagger :as swagger]
   [yada.yada :as yada]
   [edge.sexy-gateway.restmq :as restmq]))


(defn transform-keywords-in-body-spec-to-primitives [body-spec]
  (let [params (keys body-spec)
        result {}]
    (letfn [
      (traverse [remaining-params result-map]
        (if (empty? remaining-params)
          result-map
          (let [current-param (first remaining-params)
                current-keyword (current-param body-spec)]
            (traverse (next remaining-params) (assoc
                                                result-map
                                                current-param
                                                (if (= current-keyword String) s/Str current-keyword))))))]
      (traverse params result))))


(defn add-get-method [config resource-map method-map]
  (if (:GET (:allowed-operations resource-map))
    (assoc
      method-map
      :get
      {:parameters {:path {:id Long}}
       :response
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
      {:parameters {:path {:id Long}}
       :response
        (fn [ctx]
          (let [id (get-in ctx [:parameters :path :id])]
            (restmq/enqueue-get-message!
              config
              (:resource resource-map)
              id)))})
    method-map))

(defn add-put-method [config resource-map method-map]
  (println (:body resource-map))
  (if (:PUT (:allowed-operations resource-map))
    (assoc
      method-map
      :put
      {:parameters {:body (:body resource-map)
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
        {:parameters {:body (:body resource-map)
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
      ;:parameters {:path {:id Long}}
      :produces [{:media-type #{"application/json" "application/edn"}}]
      :methods
        (add-post-method config resource-map
          (add-put-method config resource-map
            (add-delete-method config resource-map
              (add-get-method config resource-map {}))))
    }))



(defn create-routes-for-resource [config resource-map]
   [ (str "/" (:resource resource-map)) (create-resource config resource-map) ])
