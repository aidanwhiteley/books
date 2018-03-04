Loading data for development
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When running with defined spring profiles, the .data file contents of this directory will be auto loaded into Mongo
(dependant on the setting of the books:reload:development:data config parameters)
i.e. the Mongo data will be "refreshed".

N.B. Commands below only tested on Windows (10)

To extract updated data from the Mongo development database use the following commands (arbitrary limit of 250 books)
mongo books-dev --eval "db.book.find({}).limit(250).forEach(function(f){print(tojson(f, '', true))})" --quiet > books.json
mongo books-dev --eval "db.user.find({}).limit(250).forEach(function(f){print(tojson(f, 'UTF-8', true))})" --quiet > users.json

Or from a database which requires authentication (where the db is also books-dev)
mongo books -u user -p password --eval "db.book.find({}).limit(250).forEach(function(f){print(tojson(f, '', true))})" --quiet > books.json

N.B. Make sure the files are saved in UTF-8 format or the loading code will complain.

To run in the indexes manually, use
mongo db -u user -p pw --eval "shell command from file"