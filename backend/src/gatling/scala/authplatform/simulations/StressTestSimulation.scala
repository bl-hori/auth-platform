package authplatform.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Stress test simulation
 * Tests system behavior under extreme load (10,000+ req/s)
 */
class StressTestSimulation extends Simulation {

  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val apiKey = System.getProperty("apiKey", "test-api-key-12345")

  // HTTP protocol configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .header("X-API-Key", apiKey)
    .userAgentHeader("Gatling Stress Test")
    .shareConnections // Reuse connections

  // User feeder
  val userFeeder = Iterator.continually(Map(
    "userId" -> s"user-${scala.util.Random.nextInt(10000)}",
    "orgId" -> s"org-${scala.util.Random.nextInt(100)}"
  ))

  // Authorization request
  val authRequest = StringBody("""
    {
      "userId": "${userId}",
      "organizationId": "${orgId}",
      "action": "READ",
      "resource": "/api/users"
    }
  """)

  // Scenarios
  val authScenario = scenario("Authorization Stress")
    .feed(userFeeder)
    .exec(
      http("Authorize")
        .post("/api/v1/authorize")
        .body(authRequest).asJson
        .check(status.in(200, 429)) // Allow rate limiting
    )

  val userScenario = scenario("User API Stress")
    .exec(
      http("List Users")
        .get("/api/v1/users")
        .queryParam("page", 0)
        .queryParam("size", 20)
        .check(status.in(200, 429))
    )

  val roleScenario = scenario("Role API Stress")
    .exec(
      http("List Roles")
        .get("/api/v1/roles")
        .check(status.in(200, 429))
    )

  val policyScenario = scenario("Policy API Stress")
    .exec(
      http("List Policies")
        .get("/api/v1/policies")
        .check(status.in(200, 429))
    )

  // Stress test load pattern
  // Phase 1: Warm up (0-30s) - 0 to 1000 req/s
  // Phase 2: Ramp up (30-90s) - 1000 to 5000 req/s
  // Phase 3: Peak load (90-210s) - 5000 to 10000 req/s
  // Phase 4: Sustained (210-330s) - 10000 req/s
  // Phase 5: Spike (330-360s) - 15000 req/s
  // Phase 6: Cool down (360-390s) - 15000 to 1000 req/s

  setUp(
    authScenario.inject(
      rampUsersPerSec(0) to 500 during (30.seconds),
      rampUsersPerSec(500) to 2500 during (60.seconds),
      rampUsersPerSec(2500) to 5000 during (120.seconds),
      constantUsersPerSec(5000) during (120.seconds),
      rampUsersPerSec(5000) to 7500 during (30.seconds),
      rampUsersPerSec(7500) to 500 during (30.seconds)
    ),
    userScenario.inject(
      rampUsersPerSec(0) to 200 during (30.seconds),
      rampUsersPerSec(200) to 1000 during (60.seconds),
      rampUsersPerSec(1000) to 2000 during (120.seconds),
      constantUsersPerSec(2000) during (120.seconds),
      rampUsersPerSec(2000) to 3000 during (30.seconds),
      rampUsersPerSec(3000) to 200 during (30.seconds)
    ),
    roleScenario.inject(
      rampUsersPerSec(0) to 100 during (30.seconds),
      rampUsersPerSec(100) to 500 during (60.seconds),
      rampUsersPerSec(500) to 1000 during (120.seconds),
      constantUsersPerSec(1000) during (120.seconds),
      rampUsersPerSec(1000) to 1500 during (30.seconds),
      rampUsersPerSec(1500) to 100 during (30.seconds)
    ),
    policyScenario.inject(
      rampUsersPerSec(0) to 100 during (30.seconds),
      rampUsersPerSec(100) to 500 during (60.seconds),
      rampUsersPerSec(500) to 1000 during (120.seconds),
      constantUsersPerSec(1000) during (120.seconds),
      rampUsersPerSec(1000) to 1500 during (30.seconds),
      rampUsersPerSec(1500) to 100 during (30.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      // Less strict assertions for stress test
      global.responseTime.percentile3.lt(500),  // p95 < 500ms under stress
      global.responseTime.percentile4.lt(2000), // p99 < 2s under stress
      global.successfulRequests.percent.gt(90)  // Allow some failures under extreme load
    )
}
