(ns core.server
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]))

(defroutes app-routes
  ;; NOTE: this will deliver all of your assets from the public directory
  ;; of resources i.e. resources/public
  (route/resources "/" {:root "public"})
  ;; NOTE: this will deliver your index.html
  (GET "*" [] (-> (response/resource-response "index.html" {:root "public"})
                  (response/content-type "text/html")))
  (route/not-found (-> (response/resource-response "index.html" {:root "public"})
                       (response/content-type "text/html"))))

;; NOTE: wrap reload isn't needed when the clj sources are watched by figwheel
;; but it's very good to know about
(def dev-app (wrap-reload (wrap-defaults #'app-routes site-defaults)))
