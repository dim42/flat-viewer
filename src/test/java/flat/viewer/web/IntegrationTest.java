package flat.viewer.web;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.time.LocalDate;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static flat.viewer.web.FlatViewHandler.DATE_FORMATTER;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(VertxUnitRunner.class)
public class IntegrationTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("http.port", 8080));
        vertx.deployVerticle(FlatViewController.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testReserve() {
        String result = given()
                .body("{\"t_id\":11, \"f_id\":45}").contentType(JSON).accept(JSON)
                .post("/rent")
                .thenReturn().asString();
        assertThat(result, equalTo("{\"result\":\"Ok\"}"));

        String start = LocalDate.now().plusDays(2).format(DATE_FORMATTER);
        given()
                .body(format("{\"t_id\":21, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reserve")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));

        given()
                .body(format("{\"t_id\":22, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reserve")
                .then().assertThat().body(equalTo("{\"result\":\"Occupied\"}"));

        given()
                .body(format("{\"t_id\":11, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/approve")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));

        given()
                .body(format("{\"t_id\":21, \"f_id\":45,\"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/cancel")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));

        given()
                .body(format("{\"t_id\":22, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reserve")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));

        given()
                .body(format("{\"t_id\":11, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reject")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));
    }
}
