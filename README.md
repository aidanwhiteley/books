# books
This project started as I wanted a simple "microservice" to use when trying out frameworks
such as Spring Cloud and AWS.

Its developed a little further such that it is starting to provide some functionality that may 
actually be useful.

So welcome to the "Cloudy Bookclub" microservice!

## Implementation

The main functionality included in the microservice includes
* based on latest Spring Boot
* Oauth2 based logon to
    * Google
    * Facebook
    * while also creating users locally so they can be assigned roles
* Spring Security for method level authorisation
* Mongo based persistence with the use of Spring Data MongoRepository 
    * next to no persistence code
    * except for some Mongo aggregation queries added to the Repository implmentation
* Accessing the Google Books API with the Spring RestTemplate

### Tests
All tests should run fine "out of the box". To make this simpler
* the integration tests (the vast majority of the tests) use Fongo - an in memory Mongo replacement
* the outh2 "logon via a social network" functionality is replaced, for the tests, by configuring in basic auth

### How to run
A lot of the functionality is protected behind oauth2 authentication (via Google and Facebook). 
To use this, you must set up credentials (oauth2 client ids) on Google and Facebook.
You must then pass then make the clientId and clientSecret available to the running code.
There are "placeholders" for these in /src/main/resources/application.yml i.e. replace the existing
"NotInSCM" (not in source code managament!) values with your own.
There are lots of other ways to pass in these values e.g. they can be passed as program arguments

--google.client.clientSecret=xxxxxxxx --google.client.clientId=yyyyyyyy --facebook.client.clientSecret=aaaaaaaa --facebook.client.clientId=bbbbbbbb

Otherwise, see the Spring documentation for more options.

### Sample data
There is some sample data provided to make initial understanding of the functionality a bit easier.
It is is the /src/main/resources/sample_data. See teh #README.txt in that directory.
The sample data is auto loaded when running with Spring profiles of "dev" (the checked in default)
and "integration".

### Admin access
The code supports four access levels
* anonymous (never logged in)
* ROLE_USER (logged in but no more permissions than anonymous)
* ROLE_EDITOR (logged in and have been given permission to create & read book reviews)
* ROLE_ADMIN (logged in with full admin access)

The application-<env>.yml files can be edited to automatically give a logged on user admin access 
by specifying there email on Google / Facebook. See the books:users:default:admin:email setting.

There's currently a bug with this functionality in the the user only gets properly
configured with admin access on the second time they log in.

### To-dos

The main "to do"s include
* change from http sessions and jsessionid to stateless JWT
* more exploration of HATEOAS and HAL in the JSON APIs
* more functionality e.g. supporting comments on book reviews

## Functionality

There is an Angular 1.x based front end that consumes the microservice that is available 
at https://github.com/aidanwhiteley

The running application can be seen at TBC

## The name

Why "The Cloudy BookClub"? Well - it's gong to run in the cloud innit. And I couldnt think
of any other domain names that weren't already taken.

## Client functionality
![Screen shot](https://github.com/aidanwhiteley/books-web/blob/master/app/images/cloudy-book-club-screen-grab.jpg "Book review")