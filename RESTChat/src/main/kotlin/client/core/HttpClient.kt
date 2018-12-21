package client.core

import client.json.Message
import client.json.UserInfo
import client.exceptions.RequestException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import server.data.json.*

class HttpClient(hostname: String, port: String) {
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
    private val gson = Gson()
    private var token = 0

    private fun processExit(code: Int, action: String) {
        if (code != HttpClient.ErrorType.SUCCESS.code) {
            throw RequestException(action, code)
        }
    }

    fun login(username: String){
        val urlToSend = "$URL_HEAD/login"
        val bodyStr = gson.toJson(Login(username))
        val body = RequestBody.create(HttpClient.Companion.JSON, bodyStr)
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
        val userInfo = gson.fromJson(responseBody, UserInfo::class.java)
        token = userInfo.token
    }

    fun logout() {
        val urlToSend = "$URL_HEAD/logout"
        val emptyBody = RequestBody.create(HttpClient.Companion.JSON, "")
        val request = Request.Builder().
                url(urlToSend).
                header("Authorization", "Token $token").
                post(emptyBody).
                build()
        val response = client.newCall(request).execute()

        processExit(response.code(), "logout")
    }

    fun getUsers(): List<UserInfo> {
        val urlToSend = "$URL_HEAD/users"
        val request = Request.Builder().
                url(urlToSend).
                header("Authorization", "Token $token").
                method("GET", null).
                build()
        val response = client.newCall(request).execute()

        processExit(response.code(), "getUsers")

        val responseBody = response.body()?.string() ?: throw Exception()
        val objType = (object: TypeToken<List<UserInfo>>(){}).type
        return gson.fromJson(responseBody, objType)
    }

    fun getUser(id: Int): UserInfo {
        val urlToSend = "$URL_HEAD/users/$id"
        val request = Request.Builder().
                url(urlToSend).
                header("Authorization", "Token $token").
                method("GET", null).
                build()
        val response = client.newCall(request).execute()

        processExit(response.code(), "getUser")

        val responseBody = response.body()?.string() ?: throw Exception()
        return gson.fromJson(responseBody, UserInfo::class.java)
    }

    fun getMessages(offset: Int? = null, count: Int? = null): List<Message> {
        val params = if (offset != null && count != null) {
            "?offset=$offset&count=$count"
        } else if (offset != null) {
            "?offset=$offset"
        } else if (count != null) {
            "?count=$count"
        } else ""
        val urlToSend = "$URL_HEAD/messages$params"
        val request = Request.Builder().
                url(urlToSend).
                header("Authorization", "Token $token").
                method("GET", null).
                build()
        val response = client.newCall(request).execute()

        processExit(response.code(), "getMessages")

        val responseBody = response.body()?.string() ?: throw Exception()
        val objType = (object: TypeToken<List<Message>>(){}).type
        return gson.fromJson(responseBody, objType)
    }

    fun sendMessage(text: String): Int {
        val message = MessageText(text)
        val urlToSend = "$URL_HEAD/messages"
        val bodyStr = gson.toJson(message)
        val body = RequestBody.create(HttpClient.Companion.JSON, bodyStr)
        val request = Request.Builder().
                url(urlToSend).
                header("Authorization", "Token $token").
                post(body).
                build()
        val response = client.newCall(request).execute()

        processExit(response.code(), "sendMessage")

        val responseBody = response.body()?.string() ?: throw Exception()
        val msgResult = gson.fromJson(responseBody, MessageToResponse::class.java)
        return msgResult.id
    }
}