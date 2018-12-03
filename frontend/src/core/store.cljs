(ns core.store
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [rum.core :as rum]
              [citrus.core :as citrus]
              [cljs-http.client :as http]
              [cljs.core.async :refer [<!]]
              [bide.core :as r]))

(declare dispatch!)

;;
;; define controller & event handlers
;;

(defmulti counter (fn [event] event))
(defmethod counter :init []
  {:state 0})

(defmethod counter :inc [_ _ state]
  {:state (inc state)})

(defmethod counter :dec [_ _ state]
  {:state (dec state)})


(defmulti film-control identity)
(defmethod film-control :init [_ _ _]
  {:state []})

(defmethod film-control :get [_ _ state]
  (if (empty? state)
    {:http {:url "http://localhost:3000/movies"
            :method :get
            :headers {"Content-Type" "application/json"}
            :on-success :actors-ready
            :on-error :actors-failed}}))

(defmethod film-control :movies-ready [_ [args] _]
  {:state (js->clj (js/JSON.parse (first args)))})

(defmethod film-control :movies-failed [_ _ state]
  (println "movies failed")
  {:state []})

(defn http [reconciler ctrl-name effect]
  (let [{:keys [on-success on-error url]} effect]
    (go (let [response (<! (http/get url {:with-credentials? false}))]
          (dispatch! ctrl-name :movies-ready (:body response))))
    nil))

(def router
  (r/router [["/" :app/index]
             ["/contact" :app/contact]]))

(defmulti route-control identity)

(defmethod route-control :init []
  {:state {:handler (or (first (bide.core/match router js/window.location.pathname))
                        :app/index)}})

(defmethod route-control :push [_ [route] _]
  {:state (first route)})

(defmulti post! identity)
(defmethod post! :contact-form [x params y z]
  (let [{:keys [url]} params]
    (go (let [res (<! (http/post "http://localhost:3000/contact"
                                 {:with-credentials? false
                                  :form-params params}))]
          res))))


;;
;; start up
;;

;; create Reconciler instance
(defonce reconciler
  (citrus/reconciler
     ;; application state
    {:state (atom {})
     ;; controllers
     :controllers {:router route-control
                   :film film-control}
     :effect-handlers {:http http}}))

;; initialize controllers
(defonce init-ctrl (citrus/broadcast-sync! reconciler :init))

;; Utility functions
(defn dispatch! [key action & values]
  (citrus/dispatch! reconciler key action values))

(defn fetch! [key]
  {:will-mount (fn [state]
                 (dispatch! key :get)
                 state)})

(defn subscription [keys]
  (rum/react (citrus/subscription reconciler keys)))

(defn goto! [route & [args]]
  (r/navigate! router route args))

(defn on-navigate
  "A function which will be called on each route change."
  [name params query]
  (println "Route change to: " name params query)
  (dispatch! :router :push {:handler name :params params :query query}))

(r/start! router {:default :app/index
                  :on-navigate on-navigate
                  :html5? true})