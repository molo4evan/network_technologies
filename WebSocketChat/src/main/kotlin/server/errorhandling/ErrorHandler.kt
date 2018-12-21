package server.errorhandling

import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString

class ErrorHandler {
    enum class ErrorType(val code: Int) {
        NOT_SUPPORTED(400),
        NO_TOKEN(401),
        USERNAME_IS_BUSY(401),
        WRONG_TOKEN(403),
        NOT_FOUND(404),
        WRONG_METHOD(405),
        OTHER(500)
    }

    fun process(exchange: HttpServerExchange, error: ErrorType){
        if (error == ErrorType.USERNAME_IS_BUSY) {
            responseUsernameIsBusy(exchange)
        } else {
            sendErrorCode(exchange, error.code)
        }
    }

    private fun sendErrorCode(exchange: HttpServerExchange, code: Int) {
        exchange.statusCode = code
        exchange.responseSender.send("")
    }

    private fun responseUsernameIsBusy(exchange: HttpServerExchange) {
        exchange.responseHeaders.add(
                HttpString("WWW-Authenticate"),
                "Token realm='Username is already in use'"
        )
        sendErrorCode(exchange, ErrorType.USERNAME_IS_BUSY.code)
    }
}