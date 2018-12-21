package server.handlers

import io.undertow.server.HttpServerExchange
import server.core.ServerData
import server.errorhandling.exceptions.NotSupportedFormatException
import java.lang.NumberFormatException

class GetMessagesHandler: RequestHandler() {
    override fun process(exchange: HttpServerExchange, serverData: ServerData) {
       // println("GetMessagesHandler")

        authorize(exchange, serverData)

        val params = exchange.queryParameters
        val offset: Int
        val count: Int
        try {
            offset = params["offset"]?.first?.toInt() ?: 0
            count = params["count"]?.first?.toInt() ?: 0
        } catch (ex: NumberFormatException) {
            throw NotSupportedFormatException()
        }
        if (offset < 0 || count < 0) throw NotSupportedFormatException()

        val messages = serverData.getMessages(offset, count)
        val body = gson.toJson(messages)

        setSuccessState(exchange)
        exchange.responseSender.send(body)
    }
}