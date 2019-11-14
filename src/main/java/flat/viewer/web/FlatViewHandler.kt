package flat.viewer.web

import flat.viewer.FlatViewService
import flat.viewer.FlatViewSlot
import flat.viewer.Result
import flat.viewer.ViewSlot
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.AsyncResult
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.Lock
import io.vertx.core.shareddata.SharedData
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Function

class FlatViewHandler(private val flatViewService: FlatViewService, private val timeSlots: Set<Map.Entry<LocalTime, LocalTime>>, private val sharedData: SharedData) {
    fun handleRent(context: RoutingContext) {
        processAndRespond(Function { data: CallData -> callRent(data) }, context)
    }

    private fun processAndRespond(function: Function<CallData, Result?>, context: RoutingContext) {
        val result = arrayOf<Result?>(Result.Error)
        sharedData.getLockWithTimeout(FLAT_VIEW_TO_NEW_TENANT_LOCK, LOCK_TIMEOUT.toLong()) { lockRes: AsyncResult<Lock> ->
            try {
                try {
                    if (lockRes.succeeded()) {
                        result[0] = function.apply(CallData(context.bodyAsJson, sharedData))
                    } else {
                        log.warn("Failed to get lock")
                    }
                } finally {
                    lockRes.result().release()
                }
                val json = JsonObject().put("result", result[0])
                context.response().setChunked(true).putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON).end(json.encode())
            } catch (e: Exception) {
                log.error("Request error", e)
                val json = JsonObject().put("error", e.toString() + if (e.cause != null) ", cause:" + e.cause else "")
                context.response().setStatusCode(422).end(json.encode())
            }
        }
    }

    private fun callRent(data: CallData): Result {
        val tenantId = data.jsonObject.getInteger("t_id")
        val flatId = data.jsonObject.getInteger("f_id")
        val flatToCurrentTenant = data.sharedData.getLocalMap<Int?, Int?>(FLAT_TO_CURRENT_TENANT_MAP)
        return flatViewService.rent(flatId, tenantId, flatToCurrentTenant)
    }

    fun handleReserve(context: RoutingContext) {
        processAndRespond(Function { data: CallData -> callReserve(data) }, context)
    }

    private fun callReserve(data: CallData): Result? {
        val tenantId = data.jsonObject.getInteger("t_id")
        val flatId = data.jsonObject.getInteger("f_id")
        val flatViewToNewTenant = sharedData.getLocalMap<FlatViewSlot?, ViewSlot?>(FLAT_VIEW_TO_NEW_TENANT_MAP)
        return flatViewService.tryReserve(flatId, getDateTime(data.jsonObject), tenantId, flatViewToNewTenant)
    }

    private fun getDateTime(body: JsonObject): LocalDateTime {
        val date = LocalDate.parse(body.getString("date"), DATE_FORMATTER)
        val start = LocalTime.parse(body.getString("time"), TIME_FORMATTER)
        val end = start.plus(body.getInteger("duration").toLong(), ChronoUnit.MINUTES)
        validate(start, end)
        return LocalDateTime.of(date, start)
    }

    private fun validate(start: LocalTime, end: LocalTime) {
        require(timeSlots.contains(AbstractMap.SimpleEntry(start, end))) { "Wrong slot" }
    }

    fun handleCancel(context: RoutingContext) {
        processAndRespond(Function { data: CallData -> callCancel(data) }, context)
    }

    private fun callCancel(data: CallData): Result {
        val tenantId = data.jsonObject.getInteger("t_id")
        val flatId = data.jsonObject.getInteger("f_id")
        val flatViewToNewTenant = sharedData.getLocalMap<FlatViewSlot?, ViewSlot?>(FLAT_VIEW_TO_NEW_TENANT_MAP)
        return flatViewService.cancel(flatId, getDateTime(data.jsonObject), tenantId, flatViewToNewTenant)
    }

    fun handleApprove(context: RoutingContext) {
        processAndRespond(Function { data: CallData -> callApprove(data) }, context)
    }

    private fun callApprove(data: CallData): Result? {
        val tenantId = data.jsonObject.getInteger("t_id")
        val flatId = data.jsonObject.getInteger("f_id")
        val flatToCurrentTenant = data.sharedData.getLocalMap<Int?, Int?>(FLAT_TO_CURRENT_TENANT_MAP)
        val flatViewToNewTenant = sharedData.getLocalMap<FlatViewSlot?, ViewSlot?>(FLAT_VIEW_TO_NEW_TENANT_MAP)
        return flatViewService.approve(flatId, getDateTime(data.jsonObject), tenantId, flatToCurrentTenant, flatViewToNewTenant)
    }

    fun handleReject(context: RoutingContext) {
        processAndRespond(Function { data: CallData -> callReject(data) }, context)
    }

    private fun callReject(data: CallData): Result {
        val tenantId = data.jsonObject.getInteger("t_id")
        val flatId = data.jsonObject.getInteger("f_id")
        val flatToCurrentTenant = data.sharedData.getLocalMap<Int?, Int?>(FLAT_TO_CURRENT_TENANT_MAP)
        val flatViewToNewTenant = sharedData.getLocalMap<FlatViewSlot?, ViewSlot?>(FLAT_VIEW_TO_NEW_TENANT_MAP)
        return flatViewService.reject(flatId, getDateTime(data.jsonObject), tenantId, flatToCurrentTenant, flatViewToNewTenant)
    }

    private class CallData internal constructor(val jsonObject: JsonObject, val sharedData: SharedData)

    companion object {
        private val log = LoggerFactory.getLogger(FlatViewHandler::class.java)
        @JvmField
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        private const val FLAT_VIEW_TO_NEW_TENANT_LOCK = "flatViewToNewTenantLock"
        private const val FLAT_VIEW_TO_NEW_TENANT_MAP = "flatViewToNewTenantMap"
        private const val FLAT_TO_CURRENT_TENANT_MAP = "flatToCurrentTenantMap"
        private const val LOCK_TIMEOUT = 100
    }

}
