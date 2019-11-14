package flat.viewer.web;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDate;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static flat.viewer.web.FlatViewHandler.DATE_FORMATTER;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(VertxUnitRunner.class)
public class IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

    private Vertx vertx;
    private Integer port;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        port = 8080;
        socket.close();
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("http.port", port)
                );
        vertx.deployVerticle(FlatViewController.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testReserve() {

//                .thenReturn().asString();
//        assertThat(result).isEqualTo("{\"result\":\"Ok\"}");
//                .body("{\"t_id\":21, \"f_id\":45, \"start\":\"2019-08-17T12:20:00\", \"end\":\"2019-08-17T12:40:00\"}")
//                .request()
//                .contentType(JSON).accept(JSON)
//                .when()
        //                        .then().extract().body().as(String.class)
//                        .then().extract().body().asString();
//        System.out.println("3result!: " + result);

        String result = given()
                .body("{\"t_id\":11, \"f_id\":45}").contentType(JSON).accept(JSON)
                .post("/rent")
                .thenReturn().asString();
        assertThat(result).isEqualToIgnoringCase("{\"result\":\"Ok\"}");

        String start = LocalDate.now().plusDays(2).format(DATE_FORMATTER);
        given()
                .body(format("{\"t_id\":21, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reserve")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));

        given()
                .body(format("{\"t_id\":22, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reserve")
                .then().assertThat().body(equalTo("{\"result\":\"Occupied\"}"));
//        assertThat(result2).isEqualToIgnoringCase("{\"result\":\"Occupied\"}");

        given()
                .body(format("{\"t_id\":11, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/approve")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));

        given()
                .body(format("{\"t_id\":21, \"f_id\":45,\"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/cancel")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));
//                .thenReturn().asString();
//        assertThat(result3).isEqualToIgnoringCase("{\"result\":\"Ok\"}");

        given()
                .body(format("{\"t_id\":22, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reserve")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));

        given()
                .body(format("{\"t_id\":11, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reject")
                .then().assertThat().body(equalTo("{\"result\":\"Ok\"}"));
    }

    @Test
    public void checkThatWeCanAdd(TestContext context) {
        Async async = context.async();
//    final String json = Json.encodePrettily(new Whisky("Jameson", "Ireland"));
//    final String length = Integer.toString(json.length());
//    vertx.createHttpClient().post(port, "localhost", "/api/whiskies")
        vertx.createHttpClient().post(port, "localhost", "/start")
                .putHeader("content-type", "application/json")
//        .putHeader("content-length", length)
                .handler(response -> {
//          context.assertEquals(response.statusCode(), 201);
                    context.assertEquals(response.statusCode(), 200);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
//            final Whisky whisky = Json.decodeValue(body.toString(), Whisky.class);
                        final String whisky = Json.decodeValue(body.toString(), String.class);
                        System.out.println("1whisky!: " + whisky);
//            context.assertEquals(whisky.getName(), "Jameson");
//            context.assertEquals(whisky.getOrigin(), "Ireland");
//            context.assertNotNull(whisky.getId());
                        async.complete();
                    });
                })
//        .write(json)
//        .write("")
                .end();

        vertx.createHttpClient().getNow(port, "localhost", "/game/1/stat", response -> {
            context.assertEquals(response.statusCode(), 200);
//      context.assertEquals(response.headers().get("content-type"), "text/html;charset=UTF-8");
            context.assertEquals(response.headers().get("content-type"), "text/plain");
            response.bodyHandler(body -> {
                System.out.println("2whisky!: " + body);
//        context.assertTrue(body.toString().contains("<title>My Whisky Collection</title>"));
                async.complete();
            });
        });
    }
//        given()
//                .contentType(ContentType.TEXT)
//                .accept(ContentType.TEXT)
//                .request()
//                .contentType(ContentType.TEXT)
//                .post("/start")
//                .then().assertThat()
//                .contentType(ContentType.TEXT)
//                .body(equalTo("1"));

//        // Check that it has created an individual resource, and check the content.
//        post("/game/" + 1).then()
//                .assertThat()
//                .statusCode(200)
////            .body("name", equalTo("Jameson"))
////            .body("origin", equalTo("Ireland"))
////            .body("id", equalTo(result.getId()));
//                .body("id", equalTo(44));
////        // Delete the bottle
////        delete("/api/whiskies/" + result.getId()).then().assertThat().statusCode(204);
//        // Check that the resource is not available anymore
////        get("/api/whiskies/" + result.getId()).then()
//        get("/game/" + 11).then()
//                .assertThat()
//                .statusCode(404);

}
