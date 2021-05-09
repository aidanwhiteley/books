# Docker file for the MongoDB tier used by the Books Microservice.
#
# This Dockerfile does not load any demonstration data for sample users. To set up an admin user,
# make sure that the Spring profile used for the books microservice specifies an email address
# for books.users.default.admin.email.
#
# To build the Docker image use:
# docker build --rm -t aidanwhiteley/books-db-mongodb:4.4.5-bionic .
#
# To run the Dockerfile created image use something like:
# docker run -e MONGO_INITDB_APP_USERNAME=abc -e MONGO_INITDB_APP_PASSWORD=def -e MONGO_INITDB_DATABASE=ghi aidanwhiteley/books-db-mongodb
#
# The Dockerfile created image is expected to be run from a docker-compose that sets the environment variables
# and is also in control of setting the overall MongoDb authorisation settings i.e. requiring authorisation!
#
FROM mongo:4.4.5-bionic
COPY db_init/*.sh /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/addUserFromEnvVar.sh
