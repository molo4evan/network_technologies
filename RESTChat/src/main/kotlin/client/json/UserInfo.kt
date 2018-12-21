package client.json

data class UserInfo(
        val id: Int,
        val username: String,
        var online: Boolean?,
        val token: Int = username.hashCode() + 127
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