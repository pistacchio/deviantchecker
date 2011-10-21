(ns org.github.pistacchio.deviantchecker.core
  (:use compojure.core
        ring.adapter.jetty
        org.github.pistacchio.deviantchecker.scraper
        [clojure.contrib.json :only (json-str)]
        net.cgrand.enlive-html)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :as io])
  (:gen-class))

(def *data-file* "data/data.dat")

;; ** utilities ** ;;
(defn emit**
  "given an enlive form, returns a string"
  [tpl]
  (apply str (emit* tpl)))

(defn get-file
  "return the absolute path of relative-path"
  [relative-path]
  (.getPath (io/resource relative-path)))

;; ** data management ** ;;
(defn load-galleries
  "returns a gallery database stored in *data-file*"
  []
  (load-file (get-file *data-file*)))

(defn save-data
  "serializes data to *data-file* like"
  [data]
  (with-open [f (java.io.FileWriter. (get-file *data-file*))]
    (print-dup (into [] data) f)))

(defn get-gallery
  "retrieves gallery from a map data"
  [gallery-url data]
  (first (filter #(= (% :href) gallery-url) data)))

(defn update-data
  "inserts of updates gallery into data"
  [gallery data]
  (let [gallery-url (gallery :href)](save-data
    (conj
      (remove #(= (% :href) gallery-url) data)
      gallery))))

;; ** views ** ;;
(defn get-home
  "GET /
 error-message may be a vector with a selector and an error message to display within it."
  [& error-message]
  (let [tpl (html-resource (java.io.File. (get-file "tpl/home.html")))
        data (load-galleries)
        main-transform #(transform % [:li.gallery]
                          (clone-for [g data]
                            [:a.url] (set-attr :href (g :last_page))
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
    (let [current-data (load-galleries)
          gallery-url (normalize-url input)]
      (if (empty? (get-gallery gallery-url current-data)) ;; gallery hasn't been added yet
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
  (let [current-data (load-galleries)]
    (if (not (empty? gallery-url))
      (save-data (remove #(= (% :href) gallery-url) current-data)))
    (get-home)))

(defn check-gallery
  "checks if a gallery has been updated since last check"
  [gallery]
  (let [ data (load-galleries)
         stored-gallery (get-gallery gallery data)
         updated (if (seq stored-gallery) ;; gallery found
                    (let [current-gallery (gallery-info gallery)
                          {cur-gal-last-page :last_page cur-gal-num-images :num_images} current-gallery
                          {strd-gal-last-page :last_page strd-gal-num-images :num_images} stored-gallery]
                      (if (and (= cur-gal-last-page strd-gal-last-page) ;; gallery unchanged
                               (= cur-gal-num-images strd-gal-num-images))
                          "NO"
                          (do
                            (update-data current-gallery data)
                            "YES")))
                    "NO")]
      {:headers {"Content-Type" "application/json"}
       :body (json-str {:updated updated})}))

;; ** route dispatcher ** ;;
(defroutes main-routes
  (GET "/" [] (get-home))
  (GET "/add" [d] (add-gallery d))
  (GET "/delete" [d] (delete-gallery d))
  (GET "/check" [d] (check-gallery d))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app (handler/api main-routes))

;; ** server starter ** ;;

(defn -main [& args] (run-jetty app {:port 3000}))