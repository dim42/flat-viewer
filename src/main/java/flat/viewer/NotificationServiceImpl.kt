package flat.viewer

import java.time.LocalDateTime
import java.util.*

class NotificationServiceImpl : NotificationService {
    private val flatViewToNewTenant: MutableMap<FlatViewSlot, Int> = HashMap()
    private val flatToCurrentTenant: MutableMap<Int, Int> = HashMap()
    override fun subscribeCurrent(flatId: Int, tenantId: Int) {
        flatToCurrentTenant[flatId] = tenantId
    }

    override fun notifyCurrent(flatId: Int, viewSlot: ViewSlot) {
        val currentTenantId = flatToCurrentTenant[flatId]
        if (currentTenantId != null) {
            notifyTenant(currentTenantId, viewSlot.startTime, viewSlot.state!!)
        }
    }

    override fun subscribeNew(flatId: Int, viewSlot: ViewSlot) {
        flatViewToNewTenant[FlatViewSlot(flatId, viewSlot.startTime)] = viewSlot.newTenantId
    }

    override fun unsubscribeNew(flatId: Int, viewSlot: ViewSlot) {
        flatViewToNewTenant.remove(FlatViewSlot(flatId, viewSlot.startTime))
    }

    override fun notifyNew(flatId: Int, viewSlot: ViewSlot) {
        val newTenantId = flatViewToNewTenant[FlatViewSlot(flatId, viewSlot.startTime)]
        if (newTenantId != null) {
            notifyTenant(newTenantId, viewSlot.startTime, viewSlot.state!!)
            unsubscribeNew(flatId, viewSlot)
        }
    }

    override fun notifyTenant(tenantId: Int, startTime: LocalDateTime, state: SlotState) {
        // stub
    }
}
