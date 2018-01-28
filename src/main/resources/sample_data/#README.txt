Loading data for development
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When running with the spring profile of "dev", the json file contents of this directory will be auto loaded into Mongo
(dependant on the setting of the books:reload:development:data config parameters
i.e. the Mongo data will be "refreshed".

N.B. Commands below only tested on Windows (10)

To extract updated data from the Mongo development database use the following commands
mongo books-dev --eval "db.book.find({}).limit(50).forEach(function(f){print(tojson(f, '', true))})" --quiet > books.json
mongo books-dev --eval "db.user.find({}).limit(50).forEach(function(f){print(tojson(f, '', true))})" --quiet > users.json

N.B. Make sure the files are saved in UTF-8 format or the loading code will complain - the command line code above outputs in UTF-16 on my machine.