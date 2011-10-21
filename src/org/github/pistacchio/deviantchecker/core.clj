(ns org.github.pistacchio.deviantchecker.core
  (:use compojure.core
        ring.adapter.jetty
        org.github.pistacchio.deviantchecker.scraper
        [clojure.contrib.json :only (json-str)]
        net.cgrand.enlive-html)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :as io]
            [clojure.contrib.sql :as sql])
  (:gen-class))

;; ** utilities ** ;;
(defn emit**
  "given an enlive form, returns a string"
  [tpl]
  (apply str (emit* tpl)))

(defn get-file
  "returns the absolute path of relative-path"
  [relative-path]
  (.getPath (io/resource relative-path)))

;; constants ;;

(def *db* {:classname "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname (get-file "data/data.sqlite")})

;; ** data management ** ;;
(defn load-galleries
  "returns a gallery database stored in *data-file*"
  []
  (sql/with-connection *db*
    (sql/with-query-results res ["SELECT * FROM galleries"]
      (into [] res))))

(sql/with-connection *db*
    (sql/with-query-results res ["SELECT * FROM galleries"] res))

(defn get-gallery
  "retrieves gallery from a map data"
  [gallery-url]
  (sql/with-connection *db*
    (sql/with-query-results res ["SELECT * FROM galleries WHERE href=?" gallery-url]
      (first res))))

(defn update-or-insert-gallery
  "inserts of updates gallery into data"
  [gallery]
  (sql/with-connection *db*
    (sql/update-or-insert-values
      :galleries
      ["href=?" (gallery :href)]
      gallery)))

(defn del-gallery
  [gallery-url]
  (sql/with-connection *db*
    (sql/delete-rows :galleries ["href=?" gallery-url])))

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
    (let [gallery-url (normalize-url input)]
      (if (empty? (get-gallery gallery-url)) ;; gallery hasn't been added yet
        (if-let [gallery-data (gallery-info gallery-url)] ;; can retrieve data about gallery
          (do
            (update-or-insert-gallery gallery-data)
            (get-home))
          (get-home :div#error-new-gallery "Gallery not found"))
        (get-home :div#error-new-gallery "Gallery already added.")))))

(defn delete-gallery
  "adds a a new gallery with gallery url to the list of galleries if gallery-url is not empty and the url is not added
 already."
  [gallery-url]
  (if (not (empty? gallery-url))
    (del-gallery gallery-url))
    (get-home))

(delete-gallery "http://mrshot.deviantart.com/gallery")

(defn check-gallery
  "checks if a gallery has been updated since last check"
  [gallery]
  (let [ stored-gallery (get-gallery gallery)
         updated (if (seq stored-gallery) ;; gallery found
                    (let [current-gallery (gallery-info gallery)
                          {cur-gal-last_page :last_page cur-gal-num_images :num_images} current-gallery
                          {strd-gal-last_page :last_page strd-gal-num_images :num_images} stored-gallery]
                      (if (and (= cur-gal-last_page strd-gal-last_page) ;; gallery unchanged
                               (= cur-gal-num_images strd-gal-num_images))
                          "NO"
                          (do
                            (update-or-insert-gallery current-gallery)
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