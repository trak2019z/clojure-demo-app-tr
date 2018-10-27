(ns api.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.json :as middleware]
            [ring.middleware.cors :refer [wrap-cors]]
            [oksql.core :as oksql]))


(def db {:connection-uri "jdbc:postgresql://localhost:5432/dvdrental"})

(def query (partial oksql/query db))

(defn all []
  (query :film/all))

(defn handle-contact [req]
  (println "KONTAKT" (:params req)))

(defroutes app-routes
  (GET "/movies" [] (all))
  (POST "/contact" request (handle-contact request))
  (route/not-found "Not Found"))

(def app
  (-> (handler/api (wrap-cors app-routes
                              :access-control-allow-origin [#"http://localhost:3449"]
                              :access-control-allow-methods [:get :post]))
      (middleware/wrap-json-body)
      (middleware/wrap-json-response)))
