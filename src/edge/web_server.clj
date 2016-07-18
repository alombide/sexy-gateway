;; Copyright © 2016, JUXT LTD.

(ns edge.web-server
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.bidi :as bidi]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
   [edge.sources :refer [source-routes]]
   [edge.phonebook :refer [phonebook-routes]]
   [edge.sexy-gateway.public-api-to-queue-mapper :refer [create-routes-for-resource]]
   [edge.phonebook-app :refer [phonebook-app-routes]]
   [edge.hello :refer [hello-routes other-hello-routes]]
   [schema.core :as s]
   [selmer.parser :as selmer]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]
   [yada.swagger :as swagger]))

(defn content-routes []
  ["/"
   [
    ["index.html"
     (yada/resource
      {:id :edge.resources/index
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx]
                     (selmer/render-file "index.html" {:title "Edge Index"
                                                       :ctx ctx}))}}})]

    ["" (assoc (yada/redirect :edge.resources/index) :id :edge.resources/content)]

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.
    ;; Here's an example

    [""
     (-> (yada/as-resource (io/file "target"))
         (assoc :id :edge.resources/static))]]])


(defn create-routes [config]
  (let [resources (:resources config)]
    (mapv
      (fn [resource] (create-routes-for-resource config resource))
      resources)))



(defn routes
  "Create the URI route structure for our application."
  [config {:keys [port]}]
  (let [routes (create-routes config)]
  [""
   [
    ;; Hello World!
    ;(hello-routes)
    ;(other-hello-routes)

    ;(phonebook-routes db config)
    ;(phonebook-app-routes db config)
    ["/sexy-gateway-api/swagger.json"
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
                :tags [{:name "sexy-gateway"
                              ;:description (str "All paths for resource " (:resource resource-map))}
                              :description "All paths " }
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
        :edge.resources/sexy-gateway-swagger)]

    ;; The Edge source code is served for convenience
    (source-routes)

    ;; Our content routes, and potentially other routes.
    ;(content-routes)

    ;; This is a backstop. Always produce a 404 if we ge there. This
    ;; ensures we never pass nil back to Aleph.
    [true (handler nil)]]]))



(s/defrecord WebServer [config
                        listener]
  Lifecycle
  (start [component]
    (let [port (:port (:web-server config))]
      (if listener
        component                         ; idempotence
        (let [vhosts-model
            (vhosts-model
              [{:scheme :http :host (format "localhost:%d" port)}
                (routes config {:port port})])
              listener (yada/listener vhosts-model {:port port})]
            (infof "Started web-server on port %s" (:port listener))
            (assoc component :listener listener)))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (dissoc component :listener)))

(defn new-web-server [config]
  (using
   (map->WebServer {:config config})
   []))
