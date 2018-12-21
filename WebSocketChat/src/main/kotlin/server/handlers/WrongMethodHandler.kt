package server.handlers

import io.undertow.server.HttpServerExchange
import server.core.ServerData
import server.errorhandling.exceptions.WrongMethodException

class WrongMethodHandler(data: ServerData): BaseHandler(data) {
    override fun process(exchange: HttpServerExchange?) {
        throw WrongMethodException()
    }
}