package client.core

import client.exceptions.RequestException
import data.json.ClientUser
import java.lang.NumberFormatException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*

class Client(private val username: String, private val hostname: String, private val port: Int): Thread() {
    val localUsers = mutableListOf<ClientUser>()
    private val connector = HttpClient(hostname, port.toString(), this)

    override fun run(){
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

        connector.connect()

        val scanner = Scanner(System.`in`)

        while (!isInterrupted) {
            val command = scanner.nextLine()
            when {
                command == "/logout" -> {
                    connector.logout()
                    System.exit(0)
                }
                command == "/list" -> showUsers()
                command.startsWith("/user") -> showUser(command.replaceFirst("/user", ""))
                else -> connector.sendMessage(command)
            }
        }
    }

    private fun showUsers(){
        if (localUsers.isEmpty()) println("No users in list")
        for (user in localUsers){
            val status = when (user.online) {
                true -> "online"
                false -> "offline"
                null -> "disconnected"
            }
            println("ID: ${user.id}, Username: ${user.username}, Status: $status")
        }
    }

    private fun showUser(postfix: String){
        val indexString = postfix.split(" ").first()
        val index = try {
            indexString.toInt()
        } catch (ex: NumberFormatException) {
            println("Wrong command parameter format")
            return
        }
        val user = try {
            localUsers[index]
        } catch (ex: IndexOutOfBoundsException) {
            println("No user with such ID")
            return
        }
        println("ID: ${user.id}, Username: ${user.username}, Status: ${user.online}")
    }
}