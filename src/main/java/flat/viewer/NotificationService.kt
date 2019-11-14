package flat.viewer;

import java.time.LocalDateTime;

public interface NotificationService {

    void subscribeCurrent(Integer flatId, Integer tenantId);

    void notifyCurrent(Integer flatId, ViewSlot viewSlot);

    void subscribeNew(Integer flatId, ViewSlot viewSlot);

    void unsubscribeNew(Integer flatId, ViewSlot viewSlot);

    void notifyNew(Integer flatId, ViewSlot viewSlot);

    void notifyTenant(Integer tenantId, LocalDateTime startTime, SlotState state);
}
