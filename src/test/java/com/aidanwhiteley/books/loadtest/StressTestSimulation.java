package com.aidanwhiteley.books.loadtest;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.nothingFor;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.core.CoreDsl.stressPeakUsers;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

@SuppressWarnings("this-escape")
public class StressTestSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("text/html,text/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .doNotTrackHeader("1")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .acceptEncodingHeader("gzip, deflate")
            .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");

    ScenarioBuilder scn = scenario("BasicSimulation")
            .exec(http("request_1")
                    .get("/api/books/?page=0&size=5")
                    .check(status().is(200))
                    .check(jsonPath("$.content").exists()))
            .pause(1,5);

    {
        setUp(
                scn.injectOpen(
                        nothingFor(4),
                        atOnceUsers(2),
                        rampUsers(3).during(5), // 3
                        constantUsersPerSec(3).during(5).randomized(),
                        rampUsersPerSec(3).to(9).during(10).randomized(),
                        stressPeakUsers(15).during(10)
        ).protocols(httpProtocol));
    }
}
