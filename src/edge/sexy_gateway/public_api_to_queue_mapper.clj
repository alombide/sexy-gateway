(ns edge.sexy-gateway.public-api-to-queue-mapper
  (:require
   [bidi.bidi :as bidi]
   [clojure.tools.logging :refer :all]
   [schema.core :as s]
   [yada.swagger :as swagger]
   [yada.yada :as yada]
   [edge.sexy-gateway.restmq :as restmq]))


(defn create-get-resource [config resource-map]
   {:swagger/tags ["all" "GET" (:resource resource-map)]
    :produces
      [{:media-type #{"application/json"
                      "application/edn"}}]
    :response
    (fn [ctx]
      (let [id (get-in ctx [:parameters :path :id])]
        (restmq/enqueue-get-message!
          config
          (:resource resource-map)
          id))) })

(defn create-put-resource [config resource-map]
  {:swagger/tags ["all" "PUT" (:resource resource-map)]
   :parameters
    {:body (:body resource-map)}
   :consumes
    [{:media-type #{"application/json"
                    "application/edn"}}]
   :produces
      [{:media-type #{"application/json"
                      "application/edn"}}]
   :response
      (fn [ctx]
        (let [id (get-in ctx [:parameters :path :id])]
          (restmq/enqueue-put-message!
            config
            (:resource resource-map)
            id
            (:body ctx))))})

(defn create-delete-resource [config resource-map]
 {:swagger/tags ["all" "DELETE" (:resource resource-map)]
  :produces
    [{:media-type #{"application/json"
                    "application/edn"}}]
  :response
   (fn [ctx]
    (let [id (get-in ctx [:parameters :path :id])]
     (restmq/enqueue-delete-message!
       config
       (:resource resource-map)
       id)))})


(defn create-post-resource [config resource-map]
  { :swagger/tags ["all" "POST" (:resource resource-map)]
    :parameters {:body (:body resource-map)}
    :consumes [{:media-type #{"application/json"
                              "application/edn"}}]
    :produces
              [{:media-type #{"application/json"
                              "application/edn"}}]
    :response (fn [ctx]
                (restmq/enqueue-post-message!
                  config
                  (:resource resource-map)
                  (:body ctx))) })



(defn create-resource [config resource-map]
  (let [temp-map

   {:id (keyword (str "sexy-gateway.resources/" (:resource resource-map)))
    :description (:description resource-map)
    :parameters {:path {:id Long}}
    :produces [{:media-type #{"application/edn;q=0.9"
                              "application/json;q=0.8"
                              "application/transit+json;q=0.7"}
                :charset "UTF-8"}]
    :methods {}

    :responses {404 {:produces #{"application/json"}
                     :response (fn [ctx]
                                 (let [id (get-in ctx [:parameters :path :id])]
                                  {:resource (:resource resource-map)
                                   :id id
                                   :message "Not found."}))}}}

      operations (:allowed-operations resource-map)
      methods {}]

    (if (:GET operations)
      (let [methods (assoc methods :get (create-get-resource config resource-map))]
        (if (:PUT operations)
          (let [methods (assoc methods :put (create-put-resource config resource-map))]
            (if (:DELETE operations)
              (let [methods (assoc methods :delete (create-delete-resource config resource-map))]
                (if (:POST operations)
                  (let [methods (assoc methods :post (create-post-resource config resource-map))]
                    (assoc temp-map :methods methods)))))))))))


(defn create-routes-for-resource [config resource-map]
  (let [routes ["/sexy-gateway"
       [
        ;; Phonebook entry, with path parameter
        [["/" :id] (create-resource config resource-map)]]]]
    [""
      [
        routes]]))
