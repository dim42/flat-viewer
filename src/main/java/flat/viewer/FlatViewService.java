package flat.viewer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static flat.viewer.Result.*;
import static flat.viewer.SlotState.*;

public class FlatViewService {

    private static final int NOTICE_TIME = 24;

    private final NotificationService notificationService;
    //    Map<Integer, Map<LocalDateTime, ViewSlot>> flatToViewSlots = new HashMap<>();// shared
    private final Map<FlatViewSlot, ViewSlot> flatViewToNewTenant = new HashMap<>();// shared
    private final Map<Integer, Integer> flatToCurrentTenant = new HashMap<>();

    public FlatViewService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public Result rent(Integer flatId, Integer tenantId) {
        Integer prev = flatToCurrentTenant.putIfAbsent(flatId, tenantId);
        if (prev == null) {
            notificationService.subscribeCurrent(flatId, tenantId);
            return Ok;
        }
        return Occupied;
    }

    public Result tryReserve(Integer flatId, LocalDateTime start, Integer tenantId) {
//        Map<LocalDateTime, ViewSlot> viewSlots = flatToViewSlots.computeIfAbsent(flatId, flatId_ -> new HashMap<>());
        final Result[] result = new Result[1];
//        viewSlots.compute(start, (start_, viewSlot_) -> {
        flatViewToNewTenant.compute(new FlatViewSlot(flatId, start), (start_, viewSlot_) -> {
            if (viewSlot_ == null) {
                viewSlot_ = new ViewSlot(start, tenantId);
                viewSlot_.setState(RESERVING);
                notificationService.subscribeNew(flatId, viewSlot_);
                notificationService.notifyCurrent(flatId, viewSlot_);
                result[0] = Ok;
            } else {
                if (viewSlot_.getState() == REJECTED) {
                    result[0] = Rejected;
                } else {
                    result[0] = Occupied;
                }
            }
            return viewSlot_;
        });
        return result[0];

//        SharedData sharedData = vertx.sharedData();
//
//        sharedData.getLockWithTimeout("mylock", 10000, res -> {
//            if (res.succeeded()) {
//                // Got the lock!
//                Lock lock = res.result();
//
//            } else {
//                // Failed to get lock
//            }
//        });

    }

    public Result cancel(Integer flatId, LocalDateTime start, Integer tenantId) {
//        Map<LocalDateTime, ViewSlot> viewSlots = flatToViewSlots.computeIfAbsent(flatId, flatId_ -> new HashMap<>());
//        viewSlots.remove(start);
        flatViewToNewTenant.remove(new FlatViewSlot(flatId, start));
        ViewSlot viewSlot = new ViewSlot(start, tenantId);
        viewSlot.setState(CANCELED);
        notificationService.unsubscribeNew(flatId, viewSlot);
        notificationService.notifyCurrent(flatId, viewSlot);
        return Ok;
    }

    public Result approve(Integer flatId, LocalDateTime start, Integer currentTenantId) {
        if (LocalDateTime.now().isAfter(start.minusHours(NOTICE_TIME))) {
            return TooLate;
        }
        if (!Objects.equals(flatToCurrentTenant.get(flatId), currentTenantId)) {
            return NotCurrent;
        }
        // flatSLot to ?
//        Map<LocalDateTime, ViewSlot> viewSlots = flatToViewSlots.computeIfAbsent(flatId, flatId_ -> new HashMap<>());
        final Result[] result = new Result[1];
//        viewSlots.compute(start, (start_, viewSlot_) -> {
        flatViewToNewTenant.compute(new FlatViewSlot(flatId, start), (start_, viewSlot_) -> {
            if (viewSlot_ == null) {
                return null;
            }
            viewSlot_.setState(APPROVED);
            notificationService.notifyNew(flatId, viewSlot_);
            result[0] = Ok;
            return viewSlot_;
        });
        return result[0];
    }

    public Result reject(Integer flatId, LocalDateTime start, Integer currentTenantId) {
        if (!Objects.equals(flatToCurrentTenant.get(flatId), currentTenantId)) {
            return NotCurrent;
        }
//        Map<LocalDateTime, ViewSlot> viewSlots = flatToViewSlots.computeIfAbsent(flatId, flatId_ -> new HashMap<>());
//        viewSlots.compute(start, (start_, viewSlot_) -> {
        flatViewToNewTenant.compute(new FlatViewSlot(flatId, start), (start_, viewSlot_) -> {
            if (viewSlot_ == null) {
                viewSlot_ = new ViewSlot(start, null);
            }
            viewSlot_.setState(REJECTED);
            notificationService.notifyNew(flatId, viewSlot_);
            return viewSlot_;
        });
        return Ok;
    }
}
