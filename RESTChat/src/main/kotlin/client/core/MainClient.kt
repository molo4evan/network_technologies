package client.core

import client.exceptions.RequestException
import client.json.UserInfo
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*

class MainClient(
        private val username: String,
        private val hostname: String,
        private val port: String
): Thread() {
    private val userList = mutableListOf<UserInfo>()
    private val connector = HttpClient(hostname, port)
    private val fetcher = MessageFetcher(connector, userList, this)
    val scan = Scanner(System.`in`)

    override fun run() {
        println("Trying to connect to $hostname:$port")
        try {
            connector.login(username)
        } catch (ex: RequestException) {
            println(ex.errorMessage)
            return
        } catch (ex: ConnectException) {
            println("Can't connect with server")
            return
        } catch (ex: SocketTimeoutException) {
            println("Can't connect with server")
            return
        }

        println("Login successful, username: $username")
        fetcher.start()

        while (true) {
            val command = try {
                scan.nextLine()
            } catch (ex: IllegalStateException) {
                endWork()
                return
            }

            if (command == "/logout") {
                endWork()
                return
            } else {
                try {
                    processCommand(command)
                } catch (ex: RequestException) {
                    println(ex.errorMessage)
                    endWork()
                    return
                }
            }
        }
    }

    private fun processCommand(command: String) {
        when {
            command == "/list" -> updateAndShowUserList()
            command.startsWith("/user") -> showUserInfo(command)
            else -> send(command)
        }
    }

    private fun updateAndShowUserList(){
        fetcher.updateUserList()
        synchronized(userList) {
            for (user in userList) {
                val status = when {
                    user.online == null -> "disconnected"
                    user.online!! -> "online"
                    else -> "offline"
                }
                println("Username: ${user.username}, User ID: ${user.id}, Status: $status.")
            }
        }

    }

    private fun showUserInfo(command: String) {
        val userId = try {
            command.split(" ")[1].toInt()
        } catch (ex: NumberFormatException) {
            println("Incorrect user ID format: expected number but got '${command.split(" ")[1]}'")
            return
        }

        val info = try {
            connector.getUser(userId)
        } catch (ex: RequestException) {
            if (ex.errorCode == HttpClient.ErrorType.NOT_FOUND.code) {
                println("User with such ID does not exists")
                return
            } else {
                endWork()
                throw ex
            }
        }

        val status = when {
            info.online == null -> "disconnected"
            info.online!! -> "online"
            else -> "offline"
        }
        println("Username: ${info.username}, User ID: ${info.id}, Status: $status.")
    }

    private fun send(message: String) = fetcher.updateLastID(connector.sendMessage(message))

    private fun endWork() {
        fetcher.interrupt()
        try {
            connector.logout()
            println("Logout successful.")
        } catch (ex: RequestException) {
            println(ex.errorMessage)
        }
    }

    fun close() = scan.close()
}