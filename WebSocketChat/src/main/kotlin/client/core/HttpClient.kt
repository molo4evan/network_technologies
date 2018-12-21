package client.core

import data.json.UserInfo
import client.exceptions.RequestException
import com.google.gson.Gson
import data.json.*
import okhttp3.*

class HttpClient(hostname: String, port: String, private val master: Client) {
    companion object {
        private val JSON = MediaType.parse("application/json")
    }

    enum class ErrorType(val code: Int) {
        SUCCESS(200),
        NOT_SUPPORTED(400),
        NO_TOKEN(401),
        USERNAME_IS_BUSY(666),
        WRONG_TOKEN(403),
        NOT_FOUND(404),
        WRONG_METHOD(405),
        OTHER(500)
    }

    private val URL_HEAD = "http://$hostname:$port"
    private val client = OkHttpClient()
    private lateinit var websocket: WebSocket
    private var token = ""
    val gson = Gson()
    val users = master.localUsers

    private fun processExit(code: Int, action: String) {
        if (code != HttpClient.ErrorType.SUCCESS.code) {
            throw RequestException(action, code)
        }
    }

    fun login(username: String){
        println("Trying to login in $URL_HEAD...")

        val urlToSend = "$URL_HEAD/login"
        val bodyStr = gson.toJson(Login(username))
        val body = RequestBody.create(HttpClient.JSON, bodyStr)
        val request = Request.Builder().
                url(urlToSend).
                header("Content-Type", "application/json").
                post(body).
                build()
        val response = client.newCall(request).execute()

        val wwwAuthenticate = response.header("WWW-Authenticate")
        if (wwwAuthenticate == "Token realm='Username is already in use'") {
            throw RequestException("login", ErrorType.USERNAME_IS_BUSY.code)
        }

        processExit(response.code(), "login")

        val responseBody = response.body()?.string() ?: throw Exception()
        val loginData = gson.fromJson(responseBody, LoginData::class.java)

        token = loginData.myToken

        users@ for (newbie in loginData.users){
            for (user in users){
                if (user.id == newbie.id){
                    continue@users
                }
            }
            users.add(newbie)
        }

        println("Login successful, username: $username\n")

        messages@ for (message in loginData.messages){
            for (localUser in users) {
                if (localUser.id == message.author) {
                    println("${localUser.username}: ${message.message}")
                    continue@messages
                }
            }
            println("<UNKNOWN>: ${message.message}")
        }
    }

    fun connect(){
        //println("Trying to connect to websocket...")

        val urlToSend = "$URL_HEAD/messages"
        val request = Request.Builder().
            url(urlToSend).
            header("Authorization", "Token $token").
            get().
            build()

        websocket = client.newWebSocket(request, ServerConnectionListener(this))

        //println("Connection established")
    }

    fun logout() {
        val urlToSend = "$URL_HEAD/logout"
        val emptyBody = RequestBody.create(HttpClient.JSON, "")
        val request = Request.Builder().
                url(urlToSend).
                header("Authorization", "Token $token").
                post(emptyBody).
                build()
        val response = client.newCall(request).execute()

        processExit(response.code(), "logout")

        websocket.close(1000, null)
    }

    fun sendMessage(text: String) {
        val message = MessageText(text, token)
        val msgStr = gson.toJson(message)
        websocket.send(msgStr)
    }

    fun endWork(){
        master.interrupt()
    }
}