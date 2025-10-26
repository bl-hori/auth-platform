package authplatform.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Authorization endpoint load test
 * Tests the critical /authorize endpoint under high load
 */
class AuthorizationLoadSimulation extends Simulation {

  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val apiKey = System.getProperty("apiKey", "test-api-key-12345")

  // HTTP protocol configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .header("X-API-Key", apiKey)
    .userAgentHeader("Gatling Performance Test")

  // Authorization request payload
  val authorizationRequest = StringBody("""
    {
      "userId": "${userId}",
      "organizationId": "org-001",
      "action": "READ",
      "resource": "/api/users",
      "context": {
        "ip": "192.168.1.1",
        "userAgent": "Gatling"
      }
    }
  """)

  // Feeder for user IDs
  val userIdFeeder = Iterator.continually(Map("userId" -> s"user-${scala.util.Random.nextInt(1000)}"))

  // Authorization scenario
  val authorizationScenario = scenario("Authorization Requests")
    .feed(userIdFeeder)
    .exec(
      http("Authorize Request")
        .post("/api/v1/authorize")
        .body(authorizationRequest).asJson
        .check(status.is(200))
        .check(jsonPath("$.decision").in("ALLOW", "DENY"))
        .check(responseTimeInMillis.lte(100)) // Cache hits should be <100ms
    )

  // Batch authorization scenario
  val batchAuthorizationRequest = StringBody("""
    {
      "requests": [
        {
          "userId": "${userId}",
          "organizationId": "org-001",
          "action": "READ",
          "resource": "/api/users"
        },
        {
          "userId": "${userId}",
          "organizationId": "org-001",
          "action": "WRITE",
          "resource": "/api/roles"
        }
      ]
    }
  """)

  val batchAuthorizationScenario = scenario("Batch Authorization Requests")
    .feed(userIdFeeder)
    .exec(
      http("Batch Authorize Request")
        .post("/api/v1/authorize/batch")
        .body(batchAuthorizationRequest).asJson
        .check(status.is(200))
        .check(jsonPath("$.results[*].decision").exists)
    )

  // Load simulation - ramp up to 1000 requests/second
  setUp(
    authorizationScenario.inject(
      constantUsersPerSec(100) during (30.seconds),
      rampUsersPerSec(100) to 1000 during (60.seconds),
      constantUsersPerSec(1000) during (120.seconds)
    ),
    batchAuthorizationScenario.inject(
      constantUsersPerSec(50) during (30.seconds),
      rampUsersPerSec(50) to 500 during (60.seconds),
      constantUsersPerSec(500) during (120.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile3.lt(50),  // 95th percentile < 50ms
      global.responseTime.percentile4.lt(100), // 99th percentile < 100ms
      global.successfulRequests.percent.gt(99)
    )
}
