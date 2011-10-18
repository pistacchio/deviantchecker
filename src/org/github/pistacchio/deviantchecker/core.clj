(ns org.github.pistacchio.deviantchecker.core
  (:use compojure.core
        ring.adapter.jetty
        org.github.pistacchio.deviantchecker.scraper)
  (:use ring.middleware.stacktrace)
  (:use net.cgrand.enlive-html)
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:gen-class))

(def *data-file* "resources/data/data.dat")

;; ** data management ** ;;
(defn load-data
  "returns a gallery database stored in *data-file*"
  []
  (load-file *data-file*))

(defn save-data
  "serializes data to *data-file* like"
  [data]
  (with-open [f (java.io.FileWriter. *data-file*)]
    (print-dup (into [] data) f)))

;; ** utilities ** ;;
(defn emit**
  "given an enlive form, returns a string"
  [tpl]
  (apply str (emit* tpl)))

(defn get-template
  "returns a enlive template object loaded from the file tpl"
  [tpl]
  "")

;; ** views ** ;;

(defn get-home
  "GET /"
  [& error-message]
  (let [tpl (html-resource (java.io.File. "resources/tpl/home.html"))
        data (load-data)
        main-transform #(transform % [:li.gallery]
                          (clone-for [g data]
                            [:a.url] (set-attr :href (g :href))
                            [:a.url] (content (g :href))
                            [:a.delete] (set-attr :href (str "/delete?d=" (g :href)))))
        error-div (first error-message)
        error-text (second error-message)
        error-transform (fn [t s m]  (if (empty? error-message) t (transform t [s] (content m))))]
    (emit** (-> tpl main-transform (error-transform error-div error-text)))))

(defn add-gallery
  "adds a a new gallery with gallery url to the list of galleries if gallery-url is not empty and the url is not added
 already."
  [input]
  (if (string? input)
    (let [current-data (load-data)
          gallery-url (normalize-url input)]
      (if (empty? (filter #(= (% :href) gallery-url) current-data)) ;; gallery hasn't been added yet
        (if-let [gallery-data (gallery-info gallery-url)] ;; can retrieve data about gallery
          (do
            (save-data (conj current-data gallery-data))
            (get-home))
          (get-home :div#error-new-gallery "Gallery not found"))
        (get-home :div#error-new-gallery "Gallery already added.")))))

(defn delete-gallery
  "adds a a new gallery with gallery url to the list of galleries if gallery-url is not empty and the url is not added
 already."
  [gallery-url]
  (let [current-data (load-data)]
    (if (not (empty? gallery-url))
      (save-data (remove #(= (% :href) gallery-url) current-data)))
    (get-home)))

;; ** route dispatcher ** ;;
(defroutes main-routes
  (GET "/" [] (get-home))
  (GET "/add" [d] (add-gallery d))
  (GET "/delete" [d] (delete-gallery d))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app (handler/api main-routes))

;; ** server starter ** ;;

(defn -main [& args] (run-jetty app {:port 9000}))