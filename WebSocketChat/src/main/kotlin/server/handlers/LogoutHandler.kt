package server.handlers

import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods
import server.core.Server
import server.core.ServerData
import server.errorhandling.exceptions.ServerException
import server.errorhandling.exceptions.WrongMethodException

class LogoutHandler(data: ServerData): BaseHandler(data) {
    override fun process(exchange: HttpServerExchange?) {
        if (exchange == null) throw ServerException()
        if (exchange.requestMethod != Methods.POST) throw WrongMethodException()

        val token = authorize(exchange, data)
        data.userLogout(token)

        exchange.requestReceiver.receiveFullBytes{
                _: HttpServerExchange?, _: ByteArray? ->

            setSuccessState(exchange)
            val body = gson.toJson(Server.ByeMessage())
            exchange.responseSender.send(body)
        }
    }

}