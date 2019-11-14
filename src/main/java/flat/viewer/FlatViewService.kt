package flat.viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import static flat.viewer.Result.*;
import static flat.viewer.SlotState.*;

public class FlatViewService {

    private static final Logger log = LoggerFactory.getLogger(FlatViewService.class);

    private static final int NOTICE_TIME = 24;

    private final NotificationService notificationService;

    public FlatViewService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public Result rent(Integer flatId, Integer tenantId, Map<Integer, Integer> flatToCurrentTenant) {
        Integer prev = flatToCurrentTenant.putIfAbsent(flatId, tenantId);
        if (prev == null) {
            notificationService.subscribeCurrent(flatId, tenantId);
            return Ok;
        }
        return Occupied;
    }

    public Result tryReserve(Integer flatId, LocalDateTime start, Integer tenantId, Map<FlatViewSlot, ViewSlot> flatViewToNewTenant) {
        final Result[] result = new Result[1];
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
    }

    public Result cancel(Integer flatId, LocalDateTime start, Integer tenantId, Map<FlatViewSlot, ViewSlot> flatViewToNewTenant) {
        flatViewToNewTenant.remove(new FlatViewSlot(flatId, start));
        ViewSlot viewSlot = new ViewSlot(start, tenantId);
        viewSlot.setState(CANCELED);
        notificationService.unsubscribeNew(flatId, viewSlot);
        notificationService.notifyCurrent(flatId, viewSlot);
        return Ok;
    }

    public Result approve(Integer flatId, LocalDateTime start, Integer currentTenantId, Map<Integer, Integer> flatToCurrentTenant,
                          Map<FlatViewSlot, ViewSlot> flatViewToNewTenant) {
        if (LocalDateTime.now().isAfter(start.minusHours(NOTICE_TIME))) {
            log.info("Slot time: {}", start);
            return TooLate;
        }
        if (!Objects.equals(flatToCurrentTenant.get(flatId), currentTenantId)) {
            log.info("Flat tenant: {}, current tenant: {}", flatToCurrentTenant.get(flatId), currentTenantId);
            return NotCurrent;
        }
        final Result[] result = new Result[1];
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

    public Result reject(Integer flatId, LocalDateTime start, Integer currentTenantId, Map<Integer, Integer> flatToCurrentTenant,
                         Map<FlatViewSlot, ViewSlot> flatViewToNewTenant) {
        if (!Objects.equals(flatToCurrentTenant.get(flatId), currentTenantId)) {
            log.info("Flat tenant: {}, current tenant: {}", flatToCurrentTenant.get(flatId), currentTenantId);
            return NotCurrent;
        }
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
