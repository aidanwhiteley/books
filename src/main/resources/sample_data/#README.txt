Loading data for development
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When running with the spring profile of "dev", the *.json contents of this directory will be auto loaded 
i.e. the Mongo data will be "refreshed".
N.B. Commands below only tested on Windows (10)

To extract updated from the Mongo database use the following commands
mongo books-dev --eval "db.book.find({}).limit(50).forEach(function(f){print(tojson(f, \"\", true))})" --quiet > books.json
mongo books-dev --eval "db.user.find({}).limit(50).forEach(function(f){print(tojson(f, \"\", true))})" --quiet > users.json

N.B. Make sure the files are saved in UTF-8 format or the loading code will complain. It is also (optionally) possible
     to remove the _id field from the files but this isnt mandatory as the entire collection is dropped before 
     re-inserting the data. It does, however, make it easier when bulk inserting lots of data.