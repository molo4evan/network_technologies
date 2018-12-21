package server.data.json

import client.json.UserInfo

data class UserInfo(
        val id: Int,
        val username: String,
        var online: Boolean?,
        val token: Int = id.hashCode() * username.hashCode()
) {
    override fun equals(other: Any?): Boolean {
        if (other !is UserInfo) return false
        return id == other.id && username == other.username && token == other.token
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + username.hashCode()
        result = 31 * result + token
        return result
    }
}