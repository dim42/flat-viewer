package flat.viewer.web

import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate


@RunWith(VertxUnitRunner::class)
class IntegrationTest {
    private lateinit var vertx: Vertx

    @get:Rule
    var runTestOnContextRule = RunTestOnContext()

    @Before
    @Throws(IOException::class)
    fun setUp(context: TestContext) {
        vertx = runTestOnContextRule.vertx()
        val options = DeploymentOptions()
                .setConfig(JsonObject().put("http.port", 8080))
        vertx.deployVerticle(FlatViewController::class.java.name, options, context.asyncAssertSuccess())
    }

    @After
    fun tearDown(context: TestContext) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    fun testReserve() {
        val result = given()
                .body("{\"t_id\":11, \"f_id\":45}").contentType(JSON).accept(JSON)
                .post("/rent")
                .thenReturn().asString()
        assertThat(result, CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        val start = LocalDate.now().plusDays(2).format(FlatViewHandler.DATE_FORMATTER)
        given()
                .body(String.format("{\"t_id\":21, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reserve")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        given()
                .body(String.format("{\"t_id\":22, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reserve")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Occupied\"}"))
        given()
                .body(String.format("{\"t_id\":11, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/approve")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        given()
                .body(String.format("{\"t_id\":21, \"f_id\":45,\"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/cancel")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        given()
                .body(String.format("{\"t_id\":22, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reserve")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        given()
                .body(String.format("{\"t_id\":11, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(JSON).accept(JSON)
                .post("/reject")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
    }
}
