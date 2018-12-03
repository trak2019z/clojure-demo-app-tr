(ns frontend.core
    (:require [rum.core :as rum]
              [core.store :as s]))

(enable-console-print!)

(println "This text is printed from src/frontend/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(rum/defcs counter <
  rum/reactive
  []
  (let [counter (s/subscription [:counter])]
    [:div.count
     [:div.button {:on-click #(s/dispatch! :counter :inc)} "+"]
     [:span {} counter]
     [:div.button {:on-click #(s/dispatch! :counter :dec)} "-"]]))

(defn prepare-films [films sorter groupper]
  (let [sort-column (case (first sorter)
                      :alphabetical "title"
                      :reverse-alphabetical "title"
                      :date "release-year"
                      :length "length"
                      :reverse-length "length"
                      nil)
        group-column (case groupper
                       :rating "rating"
                       :alphabetical "title"
                       nil)
        films (cond->> films
                sort-column (sort-by #(get % sort-column) (second sorter))
                group-column (group-by #(cond-> (get % group-column)
                                         (= group-column "title") first))
                group-column (sort-by first))]
    (if group-column
      films
      {"Wszystkie" films})))

(rum/defcs films <
  (rum/local [] :sort-by)
  (rum/local nil :group-by)
  rum/reactive
  (s/fetch! :film)
  [state]
  (let [films (s/subscription [:film])
        current-sorter @(:sort-by state)
        current-groupper @(:group-by state)
        grouppers [[nil "Brak"] [:rating "Rating"][:alphabetical "Alfabetycznie"]]
        sorters [[nil "Brak"] [:alphabetical "Alfabetycznie"] [:reverse-alphabetical "-" >]
                 [:date "Data wydania"] [:length "Długość"][:reverse-length "-" >]]
        prepared-films (prepare-films films current-sorter current-groupper)]
    [:div.hp
     [:div.hp_filters
      [:h3 {} "Sortuj"]
      [:div.buttons-group
       (for [[sort-column text reverse?] sorters]
         [:div.button {:class [(if reverse? "reverse")
                               (if (= sort-column (first current-sorter)) "active")]
                       :key sort-column
                       :on-click #(reset! (:sort-by state) [sort-column (or reverse? <)])}
          text])]
      [:h3 {} "Grupuj"]
      [:div.buttons-group
       (for [[group-column text] grouppers]
         [:div.button {:class (if (= group-column current-groupper) "active")
                       :key group-column
                       :on-click #(reset! (:group-by state) group-column)}
          text])]]
     [:div.hp_films {}
      (for [[group films] prepared-films]
        [:div {:key group}
         [:h3 {} group]
         (for [film films]
           [:div.film {:key (get film "film-id")}
            [:h2 {} (get film "title" "N/A")]
            [:p {} (get film "description" "N/A")]
            [:small "Data wydania: " (get film "release-year" "N/A") " r."][:br]
            [:small "Długość: " (get film "length" "N/A")][:br]
            [:small "Rating: " (get film "rating" "N/A")][:br]])])]]))

(rum/defc input
  [atom key label params]
  [:label.form-input
   [:span {} label]
   [(or (:tag params) :input)
    (merge {:type "text"
            :value (or (key @atom) "")
            :on-change #(swap! atom assoc key (-> % .-target .-value))}
           (dissoc params :tag))]])

(rum/defcs contact <
  (rum/local {} :form)
  [state]
  (let [form (:form state)
        form-data @form]
    [:div.contact
     [:h2 {} "Kontakt"]
     [:form {:on-submit (fn [ev]
                          (.preventDefault ev)
                          (s/post! :contact-form form-data)
                          (reset! form {}))}
      (input form :name "Adresat")
      (input form :phone "Numer telefonu" {:type :text :placeholder "000-000-000"})
      (input form :message "Wiadomość" {:tag :textarea :length 1000})
      [:input.button {:type :submit :value "Wyślij"}]
      [:div
       [:h3 "Podgląd:"]
       [:p "Adresat: " (:name form-data)]
       [:p "Numer telefonu: " (:phone form-data)]
       [:p "Wiadomość: " (:message form-data)]]]]))

(defn menu [curr-route params]
  (let [routes [[:app/index "List filmów"] [:app/contact "Kontakt"]]]
    [:nav.menu.buttons-group {}
     (for [[route text] routes]
       [:a.button {:class (if (= curr-route route) "active")
                   :on-click #(s/goto! route) :key route}
        text])]))

(rum/defcs wrapper <
  rum/reactive
  [state]
  (let [{route :handler params :params} (s/subscription [:router])]
    [:div#wrapper
     (menu route params)

     (case route
       :app/index (films)
       :app/contact (contact)
       "Nie znaleziono")]))

(rum/mount (wrapper)
           (. js/document (getElementById "app")))

(defn on-js-reload [])
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

