package flat.viewer.web;

import flat.viewer.FlatViewService;
import flat.viewer.Result;
import io.vertx.core.json.JsonObject;
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

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static java.time.temporal.ChronoUnit.MINUTES;

//import io.vertx.rxjava.ext.web.RoutingContext;

public class FlatViewHandler {
    private static final Logger log = LoggerFactory.getLogger(FlatViewHandler.class);

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final FlatViewService flatViewService;
    private final Set<Entry<LocalTime, LocalTime>> timeSlots;

    public FlatViewHandler(FlatViewService flatViewService, Set<Entry<LocalTime, LocalTime>> timeSlots) {
        this.flatViewService = flatViewService;
        this.timeSlots = timeSlots;
    }

    public void handleRent(RoutingContext context) {
        JsonObject json;
        try {
            JsonObject body = context.getBodyAsJson();
            Integer tenantId = body.getInteger("t_id");
            Integer flatId = body.getInteger("f_id");
            Result result = flatViewService.rent(flatId, tenantId);
            json = new JsonObject().put("result", result);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        } catch (Exception e) {
            log.error("Request error", e);
            json = new JsonObject().put("error", e + (e.getCause() != null ? ", cause:" + e.getCause() : ""));
            context.response().setStatusCode(422).end(json.encode());
        }
    }

    public void handleReserve(RoutingContext context) {
        JsonObject json;
        try {
            JsonObject body = context.getBodyAsJson();
            Integer tenantId = body.getInteger("t_id");
            Integer flatId = body.getInteger("f_id");
            Result result = flatViewService.tryReserve(flatId, getDateTime(body), tenantId);
            json = new JsonObject().put("result", result);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        } catch (Exception e) {
            log.error("Request error", e);
            json = new JsonObject().put("error", e + (e.getCause() != null ? ", cause:" + e.getCause() : ""));
            context.response().setStatusCode(422).end(json.encode());
        }
    }

    private LocalDateTime getDateTime(JsonObject body) {
        LocalDate date = LocalDate.parse(body.getString("date"), DATE_FORMATTER);
        LocalTime start = LocalTime.parse(body.getString("time"), TIME_FORMATTER);
        LocalTime end = start.plus(body.getInteger("duration"), MINUTES);
        validate(start, end);
        return LocalDateTime.of(date, start);
    }

//            String startParam = body.getString("start");
//            LocalDateTime st = LocalDateTime.parse(startParam, DATE_TIME_FORMATTER);
//            String endParam = body.getString("end");
//            LocalDateTime end = LocalDateTime.parse(endParam, DATE_TIME_FORMATTER);

    private void validate(LocalTime start, LocalTime end) {
        if (!timeSlots.contains(new SimpleEntry<>(start, end))) {
            throw new IllegalArgumentException("Wrong slot");
        }
    }

    public void handleCancel(RoutingContext context) {
        JsonObject json;
        try {
            JsonObject body = context.getBodyAsJson();
            Integer tenantId = body.getInteger("t_id");
            Integer flatId = body.getInteger("f_id");
//            String startParam = body.getString("start");
//            LocalDateTime start = LocalDateTime.parse(startParam, DATE_TIME_FORMATTER);
            Result result = flatViewService.cancel(flatId, getDateTime(body), tenantId);
            json = new JsonObject().put("result", result);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        } catch (Exception e) {
            log.error("Request error", e);
            json = new JsonObject().put("error", e + (e.getCause() != null ? ", cause:" + e.getCause() : ""));
            context.response().setStatusCode(422).end(json.encode());
        }
    }

    public void handleApprove(RoutingContext context) {
        JsonObject json;
        try {
            JsonObject body = context.getBodyAsJson();
            Integer tenantId = body.getInteger("t_id");
            Integer flatId = body.getInteger("f_id");
//            String startParam = body.getString("start");
//            LocalDateTime start = LocalDateTime.parse(startParam, DATE_TIME_FORMATTER);
//                LocalDateTime of()
            Result result = flatViewService.approve(flatId, getDateTime(body), tenantId);
            json = new JsonObject().put("result", result);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        } catch (Exception e) {
            log.error("Request error", e);
            json = new JsonObject().put("error", e + (e.getCause() != null ? ", cause:" + e.getCause() : ""));
            context.response().setStatusCode(422).end(json.encode());
        }
    }

    public void handleReject(RoutingContext context) {
        JsonObject json;
        try {
            JsonObject body = context.getBodyAsJson();
            Integer tenantId = body.getInteger("t_id");
            Integer flatId = body.getInteger("f_id");
//            String startParam = body.getString("start");
//            LocalDateTime start = LocalDateTime.parse(startParam, DATE_TIME_FORMATTER);
            Result result = flatViewService.reject(flatId, getDateTime(body), tenantId);
            json = new JsonObject().put("result", result);
            context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
        } catch (Exception e) {
            log.error("Request error", e);
            json = new JsonObject().put("error", e + (e.getCause() != null ? ", cause:" + e.getCause() : ""));
            context.response().setStatusCode(422).end(json.encode());
        }
    }
}
