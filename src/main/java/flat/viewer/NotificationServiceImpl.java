package flat.viewer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NotificationServiceImpl implements NotificationService {
    private final Map<Integer, Set<ViewSlot>> newTenants = new HashMap<>();
    private final Map<Integer, Set<ViewSlot>> currentTenants = new HashMap<>();
    private final Map<Integer, Set<Integer>> flatIdToNewTenantsSubscribe = new HashMap<>();
    private final Map<FlatViewSlot, Integer> flatViewToNewTenant = new HashMap<>();
    private final Map<Integer, Integer> flatToCurrentTenant = new HashMap<>();

    @Override
    public void subscribeNew(Integer flatId, ViewSlot viewSlot) {
        flatViewToNewTenant.put(new FlatViewSlot(flatId, viewSlot.getStartTime()), viewSlot.getNewTenantId());

//        flatIdToNewTenantsSubscribe.computeIfAbsent(flatId, flatId_ -> new HashSet<>())
//                .add(viewSlot.getNewTenantId());
    }

    @Override
    public void unsubscribeNew(Integer flatId, ViewSlot viewSlot) {
        flatViewToNewTenant.remove(new FlatViewSlot(flatId, viewSlot.getStartTime()));
    }

    @Override
    public void notifyNew(Integer flatId, ViewSlot viewSlot) {
        Integer newTenantId = flatViewToNewTenant.get(new FlatViewSlot(flatId, viewSlot.getStartTime()));
        if (newTenantId != null) {
            notifyTenant(newTenantId, viewSlot.getStartTime(), viewSlot.getState());
            unsubscribeNew(flatId, viewSlot);
        }

//        Integer newTenantId = flatViewToNewTenant.remove(new FlatViewSlot(flatId, viewSlot.getStartTime()));
//        if (newTenantId != null) {
//            notifyTenant(newTenantId, viewSlot.getStartTime(), viewSlot.getState());
//        }

        //        if (flatViewToNewTenant.remove(new FlatViewSlot(flatId, viewSlot.getStartTime()), newTenantId)) {
//            notifyTenant(newTenantId, viewSlot.getStartTime(), viewSlot.getState());
//        }

//        Integer tenantId = flatSlotToNewTenant.get(new FlatViewSlot(flatId, viewSlot.getStartTime()));
//        flatSlotToNewTenant.compute(new FlatViewSlot(flatId, viewSlot.getStartTime()), (flatViewSlot, tenantId) -> {
//            notifyTenant(tenantId, viewSlot.getStartTime(), viewSlot.getState());
//            return null;
//        });

//        newTenants.computeIfAbsent(flatId, flatId_ -> new HashSet<>())
//                .add(viewSlot);

//        flatIdToNewTenantsSubscribe.
//        notify(flatId, viewSlot);
    }

    @Override
    public void subscribeCurrent(Integer flatId, Integer tenantId) {
        flatToCurrentTenant.put(flatId, tenantId);
    }

    @Override
    public void notifyCurrent(Integer flatId, ViewSlot viewSlot) {
        Integer currentTenantId = flatToCurrentTenant.get(flatId);
        if (currentTenantId != null) {
            notifyTenant(currentTenantId, viewSlot.getStartTime(), viewSlot.getState());
        }

//        currentTenants.computeIfAbsent(flatId, flatId_ -> new HashSet<>())
//                .add(viewSlot);
    }

    @Override
    public void notifyTenant(Integer tenantId, LocalDateTime startTime, SlotState state) {
        // stub
    }
}
