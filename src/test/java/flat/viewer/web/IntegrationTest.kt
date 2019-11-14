package flat.viewer.web

import com.jayway.restassured.RestAssured
import com.jayway.restassured.http.ContentType
import flat.viewer.web.FlatViewController
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

@RunWith(VertxUnitRunner::class)
class IntegrationTest {
    private var vertx: Vertx? = null
    @Before
    @Throws(IOException::class)
    fun setUp(context: TestContext) {
        vertx = Vertx.vertx()
        val options = DeploymentOptions()
                .setConfig(JsonObject().put("http.port", 8080))
        vertx!!.deployVerticle(FlatViewController::class.java.name, options, context.asyncAssertSuccess())
    }

    @After
    fun tearDown(context: TestContext) {
        vertx!!.close(context.asyncAssertSuccess())
    }

    @Test
    fun testReserve() {
        val result = RestAssured.given()
                .body("{\"t_id\":11, \"f_id\":45}").contentType(ContentType.JSON).accept(ContentType.JSON)
                .post("/rent")
                .thenReturn().asString()
        Assert.assertThat(result, CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        val start = LocalDate.now().plusDays(2).format(FlatViewHandler.DATE_FORMATTER)
        RestAssured.given()
                .body(String.format("{\"t_id\":21, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(ContentType.JSON).accept(ContentType.JSON)
                .post("/reserve")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        RestAssured.given()
                .body(String.format("{\"t_id\":22, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(ContentType.JSON).accept(ContentType.JSON)
                .post("/reserve")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Occupied\"}"))
        RestAssured.given()
                .body(String.format("{\"t_id\":11, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(ContentType.JSON).accept(ContentType.JSON)
                .post("/approve")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        RestAssured.given()
                .body(String.format("{\"t_id\":21, \"f_id\":45,\"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(ContentType.JSON).accept(ContentType.JSON)
                .post("/cancel")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        RestAssured.given()
                .body(String.format("{\"t_id\":22, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(ContentType.JSON).accept(ContentType.JSON)
                .post("/reserve")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
        RestAssured.given()
                .body(String.format("{\"t_id\":11, \"f_id\":45, \"date\":\"%s\", \"time\":\"12:20\", \"duration\":20}", start)).contentType(ContentType.JSON).accept(ContentType.JSON)
                .post("/reject")
                .then().assertThat().body(CoreMatchers.equalTo("{\"result\":\"Ok\"}"))
    }
}
