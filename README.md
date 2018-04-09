# books
This project started as I wanted a simple "microservice" to use when trying out frameworks
such as Spring Cloud, Pivotal Cloud Foundy and AWS.

Its developed a little further such that it is starting to provide some functionality that may 
actually be useful.

So welcome to the "Cloudy Bookclub" microservice!

[![Build Status](https://travis-ci.org/aidanwhiteley/books.svg?branch=master)](https://travis-ci.org/aidanwhiteley/books) 
[![Sonar Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.aidanwhiteley%3Abooks&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.aidanwhiteley%3Abooks)
[![codecov](https://codecov.io/gh/aidanwhiteley/books/branch/master/graph/badge.svg)](https://codecov.io/gh/aidanwhiteley/books)


## Implementation

The main functionality included in the microservice includes
* based on latest Spring Boot 2
* Oauth2 based logon to
    * Google
    * Facebook
* the oauth2 logon data is transmogrified into locally stored users - with associated roles - and into a JWT token - 
making the web application entirely free of http session state (which has its pros and cons!)
* Spring Security for role based method level authorisation
* Mongo based persistence with the use of Spring Data MongoRepository 
    * next to no persistence code
    * except for some Mongo aggregation queries added to the Repository implementation
* Accessing the Google Books API with the Spring RestTemplate and, a work in progress, the reactive Spring WebClient

### Tests
All tests should run fine "out of the box". By default, the tests expect there to be a Mongo instance running locally. 
There is an option to run the tests using Fongo (an in memory Mongo replacement) - change the spring.profiles.active in application.yml to "fongo".
However, to run the project, Mongo is always required - even if tests are run against Fongo.

#### Stress Test
To examine how the WebClient code is behaving there is a maven plugin set up that runs a basic Gatling load test.
Run the command:

mvn gatling:test

The (Scala) source code of this test in at test/scala/com/aidanwhiteley/books/loadtest/StressTestSimulation1.scala

This is currently a "work in progress" and needs uplifting to the latest version of Gatling.

### How to run
A lot of the functionality is protected behind oauth2 authentication (via Google and Facebook). 
To use this, you must set up credentials (oauth2 client ids) on Google and Facebook.
You must then pass then make the clientId and clientSecret available to the running code.
There are "placeholders" for these in /src/main/resources/application.yml i.e. replace the existing
"NotInSCMx" (not in source code managament!) values with your own.
There are lots of other ways to pass in these values e.g. they can be passed as program arguments

--spring.security.oauth2.client.registration.google.client-id=xxxx --spring.security.oauth2.client.registration.google.client-secret=xxxx --spring.security.oauth2.client.registration.facebook.client-id=xxxx --spring.security.oauth2.client.registration.facebook.client-secret=xxxx 

Otherwise, see the Spring documentation for more options.

"Out of the box" the code runs with the "dev" Spring profile. When running in other environments you will need to decide the 
required profile and set some Spring parameters accordingly. For instance, you **WILL** want to 
set/change the secretKey used for the JWT token signing (see books:jwt:secretKey in the yml files).

You will also need access to a Mongo instance. The connection URL (in the yml files) will result in the automatic
creation of a Mongo database and the two required collections (dependant on the security config of your Mongo install).

### How to build
This project makes use of the excellent Lombok project. So to build in your favourite IDE, if necessary
head on over to [Lombok](https://projectlombok.org/) and click the appropriate "Install" link (tested with IntelliJ and Eclipse).

### Sample data
There is some sample data provided to make initial understanding of the functionality a bit easier.
It is is the /src/main/resources/sample_data. See the #README.txt in that directory.
The sample data is auto loaded when running with Spring profiles of "dev" (the checked in default)
and "integration".

#### Indexes
The Mongo indexes required by the application are also "auto created" when running in "dev" or "integration" profiles.
When running with other profiles, you should manually apply the indexes defined in /src/main/resources/indexes.
In particular, the application's Search functionality won't work unless you run the command to build
the weighted full text index across various fields of the Book collection. The rest of the application will run without 
indexes - just more slowly as the data volumes increase!

#### Admin emails
There is functionality to send an email to an admin when a new user has logged on. This is intended to prompt the
admin to give the new user the ROLE_EDITOR role (or delete the user!).
This functionality must be enabled - see the books.users.registrationAdminEmail entries in application.yml (where 
it is disabled by default).

## Levels of access
The code supports four access levels
* anonymous (never logged in)
* ROLE_USER (logged in but no more permissions than anonymous)
* ROLE_EDITOR (logged in and have been given permission to create & read book reviews)
* ROLE_ADMIN (logged in with full admin access)

The application-<env>.yml files can be edited to automatically give a logged on user admin access 
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


## Stateless Apps
A lot of the time developing this microservice was spent in making it entirely independant of HTTP session state  - based around issuing a 
JWT after the user has authenticated via Google / Facebook.

This turned out to be suprisingly difficult - with the cause of the difficulty mainly being in the Spring Boot OAuth2 implementation 
in Spring Boot 1.x. The Google/Facebook re-direct back to the microservice needed to hit the same session / JVM as I think that the 
Oauth2ClientContext was storing some state in http session. 

The current version of this application has moved to the Oauth2 functionality in Spring Security 5. While this greatly reduces the 
"boilerplate" code needed to logon via Google or Facebook it still, by default, stores data in the HTTP session (to validate the data in the redirect back from Google / Facebook).
However, it does allow configuration of your own AuthorizationRequestRepository meaning that it is possible to implement a cookie
based version. So, finally, this application is completely free of any HTTP session state! Which was the point of originally starting to write this 
microservice as I wanted to try it out on cloud implementations such as the Pivotal Cloud Foundry and AWS.

## Functionality

There is an Angular 1.x based front end that consumes the microservice that is available 
at https://github.com/aidanwhiteley

The running application can be seen at https://cloudybookclub.com/


## To-dos

The main "to do"s include
* more exploration of HATEOAS and HAL in the JSON APIs
* making the front end app a bit prettier!

## The name

Why "The Cloudy BookClub"? Well - it's gong to run in the cloud innit. And I couldnt think
of any other domain names that weren't already taken.


## Client functionality
![Screen shot](https://github.com/aidanwhiteley/books-web/blob/master/app/images/cloudy-book-club-screen-grab.jpg "Book review")

