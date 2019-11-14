package flat.viewer

import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class FlatViewService(private val notificationService: NotificationService) {
    fun rent(flatId: Int, tenantId: Int, flatToCurrentTenant: MutableMap<Int?, Int?>): Result {
        val prev = flatToCurrentTenant.putIfAbsent(flatId, tenantId)
        if (prev == null) {
            notificationService.subscribeCurrent(flatId, tenantId)
            return Result.Ok
        }
        return Result.Occupied
    }

    fun tryReserve(flatId: Int, start: LocalDateTime, tenantId: Int, flatViewToNewTenant: MutableMap<FlatViewSlot?, ViewSlot?>): Result? {
        val result = arrayOfNulls<Result>(1)
        flatViewToNewTenant.compute(FlatViewSlot(flatId, start)) { _: FlatViewSlot?, viewSlot_: ViewSlot? ->
            if (viewSlot_ == null) {
                val viewSlot = ViewSlot(start, tenantId)
                viewSlot.state = SlotState.RESERVING
                notificationService.subscribeNew(flatId, viewSlot)
                notificationService.notifyCurrent(flatId, viewSlot)
                result[0] = Result.Ok
                viewSlot
            } else {
                if (viewSlot_.state == SlotState.REJECTED) {
                    result[0] = Result.Rejected
                } else {
                    result[0] = Result.Occupied
                }
                viewSlot_
            }
        }
        return result[0]
    }

    fun cancel(flatId: Int, start: LocalDateTime, tenantId: Int, flatViewToNewTenant: MutableMap<FlatViewSlot?, ViewSlot?>): Result {
        flatViewToNewTenant.remove(FlatViewSlot(flatId, start))
        val viewSlot = ViewSlot(start, tenantId)
        viewSlot.state = SlotState.CANCELED
        notificationService.unsubscribeNew(flatId, viewSlot)
        notificationService.notifyCurrent(flatId, viewSlot)
        return Result.Ok
    }

    fun approve(flatId: Int, start: LocalDateTime, currentTenantId: Int, flatToCurrentTenant: Map<Int?, Int?>,
                flatViewToNewTenant: MutableMap<FlatViewSlot?, ViewSlot?>): Result? {
        if (LocalDateTime.now().isAfter(start.minusHours(NOTICE_TIME.toLong()))) {
            log.info("Slot time: {}", start)
            return Result.TooLate
        }
        if (flatToCurrentTenant[flatId] != currentTenantId) {
            log.info("Flat tenant: {}, current tenant: {}", flatToCurrentTenant[flatId], currentTenantId)
            return Result.NotCurrent
        }
        val result = arrayOfNulls<Result>(1)
        flatViewToNewTenant.compute(FlatViewSlot(flatId, start)) { _: FlatViewSlot?, viewSlot_: ViewSlot? ->
            if (viewSlot_ == null) {
                return@compute null
            }
            viewSlot_.state = SlotState.APPROVED
            notificationService.notifyNew(flatId, viewSlot_)
            result[0] = Result.Ok
            viewSlot_
        }
        return result[0]
    }

    fun reject(flatId: Int, start: LocalDateTime, currentTenantId: Int?, flatToCurrentTenant: Map<Int?, Int?>,
               flatViewToNewTenant: MutableMap<FlatViewSlot?, ViewSlot?>): Result {
        if (flatToCurrentTenant[flatId] != currentTenantId) {
            log.info("Flat tenant: {}, current tenant: {}", flatToCurrentTenant[flatId], currentTenantId)
            return Result.NotCurrent
        }
        flatViewToNewTenant.compute(FlatViewSlot(flatId, start)) { _: FlatViewSlot?, viewSlot_: ViewSlot? ->
            val viewSlot = viewSlot_ ?: ViewSlot(start, -1)
            viewSlot.state = SlotState.REJECTED
            notificationService.notifyNew(flatId, viewSlot)
            viewSlot
        }
        return Result.Ok
    }

    companion object {
        private val log = LoggerFactory.getLogger(FlatViewService::class.java)
        private const val NOTICE_TIME = 24
    }

}
