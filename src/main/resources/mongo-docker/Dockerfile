# Docker file for the MongoDB tier used by the Books Microservice
#
# To build Docker image use:
# docker build -t aidanwhiteley/books-db-mongodb .
#
# To run the Dockerfile created image use something like:
# docker run -e MONGO_INITDB_APP_USERNAME=abc -e MONGO_INITDB_APP_PASSWORD=def aidanwhiteley/books-db-mongodb
#
# The Dockerfile created image is expected to be run from a docker-compose that sets the environment variables
# and is also in control of setting the overall MongoDb authorisation settings i.e. requiring authorisation!
#
FROM mongo:4.2.1-bionic
COPY db_init/*.js /docker-entrypoint-initdb.d/
COPY db_init/*.sh /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/addUserFromEnvVar.sh
