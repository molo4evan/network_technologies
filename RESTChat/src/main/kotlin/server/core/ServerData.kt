package server.core

import client.json.Message
import client.json.UserInfo
import server.errorhandling.exceptions.ServerException
import java.util.*

class ServerData {
    companion object {
        private const val MILLIS_TO_LEAVE = 300_000L
    }

    private val mesages = mutableListOf<Message>()
    val users = mutableListOf<UserInfo>()
    private var messageId = 0
    private var userId = 0

    private val disconnector = Timer(true)
    private val leaverChecker = LeaverChecker(this)

    init {
        disconnector.schedule(leaverChecker, 0, MILLIS_TO_LEAVE)
    }

    fun addUser(username: String): UserInfo {
        synchronized(users){
            for (user in users) {
                if (user.username == username && user.online != true) {
                    user.online = true
                    return user
                }
            }
            val user = UserInfo(userId++, username, true)
            users.add(user)
            println("ADDED USER $user")
            return user
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

    fun isAuthorizedToken(token: Int) :Boolean {
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

    fun userLogout(userToken: Int) {
        synchronized(users){
            for (user in users) {
                if (user.token == userToken) {
                    user.online = false
                    println("LOGOUT USER ${user.username}")
                    leaverChecker.userIsAlive(user.id)
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
                    return
                }
            }
        }
    }

    fun getMessages(start: Int, count: Int): List<Message> {
        val out = mutableListOf<Message>()
        var current = start
        while (current < mesages.size && current - start < count) {
            out.add(mesages[current++])
        }
        return out
    }

    fun addMessage(text: String, authorToken: Int): Message {
        synchronized(users){
            for (user in users) {
                if (user.token == authorToken) {
                    if (user.online != true) throw ServerException()
                    val message = Message(messageId++, text, user.id)
                    mesages.add(message)
                    println("ADDED MESSAGE $message")
                    return message
                }
            }
        }
        throw ServerException()
    }
}