package server.core

import java.util.*

class LeaverChecker(private val data: ServerData): TimerTask() {
    private var usersAlive = data.users.filter { it.online == true }.map { it.id to false }.toMap().toMutableMap()

    override fun run() {
        synchronized(usersAlive){
            for (userStatus in usersAlive){
                if (!userStatus.value) {
                    data.userTimeout(userStatus.key)
                }
            }
            usersAlive.clear()
            usersAlive.putAll(data.users.filter { it.online == true }.map { it.id to false }.toMap().toMutableMap())
        }
    }

    fun userIsAlive(id: Int) {
        synchronized(usersAlive){
            usersAlive[id] = true
        }
    }
}