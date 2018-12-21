package data.json

data class ClientUser(
    val id: Int,
    val username: String,
    var online: Boolean?
)