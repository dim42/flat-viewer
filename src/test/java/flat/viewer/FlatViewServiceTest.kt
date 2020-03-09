package flat.viewer

import flat.viewer.Result.*
import flat.viewer.SlotState.*
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.Spy
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDateTime
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class FlatViewServiceTest {
    @Spy
    private val notificationService: NotificationService = NotificationServiceImpl()
    private lateinit var flatViewService: FlatViewService
    private lateinit var flatToCurrentTenant: MutableMap<Int?, Int?>
    private lateinit var flatViewToNewTenant: MutableMap<FlatViewSlot?, ViewSlot?>

    @Before
    fun setUp() {
        flatViewService = FlatViewService(notificationService)
        flatToCurrentTenant = HashMap()
        flatViewToNewTenant = HashMap()
    }

    @Test
    fun testTryReserve_notRented() {
        val tenant1Id = 11
        val tenant2Id = 12
        val flatId = 45
        val start = LocalDateTime.now().plusDays(5)

        var result = flatViewService.tryReserve(flatId, start, tenant2Id, flatViewToNewTenant)

        assertThat(result, equalTo(Ok))
        verify(notificationService, never()).notifyTenant(ArgumentMatchers.any(Int::class.java), any(), any())

        flatViewService.cancel(flatId, start, tenant2Id, flatViewToNewTenant)
        flatViewService.rent(flatId, tenant1Id, flatToCurrentTenant)
        flatViewService.tryReserve(flatId, start, tenant2Id, flatViewToNewTenant)

        verify(notificationService).notifyTenant(tenant1Id, start, RESERVING)

        result = flatViewService.approve(flatId, start, tenant2Id, flatToCurrentTenant, flatViewToNewTenant)

        assertThat(result, equalTo(NotCurrent))
        verify(notificationService).notifyTenant(ArgumentMatchers.any(Int::class.java), any(), any())
    }

    private fun <T> any(): T {
        return Mockito.any()
    }

    @Test
    fun testTryReserve_approve() {
        val tenant1Id = 11
        val tenant2Id = 12
        val tenant3Id = 13
        val flatId = 45
        val start = LocalDateTime.now().plusDays(5)

        var result: Result?
        result = flatViewService.rent(flatId, tenant1Id, flatToCurrentTenant)
        assertThat(result, equalTo(Ok))
        result = flatViewService.tryReserve(flatId, start, tenant2Id, flatViewToNewTenant)

        assertThat(result, equalTo(Ok))
        verify(notificationService).notifyTenant(tenant1Id, start, RESERVING)

        result = flatViewService.tryReserve(flatId, start, tenant3Id, flatViewToNewTenant)
        assertThat(result, equalTo(Occupied))

        result = flatViewService.approve(flatId, start, tenant1Id, flatToCurrentTenant, flatViewToNewTenant)

        assertThat(result, equalTo(Ok))
        verify(notificationService).notifyTenant(tenant2Id, start, APPROVED)
    }

    @Test
    fun testTryReserve_cancel() {
        val tenant1Id = 11
        val tenant2Id = 12
        val tenant3Id = 13
        val flatId = 45
        val start = LocalDateTime.now().plusDays(5)

        var result: Result
        result = flatViewService.rent(flatId, tenant1Id, flatToCurrentTenant)
        assertThat(result, equalTo(Ok))
        flatViewService.tryReserve(flatId, start, tenant2Id, flatViewToNewTenant)

        result = flatViewService.cancel(flatId, start, tenant2Id, flatViewToNewTenant)

        assertThat(result, equalTo(Ok))
        verify(notificationService).notifyTenant(tenant1Id, start, CANCELED)

        result = flatViewService.tryReserve(flatId, start, tenant3Id, flatViewToNewTenant)!!
        assertThat(result, equalTo(Ok))
    }

    @Test
    fun testTryReserve_reject() {
        val tenant1Id = 11
        val tenant2Id = 12
        val tenant3Id = 13
        val flatId = 45
        val start = LocalDateTime.now().plusDays(5)
        var result: Result
        flatViewService.rent(flatId, tenant1Id, flatToCurrentTenant)
        flatViewService.tryReserve(flatId, start, tenant2Id, flatViewToNewTenant)
        verify(notificationService).notifyTenant(tenant1Id, start, RESERVING)
        result = flatViewService.reject(flatId, start, tenant1Id, flatToCurrentTenant, flatViewToNewTenant)

        assertThat(result, equalTo(Ok))
        verify(notificationService).notifyTenant(tenant2Id, start, REJECTED)

        result = flatViewService.tryReserve(flatId, start, tenant3Id, flatViewToNewTenant)!!

        assertThat(result, equalTo(Rejected))
        verify(notificationService, times(2)).notifyTenant(ArgumentMatchers.any(Int::class.java), any(), any())
    }
}
