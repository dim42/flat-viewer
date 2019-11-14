package flat.viewer;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static flat.viewer.Result.*;
import static flat.viewer.SlotState.*;

public class FlatViewService {

    private static final Logger log = LoggerFactory.getLogger(FlatViewService.class);

    private static final int NOTICE_TIME = 24;

    private final NotificationService notificationService;
    private final SharedData sharedData;
    //    Map<Integer, Map<LocalDateTime, ViewSlot>> flatToViewSlots = new HashMap<>();// shared
    private final Map<FlatViewSlot, ViewSlot> flatViewToNewTenant = new HashMap<>();// shared
    private final Map<Integer, Integer> flatToCurrentTenant = new HashMap<>();

    public FlatViewService(NotificationService notificationService, SharedData sharedData) {
        this.notificationService = notificationService;
        this.sharedData = sharedData;
    }

    public Result rent(Integer flatId, Integer tenantId, Map<Integer, Integer> flatToCurrentTenant) {
        if (flatToCurrentTenant == null) {
            flatToCurrentTenant = this.flatToCurrentTenant;
        }
        Integer prev = flatToCurrentTenant.putIfAbsent(flatId, tenantId);
        if (prev == null) {
            notificationService.subscribeCurrent(flatId, tenantId);
            return Ok;
        }
        return Occupied;
    }

    public Result tryReserve(Integer flatId, LocalDateTime start, Integer tenantId, Map<FlatViewSlot, ViewSlot> flatViewToNewTenant) {
        if (flatViewToNewTenant == null) {
            flatViewToNewTenant = this.flatViewToNewTenant;
        }
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

        log.info("Service Reserve Res!:" + tenantId);
        return result[0];
    }
//        final Result[] result = new Result[1];
//        log.info("Start!:");
//        CompletableFuture<Result> future = new CompletableFuture<>();
//
//        sharedData.getLockWithTimeout("flatViewToNewTenantLock", 10000, lockRes -> {
//            log.info("Lock!:");
//            if (lockRes.succeeded()) {
//                LocalMap<FlatViewSlot, ViewSlot> mymap = sharedData.getLocalMap("flatViewToNewTenantMap");
//                log.info("Map!:");
//                
//                mymap.compute(new FlatViewSlot(flatId, start), (start_, viewSlot_) -> {
//                    if (viewSlot_ == null) {
//                        viewSlot_ = new ViewSlot(start, tenantId);
//                        viewSlot_.setState(RESERVING);
//                        notificationService.subscribeNew(flatId, viewSlot_);
//                        notificationService.notifyCurrent(flatId, viewSlot_);
//                        result[0] = Ok;
//                    } else {
//                        if (viewSlot_.getState() == REJECTED) {
//                            result[0] = Rejected;
//                        } else {
//                            result[0] = Occupied;
//                        }
//                    }
//                    log.info("Inside!:");
//                    return viewSlot_;
//                });
//                future.complete(result[0]);
//
//            } else {
//                // Failed to get lock
////                future.fail(lockRes.cause());
//                // log
//                System.out.println("lockRes!:" + lockRes);
//
//            }
////                future.completeExceptionally("error");
//        });
//        
//
//        Result result1 = null;
//        try {
//            result1 = future.get(2000, TimeUnit.MILLISECONDS);
//        } catch (Exception e) {
//            log.info("Err!:",e);
//        }
//        log.info("Res!:");
//        return result1;


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

//    }

