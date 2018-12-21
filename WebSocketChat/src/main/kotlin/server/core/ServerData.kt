package server.core

import data.json.ClientUser
import data.json.Message
import data.json.UserInfo
import io.undertow.websockets.core.WebSocketChannel
import server.errorhandling.exceptions.ServerException
import java.net.SocketAddress
import java.util.*

class ServerData {
    companion object {
        private const val MILLIS_TO_LEAVE = 300_000L
    }

    val messages = mutableListOf<Message>()
    val users = mutableListOf<UserInfo>()
    private var messageId = 0
    private var userId = 0

    private val peers = mutableListOf<String>()

    private val disconnector = Timer(true)
    private val leaverChecker: LeaverChecker = LeaverChecker(this)

    lateinit var sendMsgToAll: (Message) -> Unit
    lateinit var sendUserToAll: (ClientUser) -> Unit

    init {
        //disconnector.schedule(leaverChecker, 0, MILLIS_TO_LEAVE)
    }

    fun addUser(username: String): UserInfo {
        synchronized(users){
            for (user in users) {
                if (user.username == username && user.online != true) {
                    user.online = true
                    user.token = UUID.randomUUID().toString()
                    println("LOGIN USER $user")
                    if (::sendUserToAll.isInitialized){
                        val cliUser = ClientUser(user.id, user.username, user.online)
                        sendUserToAll(cliUser)
                    } else {
                        throw ServerException()
                    }
                    return user
                }
            }
            val user = UserInfo(userId++, username, true)
            users.add(user)
            println("ADDED USER $user")
            if (::sendUserToAll.isInitialized){
                val cliUser = ClientUser(user.id, user.username, user.online)
                sendUserToAll(cliUser)
            }
            return user
        }
    }

    fun setAddress(token: String, channel: WebSocketChannel){
        connections@ for (connection in channel.peerConnections){
            for (peer in peers){
                if (peer == connection.peerAddress.toString()){
                    continue@connections
                }
            }
            peers.add(connection.peerAddress.toString())
            for (user in users){
                if (user.token == token){
                    user.address = connection.peerAddress.toString()
                    return
                }
            }
            return
        }


    }

    fun disconnectUsers(connections: Set<WebSocketChannel>){
        mainFor@ for (user in users) {
            if (user.online != true) continue@mainFor
            for (connection in connections) {
                if (user.address == connection.peerAddress.toString()) continue@mainFor
            }
            userTimeout(user.id)
        }
    }

    fun getUser(userId: Int): UserInfo? {
        synchronized(users){
            for (user in users){
                if (user.id == userId) return user
            }
            return null
        }
    }

    fun isBusy(username: String): Boolean {
        synchronized(users){
            for (user in users) {
                if (user.username == username && user.online == true) {
                    return true
                }
            }
            return false
        }
    }

    fun isAuthorizedToken(token: String):Boolean {
        synchronized(users){
            for (user in users) {
                if (user.token == token) {
                    leaverChecker.userIsAlive(user.id)
                    return true
                }
            }
            return false
        }
    }

    fun userLogout(userToken: String) {
        synchronized(users){
            for (user in users) {
                if (user.token == userToken) {
                    user.online = false
                    peers.remove(user.address)
                    println("LOGOUT USER ${user.username}")
                    leaverChecker.userIsAlive(user.id)
                    if (::sendUserToAll.isInitialized){
                        val cliUser = ClientUser(user.id, user.username, user.online)
                        sendUserToAll(cliUser)
                    } else {
                        throw ServerException()
                    }
                    return
                }
            }
        }
    }

    fun userTimeout(userId: Int) {
        synchronized(users){
            for (user in users) {
                if (user.id == userId) {
                    user.online = null
                    println("DISCONNECTED USER ${user.username}")
                    if (::sendUserToAll.isInitialized){
                        val cliUser = ClientUser(user.id, user.username, user.online)
                        sendUserToAll(cliUser)
                    } else {
                        throw ServerException()
                    }
                    return
                }
            }
        }
    }

    fun getMessages(start: Int, count: Int): List<Message> {
        val out = mutableListOf<Message>()
        var current = start
        while (current < messages.size && current - start < count) {
            out.add(messages[current++])
        }
        return out
    }

    fun addMessage(text: String, authorToken: String): Message {
        synchronized(users){
            for (user in users) {
                if (user.token == authorToken) {
                    if (user.online != true) throw ServerException()
                    val message = Message(messageId++, text, user.id)
                    messages.add(message)
                    println("ADDED MESSAGE $message")
                    if (::sendMsgToAll.isInitialized){
                        sendMsgToAll(message)
                    } else {
                        throw ServerException()
                    }
                    return message
                }
            }
        }
        throw ServerException()
    }
}