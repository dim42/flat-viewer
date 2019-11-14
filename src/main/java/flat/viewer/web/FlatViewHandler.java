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
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static java.time.temporal.ChronoUnit.MINUTES;

public class FlatViewHandler {
    private static final Logger log = LoggerFactory.getLogger(FlatViewHandler.class);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final FlatViewService flatViewService;
    private final Set<Entry<LocalTime, LocalTime>> timeSlots;

    public FlatViewHandler(FlatViewService flatViewService, Set<Entry<LocalTime, LocalTime>> timeSlots) {
        this.flatViewService = flatViewService;
        this.timeSlots = timeSlots;
    }

    public void handleRent(RoutingContext context) {
        processAndRespond(context, this::callRent);
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

    private Result callRent(JsonObject body) {
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        return flatViewService.rent(flatId, tenantId);
    }

    public void handleReserve(RoutingContext context) {
        processAndRespond(context, this::callReserve);
    }

    private Result callReserve(JsonObject body) {
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        return flatViewService.tryReserve(flatId, getDateTime(body), tenantId);
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
        processAndRespond(context, this::callCancel);
    }

    private Result callCancel(JsonObject body) {
        JsonObject json;
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        return flatViewService.cancel(flatId, getDateTime(body), tenantId);
    }

    public void handleApprove(RoutingContext context) {
        processAndRespond(context, this::callApprove);
    }

    private Result callApprove(JsonObject body) {
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        return flatViewService.approve(flatId, getDateTime(body), tenantId);
    }

    public void handleReject(RoutingContext context) {
        processAndRespond(context, this::callReject);
    }

    private Result callReject(JsonObject body) {
        Integer tenantId = body.getInteger("t_id");
        Integer flatId = body.getInteger("f_id");
        return flatViewService.reject(flatId, getDateTime(body), tenantId);
    }
}
