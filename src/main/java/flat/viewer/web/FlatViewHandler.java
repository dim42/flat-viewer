package flat.viewer.web;

import flat.viewer.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static java.time.temporal.ChronoUnit.MINUTES;

public class FlatViewHandler {
    private static final Logger log = LoggerFactory.getLogger(FlatViewHandler.class);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private static final String FLAT_VIEW_TO_NEW_TENANT_LOCK = "flatViewToNewTenantLock";
    private static final String FLAT_VIEW_TO_NEW_TENANT_MAP = "flatViewToNewTenantMap";
    private static final String FLAT_TO_CURRENT_TENANT_MAP = "flatToCurrentTenantMap";
    private static final int LOCK_TIMEOUT = 100;

    private final FlatViewService flatViewService;
    private final Set<Entry<LocalTime, LocalTime>> timeSlots;
    private final SharedData sharedData;

    public FlatViewHandler(FlatViewService flatViewService, Set<Entry<LocalTime, LocalTime>> timeSlots, SharedData sharedData) {
        this.flatViewService = flatViewService;
        this.timeSlots = timeSlots;
        this.sharedData = sharedData;
    }

    public void handleRent(RoutingContext context) {
        processAndRespond2(context, this::callRent);
//        processAndRespond(context, this::callRent);
    }

    private void processAndRespond(RoutingContext context, Function<JsonObject, Result> function) {
        try {
            JsonObject body = context.getBodyAsJson();
            Result result = function.apply(body);
            JsonObject json = new JsonObject().put("result", result);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        } catch (Exception e) {
            log.error("Request error", e);
            JsonObject json = new JsonObject().put("error", e + (e.getCause() != null ? ", cause:" + e.getCause() : ""));
            context.response().setStatusCode(422).end(json.encode());
        }
    }
    private void processAndRespond2(RoutingContext context, Function<SimpleEntry<JsonObject,SharedData>, Result> function) {
        Integer tenantId = context.getBodyAsJson().getInteger("t_id");
        log.info("Rent Start!:" + tenantId);
//            log.info("Start!:");

        final Result[] result = new Result[]{Result.Error};
        JsonObject body = context.getBodyAsJson();

//        Integer tenantId = body.getInteger("t_id");
//        Integer flatId = body.getInteger("f_id");
        sharedData.getLockWithTimeout(FLAT_VIEW_TO_NEW_TENANT_LOCK, LOCK_TIMEOUT, lockRes -> {
            log.info("Rent Lock!:" + tenantId);
//            log.info("Lock!:");
            if (lockRes.succeeded()) {

                LocalMap<Integer, Integer> flatToCurrentTenant = sharedData.getLocalMap(FLAT_TO_CURRENT_TENANT_MAP);
                LocalMap<FlatViewSlot, ViewSlot> mymap = sharedData.getLocalMap(FLAT_VIEW_TO_NEW_TENANT_MAP);

//                Integer tenantId = body.getInteger("t_id");
                Integer flatId = body.getInteger("f_id");
//        return flatViewService.approve(flatId, getDateTime(body), tenantId);
//                Result result = function.apply(body);

                result[0] =  function.apply(new SimpleEntry<>(body,sharedData));
//                result[0] = flatViewService.tryReserve(flatId, start, tenantId, mymap);

                lockRes.result().release();

            } else {
                // Failed to get lock
//                future.fail(lockRes.cause());
                // log
                System.out.println("lockRes!:" + lockRes);

            }
            log.info("Rent Res!:" + result[0] + " " + tenantId);
//                log.info("Res!:"+result[0]+" ");
//                future.completeExceptionally("error");
            JsonObject json = new JsonObject().put("result", result[0]);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        });
    }

    
    private Result callRent(SimpleEntry<JsonObject, SharedData> entry) {
        JsonObject body = entry.getKey();
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        SharedData sharedData = entry.getValue();
        LocalMap<Integer, Integer> flatToCurrentTenant = sharedData.getLocalMap(FLAT_TO_CURRENT_TENANT_MAP);
        return flatViewService.rent(flatId, tenantId, flatToCurrentTenant);
    }

