package flat.viewer

import io.vertx.core.shareddata.Shareable
import java.time.LocalDateTime
import java.util.*

class ViewSlot(val startTime: LocalDateTime, val newTenantId: Int) : Shareable {
    var state: SlotState? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val viewSlot = other as ViewSlot
        return startTime == viewSlot.startTime
    }

    override fun hashCode(): Int {
        return Objects.hash(startTime)
    }

}
