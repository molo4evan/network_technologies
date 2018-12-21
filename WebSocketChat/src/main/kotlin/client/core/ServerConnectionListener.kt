package client.core

import com.google.gson.JsonParseException
import data.json.ClientUser
import data.json.Message
import data.json.UserInfo
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ServerConnectionListener(private val client: HttpClient) : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            when {
                text.contains("user") -> {
                    val user = client.gson.fromJson(text, ClientUser::class.java)
                    synchronized(client.users) {
                        for (localUser in client.users) {
                            if (user == localUser) {
                                if (localUser.online == true) {
                                    if (user.online == false) {
                                        println("User ${localUser.username} offline")
                                    } else if (user.online == null) {
                                        println("User ${localUser.username} disconnected")
                                    }
                                } else {
                                    if (user.online == true) {
                                        println("User ${localUser.username} online")
                                    }
                                }
                                localUser.online = user.online
                                return
                            }
                        }
                        client.users.add(user)
                        if (user.online == true) println("User ${user.username} online")
                    }
                }
                text.contains("message") -> {
                    val message = client.gson.fromJson(text, Message::class.java)
                    for (localUser in client.users) {
                        if (localUser.id == message.author) {
                            println("${localUser.username}: ${message.message}")
                            return
                        }
                    }
                    println("<UNKNOWN>: ${message.message}")
                }
                else -> {
                    println("ERROR: Unexpected server answer!")
                    webSocket.close(1, null)
                    client.endWork()
                }
            }
        } catch (ex: JsonParseException) {
            println("ERROR: Unexpected server answer!")
            webSocket.close(1, null)
            client.endWork()
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("Websocket failed")
    }
}