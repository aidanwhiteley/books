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


### To-dos

The main "to do"s include
* change from http sessions and jsessionid to stateless JWT
* more exploration of HATEOAS and HAL in the JSON APIs

## Functionality

There is an Angular 1.x based front end that consumes the microservice that is available 
at https://github.com/aidanwhiteley

The running application can be seen at TBC

## The name

Why "The Cloudy BookClub"? Well - it's gong to run in the cloud innit. And I couldnt think
of any other domain names that weren't already taken.