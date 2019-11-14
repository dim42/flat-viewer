package flat.viewer.web

import flat.viewer.FlatViewService
import flat.viewer.NotificationService
import flat.viewer.NotificationServiceImpl
import flat.viewer.TimeSlotsHelper
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import java.time.LocalTime

class FlatViewController : AbstractVerticle() {
    override fun start() {
        val notificationService: NotificationService = NotificationServiceImpl()
        val flatViewService = FlatViewService(notificationService)
        val start = LocalTime.of(10, 0)
        val end = LocalTime.of(20, 0)
        val duration = 20
        val handler = FlatViewHandler(flatViewService, TimeSlotsHelper.initTimeSlots(start, end, duration), vertx.sharedData())
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        router.route().consumes(HttpHeaderValues.APPLICATION_JSON.toString())
        router.route().produces(HttpHeaderValues.APPLICATION_JSON.toString())
        router.post("/rent").handler(handler::handleRent)
        router.post("/reserve").handler(handler::handleReserve)
        router.post("/cancel").handler(handler::handleCancel)
        router.post("/approve").handler(handler::handleApprove)
        router.post("/reject").handler(handler::handleReject)
        vertx.createHttpServer().requestHandler(router).listen(PORT, HOST)
    }

    companion object {
        private const val HOST = "localhost"
        private const val PORT = 8080
        @JvmStatic
        fun main(args: Array<String>) {
            Vertx.vertx().deployVerticle(FlatViewController())
        }
    }
}
