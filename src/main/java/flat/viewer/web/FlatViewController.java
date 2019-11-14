package flat.viewer.web;

import flat.viewer.FlatViewService;
import flat.viewer.NotificationService;
import flat.viewer.NotificationServiceImpl;
import flat.viewer.TimeSlotsHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.time.LocalTime;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class FlatViewController extends AbstractVerticle {

    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new FlatViewController());
    }

    @Override
    public void start() {
        NotificationService notificationService = new NotificationServiceImpl();
        FlatViewService flatViewService = new FlatViewService(notificationService);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(20, 0);
        int duration = 20;
        FlatViewHandler handler = new FlatViewHandler(flatViewService, TimeSlotsHelper.initTimeSlots(start, end, duration), vertx.sharedData());

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().consumes(APPLICATION_JSON.toString());
        router.route().produces(APPLICATION_JSON.toString());

        router.post("/rent").handler(handler::handleRent);
        router.post("/reserve").handler(handler::handleReserve);
        router.post("/cancel").handler(handler::handleCancel);
        router.post("/approve").handler(handler::handleApprove);
        router.post("/reject").handler(handler::handleReject);

        vertx.createHttpServer().requestHandler(router).listen(PORT, HOST);
    }
}
