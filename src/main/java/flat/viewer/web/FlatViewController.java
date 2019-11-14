package flat.viewer.web;

import flat.viewer.FlatViewService;
import flat.viewer.NotificationService;
import flat.viewer.NotificationServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static java.time.temporal.ChronoUnit.MINUTES;

//import io.vertx.rxjava.core.AbstractVerticle;
//import io.vertx.rxjava.core.http.HttpServerRequest;
//import io.vertx.rxjava.ext.web.Router;
//import io.vertx.rxjava.ext.web.RoutingContext;

public class FlatViewController extends AbstractVerticle {

    private static final AtomicLong seq = new AtomicLong();

    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new FlatViewController());
    }

    @Override
    public void start() {
//        AuctionRepository repository = new AuctionRepository(vertx.sharedData());
//        FVValidator validator = new FVValidator(repository);
        NotificationService notificationService = new NotificationServiceImpl();
        FlatViewService flatViewService = new FlatViewService(notificationService);
        //        FVValidator validator = new FVValidator();
//        AuctionHandler handler = new AuctionHandler(repository, validator);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(20, 0);
        int duration = 20;
        FlatViewHandler handler = new FlatViewHandler(flatViewService, initTimeSlots(start, end, duration));

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().consumes(APPLICATION_JSON);
        router.route().produces(APPLICATION_JSON);

        router.post("/rent").handler(handler::handleRent);
        router.post("/reserve").handler(handler::handleReserve);
        router.post("/cancel").handler(handler::handleCancel);
        router.post("/approve").handler(handler::handleApprove);
        router.post("/reject").handler(handler::handleReject);

        vertx.createHttpServer().requestHandler(router).listen(PORT, HOST);


//        SharedData sharedData = vertx.sharedData();
//
//        sharedData.getLockWithTimeout("mylock", 10000, res -> {
//            if (res.succeeded()) {
//                // Got the lock!
//                Lock lock = res.result();
//
//                LocalMap<String, String> map1 = sharedData.getClusterWideMap("mymap1");
//                
//            } else {
//                // Failed to get lock
//            }
//        });

    }

    private static Set<Map.Entry<LocalTime, LocalTime>> initTimeSlots(LocalTime start, LocalTime end, int duration) {
        long minutes = MINUTES.between(start, end);
        long count = minutes / duration;
        List<Map.Entry<LocalTime, LocalTime>> timeSlots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int add = duration * i;
            timeSlots.add(new AbstractMap.SimpleEntry<>(start.plus(add, MINUTES), start.plus(add + duration, MINUTES)));
        }
        long left = minutes % duration;
        if (left > 0) {
            Map.Entry<LocalTime, LocalTime> entry = timeSlots.get(timeSlots.size() - 1);
            timeSlots.add(new AbstractMap.SimpleEntry<>(entry.getValue(), entry.getValue().plus(left, MINUTES)));
        }
        return new HashSet<>(timeSlots);
    }
}
