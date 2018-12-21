package data.json

import java.util.*

data class UserInfo(
    val id: Int,
    val username: String,
    var online: Boolean?,
    var token: String = UUID.randomUUID().toString(),
    var address: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (other !is UserInfo) return false
        return id == other.id && username == other.username && token == other.token
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + username.hashCode()
        return result
    }
}