    public void handleReserve(RoutingContext context) {
//        try {
//            Result result = function.apply(body);
//        JsonObject body = context.getBodyAsJson();
        Integer tenantId = context.getBodyAsJson().getInteger("t_id");
        log.info("Reserve Start!:" + tenantId);
//            log.info("Start!:");

        final Result[] result = new Result[]{Result.Error};

        sharedData.getLockWithTimeout(FLAT_VIEW_TO_NEW_TENANT_LOCK, LOCK_TIMEOUT, lockRes -> {
            log.info("Reserve Lock!:" + tenantId);
//            log.info("Lock!:");
            if (lockRes.succeeded()) {

                LocalMap<FlatViewSlot, ViewSlot> mymap = sharedData.getLocalMap(FLAT_VIEW_TO_NEW_TENANT_MAP);
                log.info("Reserve Map!:" + tenantId);

                JsonObject body = context.getBodyAsJson();

//                Result result1 = callReserve(body);

//            Integer tenantId = body.getInteger("t_id");
                Integer flatId = body.getInteger("f_id");
                LocalDateTime start = getDateTime(body);
                result[0] = flatViewService.tryReserve(flatId, start, tenantId, mymap);

//                    mymap.compute(new FlatViewSlot(flatId, start), (start_, viewSlot_) -> {
//                        if (viewSlot_ == null) {
//                            viewSlot_ = new ViewSlot(start, tenantId);
//                            viewSlot_.setState(RESERVING);
//                            notificationService.subscribeNew(flatId, viewSlot_);
//                            notificationService.notifyCurrent(flatId, viewSlot_);
//                            result[0] = Ok;
//                        } else {
//                            if (viewSlot_.getState() == REJECTED) {
//                                result[0] = Rejected;
//                            } else {
//                                result[0] = Occupied;
//                            }
//                        }
//                        log.info("Inside!:"+tenantId);
//                        return viewSlot_;
//                    });

                lockRes.result().release();

            } else {
                // Failed to get lock
//                future.fail(lockRes.cause());
                // log
                System.out.println("lockRes!:" + lockRes);

            }
            log.info("Reserve Res!:" + result[0] + " " + tenantId);
//                log.info("Res!:"+result[0]+" ");
//                future.completeExceptionally("error");
            JsonObject json = new JsonObject().put("result", result[0]);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        });

//            JsonObject json = new JsonObject().put("result", result);
//            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
//        } catch (Exception e) {
//            log.error("Request error", e);
//            JsonObject json = new JsonObject().put("error", e + (e.getCause() != null ? ", cause:" + e.getCause() : ""));
//            context.response().setStatusCode(422).end(json.encode());
//        }
//        
//        processAndRespond(context, this::callReserve);
    }

    private Result callReserve(JsonObject body) {
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        return flatViewService.tryReserve(flatId, getDateTime(body), tenantId, null);
    }

    private LocalDateTime getDateTime(JsonObject body) {
        LocalDate date = LocalDate.parse(body.getString("date"), DATE_FORMATTER);
        LocalTime start = LocalTime.parse(body.getString("time"), TIME_FORMATTER);
        LocalTime end = start.plus(body.getInteger("duration"), MINUTES);
        validate(start, end);
        return LocalDateTime.of(date, start);
    }

    private void validate(LocalTime start, LocalTime end) {
        if (!timeSlots.contains(new SimpleEntry<>(start, end))) {
            throw new IllegalArgumentException("Wrong slot");
        }
    }

    public void handleCancel(RoutingContext context) {
        Integer tenantId = context.getBodyAsJson().getInteger("t_id");
        log.info("Cancel Start!:" + tenantId);
//            log.info("Start!:");

        final Result[] result = new Result[]{Result.Error};
        JsonObject body = context.getBodyAsJson();

//        Integer tenantId = body.getInteger("t_id");
//        Integer flatId = body.getInteger("f_id");
        sharedData.getLockWithTimeout(FLAT_VIEW_TO_NEW_TENANT_LOCK, LOCK_TIMEOUT, lockRes -> {
            log.info("Cancel Lock!:" + tenantId);
//            log.info("Lock!:");
            if (lockRes.succeeded()) {

                LocalMap<FlatViewSlot, ViewSlot> mymap = sharedData.getLocalMap(FLAT_VIEW_TO_NEW_TENANT_MAP);

                Integer flatId = body.getInteger("f_id");

                result[0] = flatViewService.cancel(flatId, getDateTime(body), tenantId, mymap);

                lockRes.result().release();

            } else {
                // Failed to get lock
//                future.fail(lockRes.cause());
                // log
                System.out.println("lockRes!:" + lockRes);

            }
            log.info("Cancel Res!:" + result[0] + " " + tenantId);
//                log.info("Res!:"+result[0]+" ");
//                future.completeExceptionally("error");
            JsonObject json = new JsonObject().put("result", result[0]);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        });
        //        processAndRespond(context, this::callCancel);
    }

