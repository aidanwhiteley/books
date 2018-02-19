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
* the oauth2 logon data is transmogrified into locally stored users - with associated roles - and into a JWT token - making the web application entirely statless
(which has its pros and cons!)
* Spring Security for role based method level authorisation
* Mongo based persistence with the use of Spring Data MongoRepository 
    * next to no persistence code
    * except for some Mongo aggregation queries added to the Repository implementation
* Accessing the Google Books API with the Spring RestTemplate

### Tests
All tests should run fine "out of the box". To make this simpler the integration tests (the vast majority of the tests) use Fongo - an in memory Mongo replacement

### How to run
A lot of the functionality is protected behind oauth2 authentication (via Google and Facebook). 
To use this, you must set up credentials (oauth2 client ids) on Google and Facebook.
You must then pass then make the clientId and clientSecret available to the running code.
There are "placeholders" for these in /src/main/resources/application.yml i.e. replace the existing
"NotInSCM" (not in source code managament!) values with your own.
There are lots of other ways to pass in these values e.g. they can be passed as program arguments

--google.client.clientSecret=xxxxxxxx --google.client.clientId=yyyyyyyy --facebook.client.clientSecret=aaaaaaaa --facebook.client.clientId=bbbbbbbb

Otherwise, see the Spring documentation for more options.

"Out of the box" the code runs with the "dev" Spring profile. When running in other environments you will need to decide the 
required profile and set some Spring parameters accordingly. For instance, you **WILL** want to 
set/change the secretKey used for the JWT token signing (see books:jwt:secretKey in the yml files).

You will also need access to a Mongo instance. The connection URL (in the yml files) will result in the automatic
creation of a Mongo database and the two required collections (dependant on the security config of your Mongo install).

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
by specifying their email on Google / Facebook. See the books:users:default:admin:email setting.


### To-dos

The main "to do"s include
* more exploration of HATEOAS and HAL in the JSON APIs
* making the front end app a bit prettier!

### Security
Lots of to and froing on this. When two of the main JWT related companies can't agree of where to store a JWT token, you know alarm bells should be ringing a little bit.
Stormpath (who got taken over by Okta) [say](https://stormpath.com/blog/where-to-store-your-jwts-cookies-vs-html5-web-storage) use cookies.
Auth0 [say] (https://auth0.com/blog/cookies-vs-tokens-definitive-guide/) use local storage.

In my mind, it comes down to whether you are more scared of XSS or XSRF. Given the average marketing 
departments predilection to use their tag managers to include all sorts of random JavaScript, I'm more scared of XSS.

So this demo application stores the JWT token in a secure and "httpOnly" cookie. So that blocks XSS exploits (as the rogue JavaScript cant read the httpOnly cookie containing the JWT logon token). However, it leaves the application open
to XSRF exploits. To mitigate that, the application uses an XSRF filter and expects that a (non httpOnly) cookie will be
sent to the server side and, for state changing (non GET) requests, the value of the XSRF token must be added, via JavaScript, as an X-XSRF-TOKEN request header. The application will check that the two values are the same.

This works well (or seems to!) when the API and the HTML is on the same domain. When CORS is needed to call the API, this 
doesn't currently work. So only use this application with CORS configured (i.e. with no "front proxy") in development.
Don't use this application with CORS in production - it will leave you open to XSRF based attacks.

## Functionality

There is an Angular 1.x based front end that consumes the microservice that is available 
at https://github.com/aidanwhiteley

The running application can be seen at https://cloudybookclub.com/

## The name

Why "The Cloudy BookClub"? Well - it's gong to run in the cloud innit. And I couldnt think
of any other domain names that weren't already taken.

## Client functionality
![Screen shot](https://github.com/aidanwhiteley/books-web/blob/master/app/images/cloudy-book-club-screen-grab.jpg "Book review")