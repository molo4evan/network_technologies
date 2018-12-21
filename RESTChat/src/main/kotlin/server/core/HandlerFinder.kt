package server.core

import io.undertow.util.HttpString
import io.undertow.util.Methods
import server.data.HandlerInfo
import server.errorhandling.exceptions.WrongMethodException
import server.handlers.*

object HandlerFinder {
    private val handlers = mutableMapOf<HandlerInfo, RequestHandler>()

    init {
        handlers[HandlerInfo("login", Methods.POST)] = LoginHandler()
        handlers[HandlerInfo("logout", Methods.POST)] = LogoutHandler()
        handlers[HandlerInfo("users", Methods.GET)] = UserListHandler()
        handlers[HandlerInfo("messages", Methods.GET)] = GetMessagesHandler()
        handlers[HandlerInfo("messages", Methods.POST)] = AddMessageHandler()
    }

    fun findHandler(path: String, method: HttpString): RequestHandler {
        val pathParts = path.split("/")
        val commAndParams = pathParts[1].split("?")

        val command = if (commAndParams.size > 1) pathParts[1] else commAndParams[0]

        return handlers[HandlerInfo(command, method)] ?: throw WrongMethodException()
    }
}