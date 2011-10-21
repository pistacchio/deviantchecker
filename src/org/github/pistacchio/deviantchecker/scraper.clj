(ns org.github.pistacchio.deviantchecker.scraper
  (:use net.cgrand.enlive-html)
  (:import java.net.URL))

;; ** utils ** ;;
(defn- enlive-slurp-page
  "returns an enevelop form from string url"
  [url] (-> url URL. html-resource ))

;; ** scrape methods ** ;;
(defn- last-page-gallery-url
  "returns a string with the last page for the deviantart gallery at base-url"
  [gallery-url]
  (let [last-page-url-selector [:div.pagination :ul.pages :li.number :a]
        last-page-url (-> (select (enlive-slurp-page gallery-url) last-page-url-selector)
                          last :attrs :href) ;; /gallery/offset=2
        last-page-offset (if (seq last-page-url) (last (.split #"/" last-page-url)) nil)] ;; offset=2 ]
    (str gallery-url "/" last-page-offset)))

(defn- images-on-last-page
  "given the url of the last page of a gallery, returns the number of images"
  [last-page-url]
  (let [image-selector [:div#gmi-ResourceStream :img]]
    (count (select (enlive-slurp-page last-page-url) image-selector))))

;; ** main methods ** ;;
(defn normalize-url
  "[username |
    http://username.deviantart.com |
    http://username.deviantart.com/gallery |
    http://username.deviantart.com/gallery/?offset=123] => http://username.deviantart.com/gallery"
  [string]
  (let [chars-to-string #(apply str %)
        trimmed-s (.trim string)
        s (chars-to-string (if (= (last trimmed-s) \/) (drop-last 1 trimmed-s) trimmed-s)) ; removes trailing "/" if any
        s-offset (re-find #"(^.*?)\/\?offset=\d+" s)] ;; searches for ending /?offset=NUMBER
    (if (.startsWith s "http://")
      (if (.endsWith s "gallery")
        s
        (if (nil? s-offset)
          (str s "/gallery")
          (second s-offset)))
      (str "http://" s ".deviantart.com/gallery"))))

(normalize-url "http://username.deviantart.com/gallery/")

(defn gallery-info
  "given a string (deviantart user page, username or gallery page, returns a map with the base url, the url of the last
 page and the number of images on the last page"
  [gallery-url]
  (try
    (let [last-page (last-page-gallery-url gallery-url)
          num-images (images-on-last-page last-page)]
      {:href gallery-url :last_page last-page :num_images (str num-images)})
  (catch java.io.FileNotFoundException e nil)))
