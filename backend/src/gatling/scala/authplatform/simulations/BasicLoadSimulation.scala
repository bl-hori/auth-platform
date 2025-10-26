package authplatform.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Basic load test simulation
 * Tests baseline performance with moderate load
 */
class BasicLoadSimulation extends Simulation {

  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val apiKey = System.getProperty("apiKey", "test-api-key-12345")

  // HTTP protocol configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .header("X-API-Key", apiKey)
    .userAgentHeader("Gatling Performance Test")

  // Define scenarios
  val scn = scenario("Basic Load Test")
    .exec(
      http("Health Check")
        .get("/actuator/health")
        .check(status.is(200))
    )
    .pause(1)
    .exec(
      http("Get Users")
        .get("/api/v1/users")
        .check(status.is(200))
        .check(jsonPath("$..id").exists)
    )
    .pause(1)
    .exec(
      http("Get Roles")
        .get("/api/v1/roles")
        .check(status.is(200))
    )
    .pause(1)
    .exec(
      http("Get Policies")
        .get("/api/v1/policies")
        .check(status.is(200))
    )

  // Load simulation
  setUp(
    scn.inject(
      rampUsers(50) during (30.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(5000),
      global.responseTime.mean.lt(1000),
      global.successfulRequests.percent.gt(95)
    )
}
