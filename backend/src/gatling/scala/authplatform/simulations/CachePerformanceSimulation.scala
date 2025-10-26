package authplatform.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Cache performance test
 * Verifies that cached authorization requests meet p95 latency <10ms requirement
 */
class CachePerformanceSimulation extends Simulation {

  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val apiKey = System.getProperty("apiKey", "test-api-key-12345")

  // HTTP protocol configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .header("X-API-Key", apiKey)
    .userAgentHeader("Gatling Performance Test")

  // Fixed authorization request (should hit cache after first request)
  val cachedAuthRequest = StringBody("""
    {
      "userId": "cached-user-001",
      "organizationId": "org-001",
      "action": "READ",
      "resource": "/api/users/cached",
      "context": {
        "cached": true
      }
    }
  """)

  // Warm-up: prime the cache
  val warmupScenario = scenario("Cache Warmup")
    .exec(
      http("Prime Cache")
        .post("/api/v1/authorize")
        .body(cachedAuthRequest).asJson
        .check(status.is(200))
    )

  // Cached request scenario - should hit L1 (Caffeine) cache
  val cachedRequestScenario = scenario("Cached Authorization Requests")
    .exec(
      http("Cached Authorize")
        .post("/api/v1/authorize")
        .body(cachedAuthRequest).asJson
        .check(status.is(200))
        .check(jsonPath("$.decision").exists)
        .check(responseTimeInMillis.lte(10)) // p95 requirement: <10ms
    )

  // Mixed workload - 80% cached, 20% new requests
  val userIdFeeder = Iterator.continually(Map(
    "userId" -> (if (scala.util.Random.nextDouble() < 0.8) {
      s"cached-user-${scala.util.Random.nextInt(10)}" // 10 cached users (80%)
    } else {
      s"user-${scala.util.Random.nextInt(1000)}" // 1000 random users (20%)
    })
  ))

  val mixedAuthRequest = StringBody("""
    {
      "userId": "${userId}",
      "organizationId": "org-001",
      "action": "READ",
      "resource": "/api/users"
    }
  """)

  val mixedWorkloadScenario = scenario("Mixed Workload")
    .feed(userIdFeeder)
    .exec(
      http("Mixed Authorize")
        .post("/api/v1/authorize")
        .body(mixedAuthRequest).asJson
        .check(status.is(200))
    )

  // Test setup
  setUp(
    warmupScenario.inject(
      atOnceUsers(10) // Prime cache with 10 requests
    ),
    cachedRequestScenario.inject(
      nothingFor(5.seconds), // Wait for warmup
      constantUsersPerSec(1000) during (60.seconds) // High load on cached data
    ),
    mixedWorkloadScenario.inject(
      nothingFor(5.seconds),
      constantUsersPerSec(500) during (60.seconds) // Realistic mixed workload
    )
  ).protocols(httpProtocol)
    .assertions(
      // Cached requests should be very fast
      forAll.responseTime.percentile3.lt(10), // p95 < 10ms
      forAll.responseTime.percentile4.lt(20), // p99 < 20ms
      forAll.successfulRequests.percent.gt(99.9)
    )
}
