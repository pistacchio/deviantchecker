# deviantCHECKER

A simple **[Compojure](https://github.com/weavejester/compojure)** application with a tutorial for learning a couple of things.

## Intro

When starting off with Compojure (a popular **[Clojure](https://github.com/clojure/clojure)** web microframework built on top of [Ring](https://github.com/mmcgrana/ring)), I found some nice tutorials that helped me getting started with it:

* [TechCrunch](http://techbehindtech.com/2010/08/14/compojure-demystified-with-an-example-part-1/)
* [Mikael Sundberg's](http://cleancode.se/2010/08/30/getting-started-with-compojure.html)

Putting all gathered information together (especially about configuring the application) required a bit of trying things out, so I decided to write a short tutorial that maybe someone would find helpful.

### What deviantCHECKER is about

deviantCHECKER is first of all an exercise in futility: in fact it reproduces a functionality already available on **[Deviantart](http://www.deviantart.com/)**! Deviantart is an online community for sharing works of art where people showcase their drawings, photos and the like. deviantCHECKER is a personal application (it doesn't even have authentication) that lets you add artists you'd like to follow and automatically checks whenever they post new works.

As said, this is something you can already do by registering to Deviantart and following people, but I just wanted to put together a small website and try out what appears to be a great Clojure scraping/templating library: **[Enlive](https://github.com/cgrand/enlive)**.

This is how the final application looks like:

![deviantSCRAPER screenshot](https://github.com/pistacchio/deviantchecker/raw/master/resources/public/tutorial-app-screenshot.png) 

### What you need to know

This tutorial assumes that you have some basic knowledge of Clojure and [Leiningen](https://github.com/technomancy/leiningen) and, of course, web development.

### Testing the application

To test the application, clone the repository and run it with Leiningen:

    lein run
    
This starts a server running on your machine on port 3000, so you can visit the site by browsing to `http://127.0.0.1:3000`.

## Setting up the application

### Files

The final project structure looks like this:

![file tree](https://github.com/pistacchio/deviantchecker/raw/master/resources/public/tutorial-app-tree.png)    


The heart of our application are the two Clojure files in `/src/org/github/pistacchio/deviantchecker`.

`/resources` contains:

* static files for under `/resources/public`
* our template files (actually only one!) in `/resources/tpl` 
* our "database": `/resources/data/data.dat`.

### project.clj

First of all you want to set up the project dependances in `project.clj`.

    (defproject org.github.pistacchio.deviantchecker "0.9.0"
      :description "A one page application for keeping track of changes on deviantart.com gallieries"
      :dependencies [[org.clojure/clojure "1.2.1"]
                     [org.clojure/clojure-contrib "1.2.0"]
                     [enlive  "1.0.0"]
                     [compojure "0.6.4"]
                     [ring/ring-jetty-adapter "1.0.0-beta2"]]
      :dev-dependencies [[lein-ring "0.4.6"]]
      :ring {:handler org.github.pistacchio.deviantchecker.core/app}
      :main org.github.pistacchio.deviantchecker.core)

Apart from the obvious dependences on `org.clojure.clojure` and `org.clojure/clojure-contrib`, we are going to use: 

* Enlive to _scrape_ Deviantart pages and as a _template engine_
* Compojure as our web framework
* Ring Jetty adaper: Ring is the web library that supports Compojure. We are going to use the jetty adapter for a fast deploy
* Lein-Ring is a Leiningen plugin that interfaces with Ring and lets us start a _development_ server that makes our debugging and testing much easier. `:dev-dependencies` are only resolved at build time, not at runtime; Lein-Ring is not part part of our project, we are just using it during the development.
* `:ring` is a special Lein-Ring (you guessed it!) key that lets us specify the handler for requests (more on this later)
* Finally, the `:main` key lets the compiler know where the entry point for our application is located. This is not used by the development server, but when deploying our app.

### First steps on core.clj

#### Namespace, :use, :require

All web-related code is contained within `/src/org/github/pistacchio/deviantchecker/core.clj`. The _namespace_ part part looks like this:

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

Apart from `compojure` and Ring Jetty adapter we've talked about, we are going to need:

* the _json encoder_ from `clojure.contrib` (used for our [ajax](http://en.wikipedia.org/wiki/Ajax_(programming)) calls)
* Enlive
* `compojure.route` for defining the way calls to the server are handled
* `compojure.handler` that has some utility functions we'll use.
* `compojure.java.io` for accessing files on the file system

Finally, we use `org.github.pistacchio.deviantchecker.scraper` (our functions for scraping Deviantart) and `(:gen-class)` to make a Java class out of our namespace. This, together with the definition of the main method at the end of the file `(defn -main [& args] (run-jetty app {:port 3000}))` makes possibile to specify this as the principal class for our project (remember `:main org.github.pistacchio.deviantchecker.core` in `project.clj`?)

#### (defn -main)

As said

    (defn -main [& args] (run-jetty app {:port 3000}))

defines the entry point of our application _when not running in test_. We are simply starting a [Jetty](http://jetty.codehaus.org/jetty/) server on port `3000` serving the application `app` (keep reading).

#### Defining our application

The single function call

    (def app (handler/api main-routes))

is very important. Remember `:ring {:handler org.github.pistacchio.deviantchecker.core/app}` in `project.clj`? It refers to this `app`. We could have as well used directly main-routes (more about it shortly), but `clojure.handler/api` lets us wrap our route handler into useful Ring wrapper that enrich every request making our development easier. More on the wrappers. For more complex applications you can use `(site)`. More on wrapper here: [here](http://weavejester.github.com/compojure/compojure.handler-api.html).

#### Defining routes

Routes are the central part of our application.

    (defroutes main-routes
      (GET "/" [] (get-home))
      (GET "/add" [d] (add-gallery d))
      (GET "/delete" [d] (delete-gallery d))
      (GET "/check" [d] (check-gallery d))
      (route/resources "/")
      (route/not-found "Page not found"))

`(defroutes)` lets us define how calls are despatched. `main-routes` is how we named our route dispatcher. `(GET "/" [] (get-home))` instructs the server so that every time it receives a _GET_ request to `http://127.0.0.1:3000/` it must call `(get-home)`. The first param can of course be as well _POST_, _DELETE_, _PUT_, _HEAD_ as they are valid http methods, or _ANY_, meaning that you can call the page with whatever method.

`(GET "/add" [d] (add-gallery d))` is very similar. It intercepts calls to `http://127:0.0.1:3000/add`. What is `[d]`? We want to obtain parameters from the calls, for example when someone has submitted a form. The simplest way to get parameter is this:

    (POST "/" {params :params} (str "Your name is " (params "name")))

With the third argument we can deconstruct the _request_ object. It is a map with keys like _:request_, _:session_, _:cookies_, _:flash_ and of course _:params_ that holds POST parameters. We are getting _:params_ and assigning it to `params`. Later, we get and use _"name"_ out of _params_.

How come that in our application we can obtain parameters out of a GET request if I told that only POST params are passed? And what's with `[d]`? Remember that we've wrapped up our routes with `(api)`? It lets us treat GET parameters (querystring) just like POST params. Moreover, it expands `:params` so that you can deconstruct its keys directly, and that's why `(GET "/add" [d] (add-gallery d))` is possible. Whenever you visit `http://127.0.0.1:3000/add?d=http://e-lite.deviantart.com` our route handler calls `(add-gallery "http://e-lite.deviantart.com")`.

We can be a bit more specific about our routes because they're threated as regex, so, for example, `(GET "*" [] "hello")` will return "hello" no matter what page you visit or `(GET "/valid/number\d+" [] "hello")` will accept a call to `http://127:0.0.1:3000/valid/number/123` but not to `http://127:0.0.1:3000/valid/number/bob`.

Finally, you can get useful information out of the url itself:

    (GET "/you/name/:name/and/surname/:surname" [name surname] (str "hello " name " " surname))

Visiting `http://127.0.0.1:3000/your/name/toshiro/and/surname/mifune` will show in your browser "hello toshiro mifune".

Note that you can only specify keys after "/". Before aknowledging this, I tried to map (failing) `(GET "/add?:gallery-url" [gallery-url] (add-gallery gallery-url))`.

We've defined two more routes: `(route/resources "/")` instructs the server about where resources are. Resources are basically static files (images, script files, stylesheets and the like). By default it will look into `/public` that with Leiningen becomes `resources/public`. This is what we are using here, and I think it's a good default, but if you want to change it, you can:

    (route/resources "/img" {:root "./static_files"})

While with the default settings `http://127.0.0.1:3000/loading.gif` would look for `/resources/public/loading.gif`, with this other setting `http://127.0.0.1:3000/img/loading.gif` would look for `./static_files/loading.gif`.

We're almost done! `(route/not-found "Page not found")` will return "Page not found" whenever someone tries to visit a page we've not defined as a valid route or an internal server error occurs. Time for a fancier 404 page?

## Data

### Serialization

We are storing all the information about Deviantart galleries in a plain text file: `/resources/data/data.dat`. In a real world application, of course, a database backend would be compulsory, but for our example this is enough.

Note that in the code I define the name of our file as `data/data.day` because _resources_ is already the default path that Leiningen uses for storing additional resources. If you pack the project as a .jar or .war, resources files would be taken out of `/resources` and deployed in the root directory of the archive.

The dynamic nature of Clojure makes serialization really easy. To load our data  we use [`(load-file)`](http://clojuredocs.org/clojure_core/1.2.0/clojure.core/load-file), to store it to a file once we've modified it, we just use [`(print-dup)`](http://clojuredocs.org/clojure_core/clojure.core/print-dup). 

I've written a utility function to feed those two functions with a correct path to the data file. I use the same function later on when having to retrieve a template file.

    (defn get-file
      "return the absolute path of relative-path"
      [relative-path]
      (.getPath (io/resource relative-path)))

### Data structure in deviantSCRAPER

I won't go into details about our data layer because you can easily read about it through the code. Breafly, data about galleries is stored (in memory and on the file) as a sequence of maps. Each map represents a gallery. For each gallery we store:

* the base url of the gallery into the key `:href` (_http://USERNAME.deviantart.com/gallery_)
* the url of the last page of the gallery in case of multi-pages galleries into `:last_page` (for example _http://USERNAME.deviantart.com/gallery?offset=12_)
* the number of images on the last page into `:num_images`.

Doing so, it is easy to check whenever a user of Deviantart has added new works: we just need to retrieve again the data about a certain gallery and see if `:last_page` or `:num_pages` have changed!

## Views

While `(GET "/" [] "<h1>Hello John Travolta!</h1>")` is a perfectly valid route, chances are that you want to perform some more complex operations, and that's why we're passing the control to our "view" functions that return a string.

### Enlive

You can produce the whole page in the form of a string, but keeping html separated from the code is always a good idea and that's why we use Enlive. There are other template engines available for Clojure, for example [Hiccup](https://github.com/weavejester/hiccup) (that comes shipped with Compojure) or [Fleet](https://github.com/Flamefork/fleet) plus, of course, a plethora of Java templates engines.


Since we are doing some page scraping and Enlive is both a template engine and a scraping library, we can kill two birds with one stone and use only one library! Moreover, Enlive has a really neat mechanism that allows a perfect separation of concerns not including _anything_ template-engine related into the html. Note that unless you don't want to work with XML transformations, Enlive is only good for working with XML data (so HTML is ok), but it would prove useless if you wanted to use it for mails, css and other data you'd want to use a template engine with.

#### Loading html

If you look at `/resources/tpl/home.html`, you'll find out that it's just plain html. We'll take a look at `(get-home)` to see how we use Enlive. The key function call is

    (html-resource (java.io.File. (get-file "tpl/home.html"))

This gives us back a parsed version of our html file. Basically `net.cgrand.enlive-html/html-resource` converts the html file into a Clojure structure made of sequences and maps. For example, if we parse a file like

    <html>
        <div class="test">
            How are you, Jack Burton?
        </div>
    </html>

we get

    ({:tag :html,
      :attrs nil,
      :content ({:tag :body,
                 :attrs nil, 
                 :content ({:tag :div,
                            :attrs {:class "\\\"test\\\""},
                            :content ("How are you, Jack Burton?")})})})

#### Tranformations

As you can see, it is possible to modify and analyze this kind of structure, but fortunatly Enlive provides us with a handful of functions that make it easier to work with this kind of heavily nested data.

In `(get-home)` we have the following transformation:

    (transform % [:li.gallery]
        (clone-for [g data]
          [:a.url] (set-attr :href (g :last_page))
          [:a.url] (content (g :href))
          [:a.delete] (set-attr :href (str "/delete?d=" (g :href)))))

`(transform)` accepts a template structure like the one you can get with `(html-resource)`, a _selector_ and a _transformation_. Selectors are shaped on [css selectors](http://shivasoft.in/blog/webtech/complete-css-selector-tutorial-for-the-beginners/), `[:li.gallery]`, for example, selects `<li>` elements whose class is _gallery_.

The transformation function we are going to use is `(clone-for)`. It "clones" what we've selected and reproduces it multiple times, once for every element contained in sequence `data` and binds it to `g`. `(clone-for)` accepts multiple selectors and transformations. The two selectors that we use here are `(set-attr)` (we set the `href` attribute of two `<a>` tags) and the content of one of them with `(content)`. You can learn a lot more about Enlive transformations in this [helpful tutorial](https://github.com/cgrand/enlive/wiki/Table-and-Layout-Tutorial,-Part-1:-The-Goal).

#### From Enlive structure to string

Since we need to return a string and what we have is a bunch of sequences and maps, we make use of Enlive's function `(emit*)` that given a structure returns a _sequence_ of strings. So I've written a small function to go directly from an Enlive data structure to a string ready to be passed to the browser!

    (defn emit**
      "given an enlive form, returns a string"
      [tpl]
      (apply str (emit* tpl)))

## Finely grained responses

One of the function views is different from the others because doesn't simply return a string obtained with Enlive. In fact in Compojure you can either return a string or decide to pack a response to the browser having more control on it.

In this application, the function `(check-view)` is called via ajax (you can check the code on the view, I'm using [jQuery](http://jquery.com/)).

This is what we return:

    {:headers {"Content-Type" "application/json"}
     :body (json-str {:updated updated})} 

As Content-Type, we are not returning `text/html` (the default) but `application/json`. The response content is bound to `:body`. You can also specify a `:status` code.

## Scraping

Our scraping frunctions analyze Deviantart pages and give us back information about galleries we are interested into. It is basically Clojure code and not web-related, so I'm not going to illustrate it here. 

I just want to point out that once you have an html file parsed into a Clojure data structure (thanks to Enlive), it is very easy to use Enlive selectors to perform any sort of scraping. For example, in `org.github.pistacchio.deviantscraper.scraper/last-page-gallery-url` we find the last page of the gallery in its first page with the selector `[:div.pagination :ul.pages :li.number :a]`. This gives us a list of links. We then take the `(last)` one and extract the `:href` `:attr`ibute.

Similarly, to count the number of images on the last page, we `(count)` the number of items selected by `[:div#gmi-ResourceStream :img]`.

## Developing and deploying

Do you still remember all that talking about "development server"? Once you've configured your application like this, you can start it with

    lein ring server [port]

While developing, this will give you some invaluable helps, notably:

* it monitors application files and whenever you make some changes, it reloads the application so that you don't need to restart the server to see every little change done to the code
* in the browser, it shows a stacktrace of any error that may occur.

When you're ready to deploy your application in production, you have at hand a couple of straight forward options:

    lein run

This is by far the easiest. This starts a Jetty server serving your application.

Alternatively you can pack everything with

    lein uberjar

and run the application like you would do with any java .jar file.

    java -jar org.github.pistacchio.deviantchecker-0.9.0-standalone.jar

You can also use `lein jar` that produces a smaller .jar by not including every dependency library, but you'll then have to tinker with the `CLASSPATH`.

Leiningen can also pack it all into a _.war_ file with `lein ring war` and `lein ring uberwar` so that you can deploy it under [Tomcat](http://tomcat.apache.org/) or any other Java server supporting wars.

I tried this under Tomcat and it worked _almost_ well. The application runs and works out of the box but with a glitch I haven't been able to overcome. When you visit `http://localhost:8080/deviantscraper` (assuming we've deployed `deviantscraper.war`), Tomcat `should` perform a 302 redirect to `http://localhost:8080/deviantscraper/` (<- note the trailing slash!). This didn't work in my environment causing the .css and .gif file not to load.

## Contact

You can contact me via mail at [pistacchio@gmail.com](mailto:pistacchio@gmail.com). Feel free to fork this little project and expand it as you wish!