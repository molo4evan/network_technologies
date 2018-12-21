package client.core

import client.exceptions.RequestException
import client.json.UserInfo
import java.lang.IndexOutOfBoundsException

class MessageFetcher(
        private val connector: HttpClient,
        private val userList: MutableList<UserInfo>,
        private val host: MainClient
): Thread() {
    companion object {
        const val FETCHING_AMOUNT = 100
        const val WAITING_TIME = 1000L
    }

    private var lastId = 0

    override fun run() {
        while (!isInterrupted) {
            var fetched = false
            val freshMessages = try {
                val msgs = connector.getMessages(lastId, FETCHING_AMOUNT)
                updateUserList()
                msgs
            } catch (ex: RequestException) {
                println(ex.errorMessage)
                host.close()
                return
            }

            for (msg in freshMessages) {
                fetched = true
                val user = try {
                    userList[msg.author]
                } catch (ex: IndexOutOfBoundsException) {
                    println("I don't know user with such id")
                    host.close()
                    return
                }
                println("${user.username}: ${msg.message}")
                if (msg.id > lastId) lastId = msg.id
            }
            if (fetched) {
                lastId++
            }


            try {
                sleep(WAITING_TIME)
            } catch (ex: InterruptedException) {
                return
            }
        }
    }

    fun updateLastID(id: Int) {
        if (id > lastId) lastId = id
    }

    fun updateUserList() {
        val currentUserList = connector.getUsers()

        synchronized(userList) {
            val iter = userList.iterator()
            while (iter.hasNext()) {
                val user = iter.next()
                if (!currentUserList.contains(user)) {
                    if (user.online == true) {
                        println("User ${user.username} disconnected")
                    }
                    userList.remove(user)
                } else {
                    if (user.online == true && currentUserList[currentUserList.indexOf(user)].online != true) {
                        user.online = currentUserList[currentUserList.indexOf(user)].online
                        println("User ${user.username} disconnected")
                    }
                    if (user.online != true && currentUserList[currentUserList.indexOf(user)].online == true) {
                        user.online = currentUserList[currentUserList.indexOf(user)].online
                        println("User ${user.username} connected")
                    }
                }
            }
            for (user in currentUserList) {
                if (!userList.contains(user)) {
                    println("User ${user.username} connected")
                    userList.add(user)
                }
            }
        }

    }
}