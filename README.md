# deviantCHECKER

A simple **[Compojure](https://github.com/weavejester/compojure)** application with a tutorial for learning a couple of things.

## Intro

When starting off with Compojure (a popular **[Clojure](https://github.com/clojure/clojure)** web microframework built on top of [Ring](https://github.com/mmcgrana/ring)). I found some nice tutorials that helped me getting started with Clojure:

* [TechCrunch](http://techbehindtech.com/2010/08/14/compojure-demystified-with-an-example-part-1/)
* [Mikael Sundberg's](http://cleancode.se/2010/08/30/getting-started-with-compojure.html)

Putting all gathered information together (expecially about configuring the application) required a bit of trying things out, so I decided to write a short tutorial that maybe someone would find helpful.

### What deviantCHECKER is about

deviantCHECKER is first of all an exercise in futily: in fact it reproduces a function already available on **[Deviantart](http://www.deviantart.com/)**! Deviantart is an online community for sharing works of art where people showcase their drawings, photography and the like. deviantCHECKER is a personal (it doesn't even have authentication) application that lets you add artists you'd like to follow and automatically checks whenever they post new works.

As said, this is something you can already do by registering to Deviantart and following people, but I just wanted to try out a small website and what appears to be a great Clojure scraping/templating library: **[Enlive](https://github.com/cgrand/enlive)**.

This is how the final application looks like:

![deviantSCRAPER screenshot](/images/screenshot.jpeg) 

### What you need to know

This tutorial assumes that you have some basic knowledge of Clojure and [Leiningen](https://github.com/technomancy/leiningen) and, of course, web development.

### Testing the application

To test the application, clone the repository and run it with Leiningen:

    lein deps
    lein run
    
This starts a server running on your machine on the port 3000, so you can visit the site by browsing to `http://127.0.0.1:3000`.

## Setting up the application

The final project structure looks like this:

![deviantSCRAPER structure](/images/structure.jpeg) 

The heart of our application are the two Clojure files in `/src/org/github/pistacchio/deviantchecker`. `/resources` contains the static files for the page under `/resources/public`, our template files (actually only one!) in `/resources/tpl` and our "database": `/resources/data/data.dat`.

First of all you want to set up the project dependances in `project.cli`.

    (defproject org.github.pistacchio.deviantchecker "0.9.0"
      :description "A one page application for keeping track of changes on deviantart.com gallieries"
      :dependencies [[org.clojure/clojure "1.2.1"]
                     [org.clojure/clojure-contrib "1.2.0"]
                     [enlive  "1.0.0"]
                     [compojure "0.6.4"]
                     [ring "1.0.0-beta2"]]
      :dev-dependencies [[lein-ring "0.4.6"]]
      :ring {:handler org.github.pistacchio.deviantchecker.core/app}
      :main org.github.pistacchio.deviantchecker.core)

Apart from the obvious dependences from `org.clojure.clojure` and `org.clojure/clojure-contrib`, we are going to use: 

* Enlive for _scraping_ Deviantart pages and as a _template engine_
* Compojure as our web framework
