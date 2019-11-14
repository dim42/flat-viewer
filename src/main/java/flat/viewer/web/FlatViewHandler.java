package flat.viewer.web;

import flat.viewer.FlatViewService;
import flat.viewer.FlatViewSlot;
import flat.viewer.Result;
import flat.viewer.ViewSlot;
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
        processAndRespond(this::callRent, context);
    }

    private void processAndRespond(Function<CallData, Result> function, RoutingContext context) {
        final Result[] result = new Result[]{Result.Error};
        sharedData.getLockWithTimeout(FLAT_VIEW_TO_NEW_TENANT_LOCK, LOCK_TIMEOUT, lockRes -> {
            try {
                try {
                    if (lockRes.succeeded()) {
                        result[0] = function.apply(new CallData(context.getBodyAsJson(), sharedData));
                    } else {
                        log.warn("Failed to get lock");
                    }
                } finally {
                    lockRes.result().release();
                }
                JsonObject json = new JsonObject().put("result", result[0]);
                context.response().setChunked(true).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(json.encode());
            } catch (Exception e) {
                log.error("Request error", e);
                JsonObject json = new JsonObject().put("error", e + (e.getCause() != null ? ", cause:" + e.getCause() : ""));
                context.response().setStatusCode(422).end(json.encode());
            }
        });
    }

    private Result callRent(CallData data) {
        Integer tenantId = data.jsonObject.getInteger("t_id");
        Integer flatId = data.jsonObject.getInteger("f_id");
        LocalMap<Integer, Integer> flatToCurrentTenant = data.sharedData.getLocalMap(FLAT_TO_CURRENT_TENANT_MAP);
        return flatViewService.rent(flatId, tenantId, flatToCurrentTenant);
    }

    public void handleReserve(RoutingContext context) {
        processAndRespond(this::callReserve, context);
    }

    private Result callReserve(CallData data) {
        Integer tenantId = data.jsonObject.getInteger("t_id");
        Integer flatId = data.jsonObject.getInteger("f_id");
        LocalMap<FlatViewSlot, ViewSlot> flatViewToNewTenant = sharedData.getLocalMap(FLAT_VIEW_TO_NEW_TENANT_MAP);
        return flatViewService.tryReserve(flatId, getDateTime(data.jsonObject), tenantId, flatViewToNewTenant);
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
        processAndRespond(this::callCancel, context);
    }

    private Result callCancel(CallData data) {
        Integer tenantId = data.jsonObject.getInteger("t_id");
        Integer flatId = data.jsonObject.getInteger("f_id");
        LocalMap<FlatViewSlot, ViewSlot> flatViewToNewTenant = sharedData.getLocalMap(FLAT_VIEW_TO_NEW_TENANT_MAP);
        return flatViewService.cancel(flatId, getDateTime(data.jsonObject), tenantId, flatViewToNewTenant);
    }

    public void handleApprove(RoutingContext context) {
        processAndRespond(this::callApprove, context);
    }

    private Result callApprove(CallData data) {
        Integer tenantId = data.jsonObject.getInteger("t_id");
        Integer flatId = data.jsonObject.getInteger("f_id");
        LocalMap<Integer, Integer> flatToCurrentTenant = data.sharedData.getLocalMap(FLAT_TO_CURRENT_TENANT_MAP);
        LocalMap<FlatViewSlot, ViewSlot> flatViewToNewTenant = sharedData.getLocalMap(FLAT_VIEW_TO_NEW_TENANT_MAP);
        return flatViewService.approve(flatId, getDateTime(data.jsonObject), tenantId, flatToCurrentTenant, flatViewToNewTenant);
    }

    public void handleReject(RoutingContext context) {
        processAndRespond(this::callReject, context);
    }

    private Result callReject(CallData data) {
        Integer tenantId = data.jsonObject.getInteger("t_id");
        Integer flatId = data.jsonObject.getInteger("f_id");
        LocalMap<Integer, Integer> flatToCurrentTenant = data.sharedData.getLocalMap(FLAT_TO_CURRENT_TENANT_MAP);
        LocalMap<FlatViewSlot, ViewSlot> flatViewToNewTenant = sharedData.getLocalMap(FLAT_VIEW_TO_NEW_TENANT_MAP);
        return flatViewService.reject(flatId, getDateTime(data.jsonObject), tenantId, flatToCurrentTenant, flatViewToNewTenant);
    }

    private static class CallData {
        final JsonObject jsonObject;
        final SharedData sharedData;

        CallData(JsonObject jsonObject, SharedData sharedData) {
            this.jsonObject = jsonObject;
            this.sharedData = sharedData;
        }
    }
}