    private Result callCancel(JsonObject body) {
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        return flatViewService.cancel(flatId, getDateTime(body), tenantId, null);
    }

    public void handleApprove(RoutingContext context) {
        Integer tenantId = context.getBodyAsJson().getInteger("t_id");
        log.info("Approve Start!:" + tenantId);
//            log.info("Start!:");

        final Result[] result = new Result[]{Result.Error};
        JsonObject body = context.getBodyAsJson();

//        Integer tenantId = body.getInteger("t_id");
//        Integer flatId = body.getInteger("f_id");
        sharedData.getLockWithTimeout(FLAT_VIEW_TO_NEW_TENANT_LOCK, LOCK_TIMEOUT, lockRes -> {
            log.info("Approve Lock!:" + tenantId);
//            log.info("Lock!:");
            if (lockRes.succeeded()) {

                LocalMap<Integer, Integer> flatToCurrentTenant = sharedData.getLocalMap(FLAT_TO_CURRENT_TENANT_MAP);
                LocalMap<FlatViewSlot, ViewSlot> mymap = sharedData.getLocalMap(FLAT_VIEW_TO_NEW_TENANT_MAP);

//                Integer tenantId = body.getInteger("t_id");
                Integer flatId = body.getInteger("f_id");
//        return flatViewService.approve(flatId, getDateTime(body), tenantId);
                result[0] = flatViewService.approve(flatId, getDateTime(body), tenantId, flatToCurrentTenant, mymap);

                lockRes.result().release();

            } else {
                // Failed to get lock
//                future.fail(lockRes.cause());
                // log
                System.out.println("lockRes!:" + lockRes);

            }
            log.info("Approve Res!:" + result[0] + " " + tenantId);
//                log.info("Res!:"+result[0]+" ");
//                future.completeExceptionally("error");
            JsonObject json = new JsonObject().put("result", result[0]);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        });
//        processAndRespond(context, this::callApprove);
    }

    private Result callApprove(JsonObject body) {
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        return flatViewService.approve(flatId, getDateTime(body), tenantId, null, null);
    }

    public void handleReject(RoutingContext context) {
        Integer tenantId = context.getBodyAsJson().getInteger("t_id");
        log.info("Reject Start!:" + tenantId);
//            log.info("Start!:");

        final Result[] result = new Result[]{Result.Error};
        JsonObject body = context.getBodyAsJson();

//        Integer tenantId = body.getInteger("t_id");
//        Integer flatId = body.getInteger("f_id");
        sharedData.getLockWithTimeout(FLAT_VIEW_TO_NEW_TENANT_LOCK, LOCK_TIMEOUT, lockRes -> {
            log.info("Reject Lock!:" + tenantId);
//            log.info("Lock!:");
            if (lockRes.succeeded()) {

                LocalMap<Integer, Integer> flatToCurrentTenant = sharedData.getLocalMap(FLAT_TO_CURRENT_TENANT_MAP);
                LocalMap<FlatViewSlot, ViewSlot> mymap = sharedData.getLocalMap(FLAT_VIEW_TO_NEW_TENANT_MAP);

//                Integer tenantId = body.getInteger("t_id");
                Integer flatId = body.getInteger("f_id");
//        return flatViewService.approve(flatId, getDateTime(body), tenantId);
                result[0] = flatViewService.reject(flatId, getDateTime(body), tenantId, flatToCurrentTenant, mymap);

                lockRes.result().release();

            } else {
                // Failed to get lock
//                future.fail(lockRes.cause());
                // log
                System.out.println("lockRes!:" + lockRes);

            }
            log.info("Reject Res!:" + result[0] + " " + tenantId);
//                log.info("Res!:"+result[0]+" ");
//                future.completeExceptionally("error");
            JsonObject json = new JsonObject().put("result", result[0]);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        });
//        processAndRespond(context, this::callReject);
    }

    private Result callReject(JsonObject body) {
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        return flatViewService.reject(flatId, getDateTime(body), tenantId, null, null);
    }
}
