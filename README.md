# books
This project started as I wanted a simple "microservice" to use when trying out frameworks
such as Docker, Docker Compose, Spring Cloud, Pivotal Cloud Foundy and AWS.

It has developed a little further such that it is starting to provide some functionality that may 
actually be useful. 

So welcome to the "Cloudy Bookclub" microservice!

> [!NOTE]  
> Now uplifted to the latest Spring Boot 3.x and Java 21.

[![Actions CI Build](https://github.com/aidanwhiteley/books/workflows/Actions%20CI%20Build/badge.svg)](https://github.com/aidanwhiteley/books/actions?query=workflow%3A%22Actions+CI+Build%22)
[![Sonar Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.aidanwhiteley%3Abooks&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.aidanwhiteley%3Abooks)

## Implementation

The main functionality included in the microservice includes
* being based on latest Spring Boot 3.x and Java 21
* Oauth2 based logon using
    * Google
    * Facebook
* the oauth2 logon data is transmogrified into locally stored users - with associated roles - and into a JWT token - 
making the web application entirely free of http session state (see later for whether using JWTs as session tokens is a good idea)
* Spring Security for role based method level authorisation
* Mongo based persistence with the use of Spring Data MongoRepository 
    * next to no persistence code
    * except for some Mongo aggregation queries added to the Repository implementation
* accessing the Google Books API with the Spring RestTemplate and, a work in progress, the reactive Spring WebClient
* and Docker images and a docker-compose file that runs all the tiers of the application with one "docker-compose up --scale api-tier-java=2" command

### Running in development
The checked in default Spring profile is "mongo-java-server". This uses the in memory mongo-java-server so there is no need to run MongoDb locally. So you 
should be able to just check out the code and run the application for development purposes with no other dependencies.

To develop Mongo related code you should switch to the "dev" profile which does expect to be able to connect to a real MongoDb instance.

Please check the console log when running the application. Any key constraints/warnings related to the Spring profile being used will be
output to the console.

To run the application and access the "behind logon" functionality, see the "How to configure application logon security" section below.

### Tests
All tests should run fine "out of the box". 

By default, the tests run against mongo-java-server so there is no need to install
MongDb to test most of the application. Functionality not supported by mogo-java-server such as full text indexes results in some tests being skipped when 
running with the monog-java-server Spring profile.

When running the CI builds with Github Actions, tests run against a real Mongo instance.

Some of the integration tests make use of WireMock - see the /src/test/resources/mappings and __files directories for the configuration details.

The tests are probably about 50/50 between unit and integration tests...

#### Stress Test
To examine how the WebClient code is behaving there is a Maven plugin configured that runs a basic Gatling load test.
After starting the Spring Boot application (i.e. mvn spring-boot:run or via your IDE) run the command:

mvn gatling:test

The source code of this test in at test/java/com/aidanwhiteley/books/loadtest/StressTestSimulation.java

> [!IMPORTANT]  
> The reactive WebClient code will be replaced by the use of virtual threads at some point.

### How to configure application logon security
A lot of the functionality is protected behind oauth2 authentication (via Google and Facebook). 
To use this, you must set up credentials (oauth2 client ids) on Google and Facebook.
You must then make the clientId and clientSecret available to the running code.
There are "placeholders" for these in /src/main/resources/application.yml i.e. replace the existing
"NotInSCMx" (Not In Source Code Management!) values with your own.
There are lots of other ways to pass in these values e.g. they can be passed as program arguments
~~~~
--spring.security.oauth2.client.registration.google.client-id=xxxx --spring.security.oauth2.client.registration.google.client-secret=xxxx --spring.security.oauth2.client.registration.facebook.client-id=xxxx --spring.security.oauth2.client.registration.facebook.client-secret=xxxx 
~~~~
Otherwise, see the Spring documentation for more options.

You must also set a secret key to sign the JWTs (see later).

### Available Spring profiles
There are Spring profile files for a range of development and test scenarios. 

#### default 
	- sets active profile to dev-mongo-java-server - otherwise
	- requires oauth configured correctly for access to update operations
	- configured to disallow CORS access to APIs
	- does not clear down DB or reload test data on every restart

#### dev-mongo-java-server
	- uses an in memory mongo-java-server rather than a real MongoDb
	- requires oauth configured correctly for access to update operations
	- configured to allow CORS access to APIs
	- clears down the DB and reloads test data on every restart
	
#### dev-mongo-java-server-no-auth
	- uses an in memory mongo-java-server rather than a real MongoDb
	- configured such that all request have admin access. No oauth set up required and no logon
	- configured to allow CORS access to APIs
	- clears down the DB and reloads test data on every restart
	
#### dev-mongodb-no-auth
	- uses a real MongoDb
	- configured such that all request have admin access. No oauth set up required and no logon
	- configured to allow CORS access to APIs
	- clears down the DB and reloads test data on every restart
	
#### dev-mongodb
	- uses a real MongoDb
	- requires oauth configured correctly for access to update operations
	- configured to allow CORS access to APIs
	- clears down DB and reloads test data on every restart
	
#### CI
	- uses a real MongoDb
	- configured to allow CORS access to APIs
	- clears down the DB and reloads test data on every restart
	
#### container-demo-no-auth
    - requires the use of "docker-compose up" to start Docker containers - see later
	- uses a real MongoDb
	- configured such that all request have admin access and oauth config is not required
	- does not allow CORS access to APIs
	- clears down the Mongo DB and reloads test data on every restart of the Docker containers
	

### Configuring for production
"Out of the box" the code runs with the "mongo-java-server" Spring profile - see the first lines of application.yml. None
of the checked in available Spring profiles are intended for production use. You will need to decide the 
required functionality for your environment and configure your Spring profile accordingly. 

For instance, you **WILL** want to 
set/change the secretKey used for the JWT token signing (see books:jwt:secretKey in the yml files).
Please see the code in com.aidanwhiteley.books.controller.jwt.JwtUtils::createRandomBase64EncodedSecretKey
which will allow you to generate a random SecretKey rather than using the default one supplied
in the non-propduction properties files.

You will also need access to a Mongo instance. The connection URL (in the yml files) will result in the automatic
creation of a Mongo database and the two required collections (dependant on the security config of your Mongo install).

Check the console log when running in production - you should see **NO** warning messages!

### How to build and run
This project makes use of the excellent Lombok project. So to build in your favourite IDE, if necessary
head on over to [Lombok](https://projectlombok.org/) and click the appropriate "Install" link (tested with IntelliJ and Eclipse).

In preparation for playing with recent Java features such as virtual threads and pattern matching, the build or this project **now requires JDK21**. 

With appropriate versions of the JDK, Maven and a (optionally) Mongo installed, start with
~~~~
mvn clean compile test
~~~~
and then try
~~~~
mvn spring-boot:run
~~~~
To run a client to access the microservice, head on over to https://github.com/aidanwhiteley/books-web or see the section below on using Docker.

### Sample data
There is some sample data provided to make initial understanding of the functionality a bit easier.
It is is the /src/main/resources/sample_data. See the #README.txt in that directory.
The the details of above for the available Spring profiles to see when this sample data is auto loaded.

#### Indexes
The Mongo indexes required by the application are not "auto created" (except when running in Docker containers).
You should manually apply the indexes defined in /src/main/resources/indexes.
In particular, the application's Search functionality won't work unless you run the command to build
the weighted full text index across various fields of the Book collection. The rest of the application will run without 
indexes - just more slowly as the data volumes increase!

#### Admin emails
There is functionality to send an email to an admin when a new user has logged on. This is intended to prompt the
admin to give the new user the ROLE_EDITOR role (or delete the user!).
This functionality must be enabled - see the books.users.registrationAdminEmail entries in application.yml (where 
it is disabled by default). There's also a strong argument that having scheduled tasks runnable on each node is a poor option in an app that is trying to be "twelve factor" compliant - see https://12factor.net/admin-processes

## Levels of access
The code supports five access levels
* anonymous (never logged in)
* ROLE_USER (logged in but no more permissions than anonymous)
* ROLE_EDITOR (logged in with permission to create book reviews and comment on other people's book reviews)
* ROLE_ADMIN (logged in with full admin access)
* ROLE_ACTUATOR (logged in but with no permissions except to access Actuator endpoints)

The application-<profile>.yml files can be edited to automatically give a logged on user admin access 
by specifying their email on Google / Facebook. See the books:users:default:admin:email setting.

## Security
Lots of to and froing on this as the two of the main JWT related companies can't seem to agree on where to store a JWT token.
Stormpath (who joined forces with Okta) [say](https://stormpath.com/blog/where-to-store-your-jwts-cookies-vs-html5-web-storage) use cookies.
Auth0 [say](https://auth0.com/blog/cookies-vs-tokens-definitive-guide/) use local storage.

In my mind, it comes down to whether you are more scared of XSS or XSRF. Given the average marketing 
departments predilection to use their tag managers to include all sorts of random JavaScript, I'm more scared of XSS.

So this demo application stores the JWT token in a secure and "httpOnly" cookie. So that hopefully blocks XSS exploits (as the rogue JavaScript can't read the httpOnly cookie containing the JWT logon token). However, it leaves the application open
to XSRF exploits. To mitigate that, the application uses an XSRF filter and expects that a (non httpOnly) cookie will be
re-sent to the server side and, for state changing (non GET) requests, the value of the XSRF token must be added, via JavaScript, as an X-XSRF-TOKEN request header. The application will check that the two values are the same.

This works well (or seems to!) when the API and the HTML is on the same domain. When CORS is needed to call the API, this 
doesn't currently work. So only use this application with CORS configured (i.e. with no "front proxy") in development.
Don't use this application with CORS in production - it will leave you open to XSRF based attacks.

## OpenAPI 3 API documentation and swagger-ui

[![API Documentation](https://github.com/aidanwhiteley/books/blob/develop/src/main/resources/static/swagger-logo.png)](https://cloudybookclub.com/swagger-ui/index.html)

The public read only part of the application's REST API is automatically documented using the [springdoc-openapi](https://springdoc.org/)
tool to auto create OpenAPI 3 JSON. The API documentation can be explored and tested using the Swagger UI available [here](https://cloudybookclub.com/swagger-ui/index.html).

## Stateless Apps
A lot of the time developing this microservice was spent in making it entirely independent of HTTP session state  - based around issuing a 
JWT after the user has authenticated via Google / Facebook.

This turned out to be suprisingly difficult - with the cause of the difficulty mainly being in the Spring Boot OAuth2 implementation 
in Spring Boot 1.x. The Google/Facebook re-direct back to the microservice needed to hit the same session / JVM as I think that the 
Oauth2ClientContext was storing some state in http session. 

The current version of this application has moved to the Oauth2 functionality in Spring Security 5. While this greatly reduces the 
"boilerplate" code needed to logon via Google or Facebook it still, by default, stores data in the HTTP session (to validate the data in the redirect back from Google / Facebook).
However, it does allow configuration of your own AuthorizationRequestRepository meaning that it is possible to implement a cookie
based version. So, finally, this application is completely free of any HTTP session state! Which was the point of originally starting to write this 
microservice as I wanted to try it out on cloud implementations such as the Pivotal Cloud Foundry and AWS.

### Using JWT for session management
This demo app uses JWTs as the user's session token. In most cases this is now considered an 
anti-pattern ([pros and cons discussion](https://news.ycombinator.com/item?id=27136539)).
For my own usage (where I'm extremely unlikely to need to quickly revoke users) it's fine but I wouldn't use this solution 
for user session management again in the future.

> [!WARNING]  
> There was an bug in versions of this project prior to those using Spring Boot 3 i.e. 
> prior to 0.30.0-SNAPSHOT in that random plaintext was being passed to the JWT signing
> function rather than a SecretKey. Please see [this StackOverflow discussion](https://stackoverflow.com/questions/40252903/static-secret-as-byte-key-or-string/40274325#40274325) for more details.

## Docker
Docker images are available for the various tiers that make up the full application.
### Docker web tier
An nginx based Docker image (aidanwhiteley/books-web-angular) is available that hosts the AngularJS single page app
and acts as the reverse proxy through to the API tier (this application). See https://github.com/aidanwhiteley/books-web
for more details.
The checked in docker.compose.yml specifies the user of aidanwhiteley/books-web-angular-gateway which routes Ajax
call to the APIs via Spring Cloud Gateway based API gateway.
### API Gateway
A Spring Cloud Gateway based API gateway image is available to route web traffic on port 8000 through to 
(a cluster of) Spring Boot based books microservices.
Also provides throttling and load balancing amonst the available books microservices.
Requires a Service Registry to register with and find running books microservices.
### Service Registry 
A Spring / Netflix Eureka service registry in which instances of the books microservice register themselves and in
which the API gateway finds available instances of the books microservice.
### Java API tier
There is Google Jib created image (aidanwhiteley/books-api-java) for this application. 
The image can be recreated by running "mvn compile jib:dockerBuild".
Registers with Service Registry (dependant on the Spring profile used).
### MongoDB data tier
A MongoDB based Docker image (aidanwhiteley/books-db-mongodb or aidanwhiteley/books-db-mongodb-demodata) is available
to provide data tier required by this application.
Use the aidanwhiteley/books-db-mongodb-demodata to have sample data reloaded into the MongoDB every time the 
container is restarted.
See the src/main/resources/mongo-docker directory for Docker build of the data tier.
## Docker compose
There is a docker-compose.yml file in the root of this application. This starts Docker containers for all the above 
tiers of the overall application.
### .env file
The docker-compose file expects there to be a .env file in the same directory to define the environment 
variables expected by the various Docker images.
There is an example .env file with comments checked in. This **SHOULD** be edited according to the 
instructions in the file (noting that the containers will start and run OK with the checked in values if you are just trying things out).

## Docker Compose and running the overall application
The checked in docker-compose.yaml and .env file should result in a deployment as shown on the following diagram.

![Cloudy Docker Deployment Diagram](../media/docker1.png?raw=true)

If you have a recentish Docker available (>= 18.06.0), from the root of this project type:
~~~~
docker-compose pull
docker-compose up --scale api-tier-java=2
~~~~

Then try accessing http://localhost/ If you get 503 errors to start with, you should retry until all the tiers of the
application are up and running. You can tell when the application should be
up and running by viewing the status of the docker compose health check for the web tier e.g.

![Docker List Containers](../media/docker-ps.jpg?raw=true)
the status of books-web-angular-gateway should transition to "healthy" (from "health: starting" or "unhealthy") or
![Docker log output](../media/health-ok.jpg?raw=true)
the docker compose log output should start showing HTTP 200s (from 503s) for the calls
to the API called by the health check (i.e. /api/books).

Note 1: "docker-compose" is currently preferred compared to the more recently available "docker compose" sub command (the log output is preferable if nothing else).

Note 2: Mongo logging is currently turned off as it is quite verbose and obscures what this demo is trying to show. Edit the docker-compose.yaml to turn it back on for debugging or for production.

Note 3: If you want to persist data in Mongo between restarts of the container, rename the file docker-compose.override.yaml.persistent-data to docker-compose.override.yaml
If you do this and are running on Windows, make sure to read the Caveats section of https://hub.docker.com/_/mongo/

|Tier |URL |Notes and screen grab|
|-----|----|-----|
|Website|http://localhost/|Accessing this URL, if you left the docker-compose.yaml as is, you will be auto "logged on" as an admin user. Try the "ADD A BOOK" link and, after you have completed the "Title", you should see a list of matching books from Google Books.![Books web tier](../media/application.jpg?raw=true)|
|Service registry|http://localhost:8761/|The service registry isn't behind logon. You should see registered the API gateway, the service registry itself, two instances of the Books microservice and a Spring Boot Admin instance.![Books service registry](../media/service-registry.jpg?raw=true)|
|Spring Boot Admin|http://localhost:8888/|If you haven't changed the .env file, logon with anAdminUser-CHANGE_ME / aPassword-CHANGE_ME ![Books Spring Boot Admion](../media/spring-boot-admin.jpg?raw=true)|
|Mongo data tier|http://localhost:27017/|With the default docker-compose.yml this isn't exposed. Look for the comments in the file around about line 136 if you want to access the database directly (userids and passwords being in the .env file) ![Mongo](../media/mongo-compass.jpg?raw=true)|

### Notes
* The output from "docker-compose up" should be reasonably error free but sometimes exceptions are thrown/logged. This is most likely because of the difference between a Docker container being "up" and the service within it being available. To start with, when the startup console logging has subsided, try the website (http://localhost/) a few times to see if all depdendencies are now up and running. They should be.
* The above setup means the webserver, the API gateway and the service registry all single points of failure but there's only so much RAM on my PC!
* Oh - and the Docker db is neither shared nor replicated. So that's a SPoF as well.
* If necessary, subsitute "localhost" in the URLs above for whatever IP your Docker is running on.

## Spring Boot Admin
The application supports exposing [Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready.html) 
endpoints to be consumed by [Spring Boot Admin](http://codecentric.github.io/spring-boot-admin/current/). We need security applied to 
the Actuator end points but don't want to introduce another security layer into the application - we want to stick with the JWT based implemetation 
we already have. So we need Spring Boot Admin to be able to supply a suitable JWT token when calling the Actuator end points. 

To do this, set books.allow.actuator.user.creation to true and run the application. A JWT will be printed to the application logs for a 
user that **only** jas the Actuator role. Plug this JWT token into a Spring Boot Admin application that is configured to send the above JWT token with each 
request to the Actuator endpoints in this application. A extract of the required configuration of a class that
implements de.codecentric.boot.admin.server.web.client.HttpHeadersProvider is listed below 
with a fully working example project being available at https://github.com/aidanwhiteley/books-springbootadmin
```java
@Component
public class JwtHeaderProvider implements HttpHeadersProvider {
    
    @Override
    public HttpHeaders getHeaders(Instance instance) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, JWT_COOKIE_NAME + "=" + jwtTokenActuatorUser);
        return headers;
    }
}
```
* Set HTTP basic username/password values required when the client application registers with the Spring Boot Admin instance
    * in the client application (i.e. this application) by setting the spring.boot.admin.client.username/password values 
    * configure the Spring Boot admin application with the same values by setting spring.security.user.name/password

## The name

Why "The Cloudy BookClub"? Well - it's gong to run in the cloud innit. And I couldn't think
of any other domain names that weren't already taken.

## Client Side Functionality

There is an AngularJS based front end application that consumes the microservice available 
at https://github.com/aidanwhiteley

The running application can be seen at https://cloudybookclub.com/