    public Result cancel(Integer flatId, LocalDateTime start, Integer tenantId, Map<FlatViewSlot, ViewSlot> flatViewToNewTenant) {
        if (flatViewToNewTenant == null) {
            flatViewToNewTenant = this.flatViewToNewTenant;
        }
//        Map<LocalDateTime, ViewSlot> viewSlots = flatToViewSlots.computeIfAbsent(flatId, flatId_ -> new HashMap<>());
//        viewSlots.remove(start);

        flatViewToNewTenant.remove(new FlatViewSlot(flatId, start));

//        sharedData.getLockWithTimeout("flatViewToNewTenantLock", 10000, lockRes -> {
//            if (lockRes.succeeded()) {
//                LocalMap<FlatViewSlot, ViewSlot> mymap = sharedData.getLocalMap("flatViewToNewTenantMap");
//
//                mymap.remove(new FlatViewSlot(flatId, start));
//
//            } else {
//                // Failed to get lock
////                future.fail(lockRes.cause());
//                // log
//            }
//        });

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
        if (flatToCurrentTenant == null) {
            flatToCurrentTenant = this.flatToCurrentTenant;
        }
        if (!Objects.equals(flatToCurrentTenant.get(flatId), currentTenantId)) {
            log.info("Flat tenant: {}, current tenant: {}", flatToCurrentTenant.get(flatId), currentTenantId);
            return NotCurrent;
        }
//        Map<LocalDateTime, ViewSlot> viewSlots = flatToViewSlots.computeIfAbsent(flatId, flatId_ -> new HashMap<>());
        final Result[] result = new Result[1];
//        viewSlots.compute(start, (start_, viewSlot_) -> {

        if (flatViewToNewTenant == null) {
            flatViewToNewTenant = this.flatViewToNewTenant;
        }
        flatViewToNewTenant.compute(new FlatViewSlot(flatId, start), (start_, viewSlot_) -> {
            if (viewSlot_ == null) {
                return null;
            }
            viewSlot_.setState(APPROVED);
            notificationService.notifyNew(flatId, viewSlot_);
            result[0] = Ok;
            return viewSlot_;
        });

//        sharedData.getLockWithTimeout("flatViewToNewTenantLock", 10000, lockRes -> {
//            if (lockRes.succeeded()) {
//                LocalMap<FlatViewSlot, ViewSlot> mymap = sharedData.getLocalMap("flatViewToNewTenantMap");
//
//                mymap.compute(new FlatViewSlot(flatId, start), (start_, viewSlot_) -> {
//                    if (viewSlot_ == null) {
//                        return null;
//                    }
//                    viewSlot_.setState(APPROVED);
//                    notificationService.notifyNew(flatId, viewSlot_);
//                    result[0] = Ok;
//                    return viewSlot_;
//                });
//
//            } else {
//                // Failed to get lock
////                future.fail(lockRes.cause());
//                // log
//            }
//        });
        return result[0];
    }

    public Result reject(Integer flatId, LocalDateTime start, Integer currentTenantId, Map<Integer, Integer> flatToCurrentTenant,
                         Map<FlatViewSlot, ViewSlot> flatViewToNewTenant) {
        if (flatToCurrentTenant == null) {
            flatToCurrentTenant = this.flatToCurrentTenant;
        }
        if (!Objects.equals(flatToCurrentTenant.get(flatId), currentTenantId)) {
            return NotCurrent;
        }
//        Map<LocalDateTime, ViewSlot> viewSlots = flatToViewSlots.computeIfAbsent(flatId, flatId_ -> new HashMap<>());
//        viewSlots.compute(start, (start_, viewSlot_) -> {
        if (flatViewToNewTenant == null) {
            flatViewToNewTenant = this.flatViewToNewTenant;
        }
        flatViewToNewTenant.compute(new FlatViewSlot(flatId, start), (start_, viewSlot_) -> {
            if (viewSlot_ == null) {
                viewSlot_ = new ViewSlot(start, null);
            }
            viewSlot_.setState(REJECTED);
            notificationService.notifyNew(flatId, viewSlot_);
            return viewSlot_;
        });

//        sharedData.getLockWithTimeout("flatViewToNewTenantLock", 10000, lockRes -> {
//            if (lockRes.succeeded()) {
//                LocalMap<FlatViewSlot, ViewSlot> mymap = sharedData.getLocalMap("flatViewToNewTenantMap");
//
//                mymap.compute(new FlatViewSlot(flatId, start), (start_, viewSlot_) -> {
//                    if (viewSlot_ == null) {
//                        viewSlot_ = new ViewSlot(start, null);
//                    }
//                    viewSlot_.setState(REJECTED);
//                    notificationService.notifyNew(flatId, viewSlot_);
//                    return viewSlot_;
//                });
//
//            } else {
//                // Failed to get lock
////                future.fail(lockRes.cause());
//                // log
//            }
//        });

        return Ok;
    }
}
