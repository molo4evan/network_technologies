package server.core

import com.google.gson.Gson
import io.undertow.Handlers.path
import io.undertow.Handlers.websocket
import io.undertow.server.handlers.PathHandler
import io.undertow.websockets.core.AbstractReceiveListener
import io.undertow.websockets.core.BufferedTextMessage
import io.undertow.websockets.core.WebSocketChannel
import io.undertow.websockets.core.WebSockets
import server.handlers.LoginHandler
import server.handlers.LogoutHandler
import data.json.MessageText
import io.undertow.Undertow
import server.handlers.WrongMethodHandler
import java.lang.Exception
import java.net.SocketAddress

class Server(host: String, port: Int) {
    private val data = ServerData()
    private val gson = Gson()
    private val peers = mutableListOf<SocketAddress>()

    private val handler = PathHandler(
        path().
            addExactPath("/login", LoginHandler(data)).
            addExactPath("/logout", LogoutHandler(data)).
            addPrefixPath("/messages", websocket {
                exchange, channel ->

                //println("Websocket connecting")

                val token = try {
                    exchange.requestHeaders["Authorization"]!!.first()!!.replace("Token ", "")
                } catch (ex: Exception) {
                    println("(")
                    return@websocket
                }

                data.setAddress(token, channel)

                data.sendMsgToAll = { msg ->
                    val json = gson.toJson(msg)
                    for (session in channel.peerConnections){
                        WebSockets.sendText(json, session, null)
                    }
                }
                data.sendUserToAll = { user ->
                    val json = gson.toJson(user)
                    for (session in channel.peerConnections){
                        WebSockets.sendText(json, session, null)
                    }
                }

                channel.receiveSetter.set(object: AbstractReceiveListener() {
                    override fun onFullTextMessage(channel: WebSocketChannel?, message: BufferedTextMessage?) {
                        if (message != null && channel != null) {
                            val msgText = message.data
                            val msgObject = gson.fromJson(msgText, MessageText::class.java)
                            data.addMessage(msgObject.message, msgObject.authorToken)
                        }
                    }
                })

                channel.addCloseTask {
                    data.disconnectUsers(it.peerConnections)
                }

                channel.resumeReceives()

                //println("Websocket connected")
            }).addPrefixPath("/", WrongMethodHandler(data))
    )

    private val server = Undertow.builder().addHttpListener(port, host, handler).build()

    data class ByeMessage(val message: String = "bye!")

    fun start() = server.start()
}