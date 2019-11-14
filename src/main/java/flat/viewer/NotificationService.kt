package flat.viewer

import java.time.LocalDateTime

interface NotificationService {
    fun subscribeCurrent(flatId: Int, tenantId: Int)
    fun notifyCurrent(flatId: Int, viewSlot: ViewSlot)
    fun subscribeNew(flatId: Int, viewSlot: ViewSlot)
    fun unsubscribeNew(flatId: Int, viewSlot: ViewSlot)
    fun notifyNew(flatId: Int, viewSlot: ViewSlot)
    fun notifyTenant(tenantId: Int, startTime: LocalDateTime, state: SlotState)
}
