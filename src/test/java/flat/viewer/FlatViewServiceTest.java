package flat.viewer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static flat.viewer.Result.*;
import static flat.viewer.SlotState.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FlatViewServiceTest {

    @Spy
    private final NotificationService notificationService = new NotificationServiceImpl();
    private FlatViewService flatViewService;

    @Before
    public void setUp() {
        flatViewService = new FlatViewService(notificationService);
    }

    @Test
    public void testTryReserve_notRented() {
        Integer tenant1Id = 11;
        Integer tenant2Id = 12;
        Integer flatId = 45;
        LocalDateTime start = LocalDateTime.now().plusDays(5);

        Result result = flatViewService.tryReserve(flatId, start, tenant2Id);

        assertThat(result, equalTo(Ok));
        verify(notificationService, never()).notifyTenant(any(), any(), any());

        flatViewService.cancel(flatId, start, tenant2Id);
        flatViewService.rent(flatId, tenant1Id);
        flatViewService.tryReserve(flatId, start, tenant2Id);

        verify(notificationService).notifyTenant(tenant1Id, start, RESERVING);

        result = flatViewService.approve(flatId, start, tenant2Id);

        assertThat(result, equalTo(NotCurrent));
        verify(notificationService).notifyTenant(any(), any(), any());
    }

    @Test
    public void testTryReserve_approve() {
        Integer tenant1Id = 11;
        Integer tenant2Id = 12;
        Integer tenant3Id = 13;
        Integer flatId = 45;
        LocalDateTime start = LocalDateTime.now().plusDays(5);

//        Result result = flatViewService.tryReserve(tenant2Id, flatId, start);

//        assertThat(result, equalTo(Ok));
//        verify(notificationService, never()).notifyTenant(any(), any(), any());
//        verify(notificationService).add("one");

//        flatViewService.cancel(tenant2Id, flatId, start);
        Result result;
        result = flatViewService.rent(flatId, tenant1Id);
        assertThat(result, equalTo(Ok));
        result = flatViewService.tryReserve(flatId, start, tenant2Id);

        assertThat(result, equalTo(Ok));
        verify(notificationService).notifyTenant(tenant1Id, start, RESERVING);

        result = flatViewService.tryReserve(flatId, start, tenant3Id);
        assertThat(result, equalTo(Occupied));

        result = flatViewService.approve(flatId, start, tenant1Id);

        assertThat(result, equalTo(Ok));
        verify(notificationService).notifyTenant(tenant2Id, start, APPROVED);
    }

    @Test
    public void testTryReserve_cancel() {
        Integer tenant1Id = 11;
        Integer tenant2Id = 12;
        Integer tenant3Id = 13;
        Integer flatId = 45;
        LocalDateTime start = LocalDateTime.now().plusDays(5);

        Result result;
        result = flatViewService.rent(flatId, tenant1Id);
        assertThat(result, equalTo(Ok));
        flatViewService.tryReserve(flatId, start, tenant2Id);

        result = flatViewService.cancel(flatId, start, tenant2Id);

        assertThat(result, equalTo(Ok));
        verify(notificationService).notifyTenant(tenant1Id, start, CANCELED);

        result = flatViewService.tryReserve(flatId, start, tenant3Id);
        assertThat(result, equalTo(Ok));
    }

    @Test
    public void testTryReserve_reject() {
        Integer tenant1Id = 11;
        Integer tenant2Id = 12;
        Integer tenant3Id = 13;
        Integer flatId = 45;
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        Result result;
        flatViewService.rent(flatId, tenant1Id);
        flatViewService.tryReserve(flatId, start, tenant2Id);
        verify(notificationService).notifyTenant(tenant1Id, start, RESERVING);
        result = flatViewService.reject(flatId, start, tenant1Id);

        assertThat(result, equalTo(Ok));
        verify(notificationService).notifyTenant(tenant2Id, start, REJECTED);

        result = flatViewService.tryReserve(flatId, start, tenant3Id);

        assertThat(result, equalTo(Rejected));
        verify(notificationService, times(2)).notifyTenant(any(), any(), any());
    }
}
