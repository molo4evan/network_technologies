package server.handlers

import com.google.gson.Gson
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import server.core.ServerData
import server.errorhandling.exceptions.NoTokenException
import server.errorhandling.exceptions.NotSupportedFormatException
import server.errorhandling.exceptions.WrongTokenException
import java.lang.Exception

abstract class RequestHandler {
    companion object {
        private const val SUCCESS_CODE = 200
    }

    protected val gson = Gson()

    abstract fun process(exchange: HttpServerExchange, serverData: ServerData)

    protected fun authorize(exchange: HttpServerExchange, serverData: ServerData): Int {
        val token = try {
            exchange.requestHeaders.getFirst("Authorization").replace("Token ", "").toInt()

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