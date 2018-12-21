package data.json

data class LoginData(
    val myID: Int,
    val myToken: String,
    val users: List<ClientUser>,
    val messages: List<Message>
)