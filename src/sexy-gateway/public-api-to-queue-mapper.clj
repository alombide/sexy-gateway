(ns sexy-gateway.public-api-to-queue-mapper
  (:require
   [bidi.bidi :as bidi]
   [clojure.tools.logging :refer :all]
   [schema.core :as s]
   [yada.swagger :as swagger]
   [yada.yada :as yada]
   [sexy-gateway.restmq :as restmq]))


(defn create-get-resource [resource-map]
   {:swagger/tags ["all" "GET" (:resource resource-map)]
    :produces
      [{:media-type #{"application/json"
                      "application/edn"}}]
    :response
    (fn [ctx]
      (let [id (get-in ctx [:parameters :path :id])
        (restmq/enqueue-get-message! (:resource resource-map) id))) })

(defn create-put-resource [resource-map]
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
        (let [(get-in ctx [:parameters :path :id])]
          (restmq/enqueue-put-message! (:resource resource-map) id (:body ctx))))})

(defn create-delete-resource [resource-map]
 {:swagger/tags ["all" "DELETE" (:resource resource-map)]
  :produces
    [{:media-type #{"application/json"
                    "application/edn"}}]
  :response
   (fn [ctx]
    (let [id (get-in ctx [:parameters :path :id])]
     (restmq/enqueue-delete-message! (:resource resource-map) id)))})


(defn create-post-resource [resource-map]
  { :swagger/tags ["all" "POST" (:resource resource-map)]
    :parameters {:body (:body resource-map)}
    :consumes [{:media-type #{"application/json"
                              "application/edn"}}]
    :produces
              [{:media-type #{"application/json"
                              "application/edn"}}]
    :response (fn [ctx]
                (restmq/enqueue-post-message! (:resource resource-map) id (:body ctx))) })



(defn create-resource [resource-map]
  (let [temp-map

   {:id (keyword (str "sexy-gateway.resources/" (:resource resource-map)))
    :description (:description resource-map)
    :parameters {:path {:id Long}}
    :produces [{:media-type #{"application/edn;q=0.9"
                              "application/json;q=0.8"
                              "application/transit+json;q=0.7"}
                :charset "UTF-8"}]
    :methods
    {:get (create-get-resource resource-map)
     :put (create-put-resource resource-map)
     :delete (create-delete-resource resource-map) }

    :responses {404 {:produces #{"application/json"}
                     :response (fn [ctx]
                                 (let [id (get-in ctx [:parameters :path :id])]
                                  {:resource (:resource resource-map)
                                   :id id
                                   :message "Not found."}))}}}

      operations (:allowed-operations resource-map)
      methods {}]

    (if (:GET operations)
      (let [methods (assoc methods :get (create-get-resource resource-map))]
        (if (:PUT operations)
          (let [methods (assoc methods :put (create-put-resource resource-map))]
            (if (:DELETE operations)
              (let [methods (assoc methods :delete (create-delete-resource resource-map))]
                (if (:POST operations)
                  (let [methods (assoc methods :post (create-post-resource resource-map))]
                    (assoc temp-map :methods methods)))))))))))


(defn create-routes-for-resource [resource-map {:keys [port]}]
  (let [routes ["/phonebook"
       [
        ;; Phonebook entry, with path parameter
        [["/" :id] (create-resource  resource-map)]]]]
    [""
      [
        routes

        ;; Swagger
        ["/phonebook-api/swagger.json"
          (bidi/tag
            (yada/handler
              (swagger/swagger-spec-resource
                (swagger/swagger-spec
                  routes
                  {:info {:title "Public API"
                          :version "0.1"
                          :description "Our public API"}
                          :host (format "localhost:%d" port)
                          :schemes ["http"]
                          :tags [{:name (:resource resource-map)
                                  :description (str "All paths for resource " (:resource resource-map))}
                                 {:name "GET"
                                  :description "All paths that support GET"}
                                 {:name "PUT"
                                  :description "All paths that support PUT"}
                                 {:name "DELETE"
                                  :description "All paths that support DELETE"}
                                 {:name "POST"
                                  :description "All paths that support POST"}
                                 {:name "all"
                                  :description "All paths"}]
                                :basePath ""})))
            (keyword (str "sexy-gateway.resources/" (:resource resource-map))))]]]))



(def example-resource-map
  {
    :resource :phonebook
    :description "Phonebook resource"
    :allowed-operations {:GET true :PUT true :POST true :DELETE true }
    :body { :surname String
            :firstname String
            :phone String }
    })
