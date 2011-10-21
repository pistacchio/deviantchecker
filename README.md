# deviantCHECKER

## Part 2

In the [first part](https://github.com/pistacchio/deviantchecker) of this tutorial (that I assume you've read), we've set up our application. Most of the work is done, but you probably turned your nose up at "data.dat", not only because the name sucks and is redundant, but also because when you think "storing data" two letters pop up in your mind: **DB**

We are now going to replace our "data layer" (the functions for reading / updating `data.dat`) with functions for working with [Sqlite3](http://www.sqlite.org/) database.

## Sqlite3

Sqlite3 is single file, lightweight database engine. It is probably the simplest way to experiment with SQL as there is no setup and data is simply stored in files on the file system.

Thanks to the abstraction provided by [JDBC](http://en.wikipedia.org/wiki/Java_Database_Connectivity) (Java standard API for database access), once we have Sqlite3 working, you can probably start from there and access whatever database you want to use. When the number of galleries you follow starts exceeding several millions and you decide that Sqlite3 doesn't work anymore for you, you should be able to switch to [Postgres](http://www.postgresql.org/) or other database engines by tweaking just a few lines.

### DB structure

Under `resources/data` you'll find a couple of new files: `create_db.sql` (with the SQL command for creating our database) and `data.sqlite`, our database file.

The database only has one table: `galleries`. The table has three text fields that just remaps those use in Part 1: `href`, `last_page` and `num_images`. Also, the data is exactly the same found in data.dat:

![db data](https://github.com/pistacchio/deviantchecker/raw/db-sql/resources/public/tutorial-app-sql.png)

## Setting up!

We just need few changes to the overall structure and configuration. In `project.clj` we need to add a dependece from Sqlite3 drivers:

    [sqlitejdbc "0.5.6"]

while in the namespace and import part of `core.clj` we add

    (:require [clojure.contrib.sql :as sql])
    
That's it! We are almost done. Before modifying the functions, we can get rid of `(def *data-file* "data/data.dat")` and replace it with

    (def *db* {:classname "org.sqlite.JDBC"
               :subprotocol "sqlite"
               :subname (get-file "data/data.sqlite")})

This map is all we need to tell JDBC what driver to use for the access to the DB (`:classname`), what kind of database it is (`subprotocol`) and to find it (`:subname`).

When you want to switch database, you just have to modify this map. Other database engines may need more parameters, most popular databases for example would need `:user` and `:password`. [This article](http://en.wikibooks.org/wiki/Clojure_Programming/Examples/JDBC_Examples) has an extensive coverage of connection parameters needed by different engines.

## Actual changes

#### Renames

First of all, notice that `(update-gallery)` is now called `(update-or-insert-gallery)` because, taking advantage of `clojure.contrib.sql/update-or-insert-values` we've merged the update functionality with the insert one.

Moreover we have a new function: `del-gallery` (for removal). Also note that the arity of the data layer methods maye have changed because in the previous version we needed to be passed the whole data map, while now we don't need it. So, for example, `(get-gallery [gallery-url data])` seen in Part 1 is now `(get-gallery [gallery-url])`.

### SELECT

The core or `(load-galleries)` is

    (sql/with-connection *db*
        (sql/with-query-results res ["SELECT * FROM galleries"]
          (into [] res)))
          
It shows us the basic usage of `clojure.contrib.sql`. `(with-connection)` accepts a map descring the database to connect to (our `*db*`), open the connection and performs an action.

The "action" in `(load-galleries)` is `(with-query-result)`, another `clojure.contrib.sql` mostly used to execute queries that return some value, like SELECTS. Here, we are assigning the result of the standard SQL query `SELECT * FROM galleries` to `res`.

The result is a sequence of maps (each representing a row) that resambles _very_ closely what we had in Part 1:

    ({:href "http://USERNAME.deviantart.com/gallery", :last_page "http://USERNAME.deviantart.com/gallery/?offset=123", :num_images "10"} {} ...)
    
Every key is a table's field. You can return it as it is or perform any action on it like you would do with any sequence of maps. For convenience I've put it into a vector.

#### WHERE

`(get-gallery)` is very similar:

    (sql/with-connection *db*
        (sql/with-query-results res ["SELECT * FROM galleries WHERE href=?" gallery-url]
          (first res)))    
          
It works just like the other SELECT, but with a twist: a WHERE clause. Composing SQL statements with raw string concatenation is always risky, and `clojure.contrib.sql` privides us with a convenient statement preparation mechanism. In the select `?` is replaced with the first argument (`gallery-url`). The order or interpolation is positional, for instance:

    (sql/with-query-results res ["SELECT * FROM table WHERE field=? AND other_field=?" "field_value" 42] res)
    
would produce

    SELECT * FROM table WHERE field='field_value' AND other_field=42

### UPDATE and INSERT

It should now be easy for you to understand the other sql-related functions. what `(update-or-insert-values)` does is self explanatory.

    (sql/update-or-insert-values
          :galleries
          ["href=?" (gallery :href)]
          gallery)
          
The first arguement, passed as a _keyword_, is the name of our table. Following a vector of _WHERE_ clauses that are used by the function to retrieve the records to update (if any); here we want to select a gallery with a particular _href_.

Finally a map like the one seen before containing the fields to update. If I would want to update the number of Academy Awards won by Peter Jackson, I'd use:

    (sql/update-or-insert-values
          :directors
          ["name=?" "Peter Jackson"]
          {:academy_awards 3})

#### Other INSERTs and UPDATEs

A raw INSERT (with no UPDATE checks) you can use `(insert-records)` (for multiple INSERTs, by passing more maps in a sequence), `(insert-record)` (providing a key for each field) or `(insert-values)` for providing only some fields.

For UPDATEs you can use `(update-values)`. All those functions are documente in the [official API](http://richhickey.github.com/clojure-contrib/sql-api.html). With the basics descussed here, it should be easy to understand how they works.

### DELETE

`(del-gallery)` doesn't offer much more. The sql part is just a one-liner:

    (sql/delete-rows :galleries ["href=?" gallery-url])

With a form similar to the one seen in `update-or-insert-values`, it deletes all the rows found with the selector `["href=?" gallery-url]` in the table `:galleries`.

## Contact

That's it. As we've seen, connecting with a database with Clojure is very straight-forward.

You can contact me via mail at [pistacchio@gmail.com](mailto:pistacchio@gmail.com). Feel free to fork this little project and expand it as you wish!