package org.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ComicApiTest {

    @Test
    void health() {
        given().when()
                .get("/v1/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("repositoryKind", equalTo("local-resources"));
    }

    @Test
    void fixtureSeriesRoundTrip() {
        given().when()
                .get("/v1/series")
                .then()
                .statusCode(200)
                .body("total", greaterThanOrEqualTo(1));

        given().when()
                .get("/v1/series/fixture-series")
                .then()
                .statusCode(200)
                .body("id", equalTo("fixture-series"))
                .body("title", equalTo("Fixture Series"));

        given().when()
                .get("/v1/series/fixture-series/chapters")
                .then()
                .statusCode(200)
                .body("chapters.size()", equalTo(1));

        given().when()
                .get("/v1/series/fixture-series/chapters/chapter-001/pages")
                .then()
                .statusCode(200)
                .body("pages.size()", equalTo(1));

        given().when()
                .get("/v1/content/series/fixture-series/chapters/chapter-001/images/0")
                .then()
                .statusCode(200);

        given().when()
                .get("/v1/search?q=Fixture")
                .then()
                .statusCode(200)
                .body("total", greaterThanOrEqualTo(1));
    }

    @Test
    void unknownSeries404() {
        given().when().get("/v1/series/does-not-exist").then().statusCode(404);
    }
}
