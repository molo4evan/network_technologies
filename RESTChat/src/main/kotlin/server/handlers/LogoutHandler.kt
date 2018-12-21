package server.handlers

import io.undertow.server.HttpServerExchange
import server.core.Server
import server.core.ServerData

class LogoutHandler: RequestHandler() {
    override fun process(exchange: HttpServerExchange, serverData: ServerData) {
        //println("LogoutHandler")

        val token = authorize(exchange, serverData)
        serverData.userLogout(token)

        exchange.requestReceiver.receiveFullBytes{
            _: HttpServerExchange?, _: ByteArray? ->

            setSuccessState(exchange)
            val body = gson.toJson(Server.ByeMessage())
            exchange.responseSender.send(body)
        }
    }
}