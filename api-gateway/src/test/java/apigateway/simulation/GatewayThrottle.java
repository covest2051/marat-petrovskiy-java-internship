package apigateway.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class GatewayThrottle extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8085")
            .acceptHeader("application/json")
            .acceptEncodingHeader("gzip, deflate")
            .userAgentHeader("Mozilla/5.0 (Java Gatling Test)");

    ScenarioBuilder scn = scenario("API Gateway Load Test")
            .exec(http("LoginRequest")
                    .post("/auth/login")
                    .header("Content-Type", "application/json")
                    .body(StringBody("{\"email\":\"maratperovitch@gmail.com\",\"password\":\"123456\"}"))
                    .check(status().is(200))
                    .check(jsonPath("$.token").saveAs("jwtToken"))
            )

            .exec(http("GetUserRequest")
                    .get("/users/me")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .check(status().is(200))
            )

            .exec(http("CreateOrderRequest")
                    .post("/orders")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .header("Content-Type", "application/json")
                    .body(StringBody("{\"orderItems\":[{\"productId\":101,\"quantity\":2}]}"))
                    .check(status().is(201))
            )

            .exec(http("GetOrdersRequest")
                    .get("/orders/user")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .check(status().is(200))
            );

    {
        setUp(
                scn.injectOpen(
                        constantUsersPerSec(30).during(Duration.ofSeconds(20))
                ).protocols(httpProtocol)
        );
    }
}
