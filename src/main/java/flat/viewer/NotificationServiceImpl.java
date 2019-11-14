package flat.viewer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class NotificationServiceImpl implements NotificationService {

    private final Map<FlatViewSlot, Integer> flatViewToNewTenant = new HashMap<>();
    private final Map<Integer, Integer> flatToCurrentTenant = new HashMap<>();

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
    }

    @Override
    public void subscribeNew(Integer flatId, ViewSlot viewSlot) {
        flatViewToNewTenant.put(new FlatViewSlot(flatId, viewSlot.getStartTime()), viewSlot.getNewTenantId());
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
    }

    @Override
    public void notifyTenant(Integer tenantId, LocalDateTime startTime, SlotState state) {
        // stub
    }
}
