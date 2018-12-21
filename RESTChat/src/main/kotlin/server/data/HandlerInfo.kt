package server.data

import io.undertow.util.HttpString

data class HandlerInfo(val action: String, val method: HttpString) {
    override fun equals(other: Any?): Boolean {
        return if (other is HandlerInfo){
            action == other.action && method == other.method
        } else false
    }

    override fun hashCode(): Int {
        var result = action.hashCode()
        result = 31 * result + method.hashCode()
        return result
    }
}