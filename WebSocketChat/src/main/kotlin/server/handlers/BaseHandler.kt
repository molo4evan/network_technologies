package server.handlers

import com.google.gson.Gson
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import server.core.ServerData
import server.errorhandling.ErrorHandler
import server.errorhandling.exceptions.*
abstract class BaseHandler(protected val data: ServerData): HttpHandler {
    companion object {
        private const val SUCCESS_CODE = 200
    }

    protected val gson = Gson()

    override fun handleRequest(exchange: HttpServerExchange?) {
        try {
            process(exchange)
        } catch (ex: ServerException) {
            if (exchange != null){
                ErrorHandler().process(exchange, ex.error)
            }
        }
    }

    protected abstract fun process(exchange: HttpServerExchange?)

    protected fun authorize(exchange: HttpServerExchange, serverData: ServerData): String {
        val token = try {
            exchange.requestHeaders.getFirst("Authorization").replace("Token ", "")

        } catch (ex: Exception) {
            throw NoTokenException()
        }
        if (!serverData.isAuthorizedToken(token)) {
            //println("WRONG AUTHORIZATION ATTEMPT!")
            throw WrongTokenException()
        }
        return token
    }

    protected fun checkType(exchange: HttpServerExchange) {
        val contentType = exchange.requestHeaders[HttpString("Content-Type")].first
        val isJson = contentType.contains("application/json")
        if (!isJson) {
            throw NotSupportedFormatException()
        }
    }

    protected fun setSuccessState(exchange: HttpServerExchange) {
        exchange.responseHeaders.add(HttpString("Content-Type"), "application/json")
        exchange.statusCode = SUCCESS_CODE
    }
}