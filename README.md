# books
This project started as I wanted a simple "microservice" to use when trying out frameworks
such as Spring Cloud and AWS.

Its developed a little further such that it is starting to provide some functionality that may 
actually be useful.

So welcome to the "Cloudy Bookclub" microservice!

[![Sonar Quality Gate](https://sonarcloud.io/api/project_badges/quality_gate?project=com.aidanwhiteley%3Abooks)](https://sonarcloud.io/dashboard?id=com.aidanwhiteley%3Abooks)

## Implementation

The main functionality included in the microservice includes
* based on latest Spring Boot
* Oauth2 based logon to
    * Google
    * Facebook
* the oauth2 logon data is transmogrified into locally stored users - with associated roles - and into a JWT token - 
making the web application almost entirely statless (which has its pros and cons!)
* Spring Security for role based method level authorisation
* Mongo based persistence with the use of Spring Data MongoRepository 
    * next to no persistence code
    * except for some Mongo aggregation queries added to the Repository implementation
* Accessing the Google Books API with the Spring RestTemplate

### Tests
All tests should run fine "out of the box". However, the tests expect there to be a Mongo instance running locally.

There is an option to run the tests using Fongo (an in memory Mongo replacement) in which case no local instance of Mongo is required.
To enable this, uncomment all the code in the /src/test/java/com/aidanwhiteley/books/repository/config/FongoConfig class.
However, the use of Fongo is not currently the default option because
* there are currently problems using Fongo and the version of the Mongo drivers that default with Spring Boot 2 - see https://github.com/fakemongo/fongo/issues/316
* this project uses Mongo full text indexes across multiple fields and Fongo currently only supports this functionality on one field

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
An lot of the time developing this microservice was spent in trying to make it entirely independant of HTTP session state  - based around using JWTs.

This has almost worked! However, there's a problem with using Spring's OAuth2ClientContext which stores data into Session scope I believe. 
This is a WIP investigation at the moment. It looks as though the Google/Facebook re-direct back to the microservice needs to hit the same JVM as I think that the
Oauth2ClientContext is storing some state in http session.
Currently reading https://github.com/spring-projects/spring-security-oauth/issues/661 and looking at @SessionAttribute options.

Would I do "entirely HTTP session stateless" in a real application?

Almost certainly - **NO!**

I doubt I'll be working with one of the 17 companies that truly require "internet scale stateless applications" anytime soon. In the meantime, I'd recommend staying religious about the size of the data in the http session, continuing to use your existing "sticky session" solution and having conversations with your requirements owner about what happens when a user's http session disappears as a node goes offline for whatever reason. 

## Functionality

There is an Angular 1.x based front end that consumes the microservice that is available 
at https://github.com/aidanwhiteley

The running application can be seen at https://cloudybookclub.com/

## The name

Why "The Cloudy BookClub"? Well - it's gong to run in the cloud innit. And I couldnt think
of any other domain names that weren't already taken.


## Client functionality
![Screen shot](https://github.com/aidanwhiteley/books-web/blob/master/app/images/cloudy-book-club-screen-grab.jpg "Book review")