package server.handlers

import io.undertow.server.HttpServerExchange
import server.core.ServerData
import server.errorhandling.exceptions.NotFoundException
import server.errorhandling.exceptions.NotSupportedFormatException
import java.lang.NumberFormatException

class UserListHandler: RequestHandler() {
    override fun process(exchange: HttpServerExchange, serverData: ServerData) {
        //println("UserListHandler")

        authorize(exchange, serverData)

        val pathArgs = exchange.requestPath.split("/")
        if (pathArgs.size > 2) {
            val userId = try {
                pathArgs[2].toInt()
            } catch (ex: NumberFormatException) {
                throw NotSupportedFormatException()
            }
            getUser(exchange, serverData, userId)
        } else {
            getUsers(exchange, serverData)
        }
    }

    private fun getUser(exchange: HttpServerExchange, serverData: ServerData, userId: Int){
        val user = serverData.getUser(userId) ?: throw NotFoundException()

        setSuccessState(exchange)
        val body = gson.toJson(user)
        exchange.responseSender.send(body)
    }

    private fun getUsers(exchange: HttpServerExchange, serverData: ServerData){
        setSuccessState(exchange)
        val body = gson.toJson(serverData.users)
        exchange.responseSender.send(body)
    }
}