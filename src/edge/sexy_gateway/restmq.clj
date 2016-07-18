(ns edge.sexy-gateway.restmq
  (:require
   [http.async.client :as http]))


(defn create-resource-url [config resource]
  (str (:restmq-host config) "/q/" resource))

(defn enqueue-get-message! [config resource id]
  (with-open [client (http/create-client)]
    (http/POST
      client
      (create-resource-url config resource)
      { :body
        { :id id
          :request "GET" }
       })))


(defn enqueue-put-message! [config resource id body]
  (with-open [client (http/create-client)]
    (http/POST
      client
      (create-resource-url config resource)
      { :body
        { :id id
          :request "PUT"
          :body body }
       })))


(defn enqueue-post-message! [config resource body]
  (with-open [client (http/create-client)]
    (http/POST
      client
      (create-resource-url config resource)
      { :body
        { :request "POST"
          :body body }
       })))


(defn enqueue-delete-message! [config resource id]
  (with-open [client (http/create-client)]
  (http/POST
    client
    (create-resource-url config resource)
    { :body
      { :id id
        :request "DELETE" }
     })))
