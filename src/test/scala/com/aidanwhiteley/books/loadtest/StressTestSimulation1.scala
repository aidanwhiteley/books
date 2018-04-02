package com.aidanwhiteley.books.loadtest

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
 
class StressTestSimulation1 extends Simulation {

    val rampUpTimeSecs = 20
    val testTimeSecs   = 120
    val noOfUsers      = 10 
    val minWaitMs      = 1000 milliseconds
    val maxWaitMs      = 3000 milliseconds
    val baseName     = "async-non-blocking"
    val requestName  = baseName + "-request"
    val scenarioName = baseName + "-scenario"

    val httpProtocol = http
        .baseURL("http://localhost:8080")
        .inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.(t|o)tf""", """.*\.png"""), WhiteList())

    val uri1 = "127.0.0.1"
    
    val http_headers = Map(
        "Accept-Encoding" -> "gzip,deflate",
        "Content-Type" -> "text/json;charset=UTF-8",
        "Keep-Alive" -> "115")

    val scn = scenario(scenarioName)
        .during(testTimeSecs) {
      exec(
        http(requestName)
          .get("/api/books/?page=0&size=5")
          .headers(http_headers)
          .check(status.is(200), regex(""""numberOfElements":5""") )
      )
      .pause(minWaitMs, maxWaitMs)
    }

    setUp(scn.inject(rampUsers(noOfUsers) over (rampUpTimeSecs seconds))).protocols(httpProtocol)
}