package server.handlers

import com.google.gson.JsonSyntaxException
import io.undertow.server.HttpServerExchange
import server.core.ServerData
import server.data.json.Login
import server.errorhandling.exceptions.BusyUserException
import server.errorhandling.exceptions.NotSupportedFormatException

class LoginHandler: RequestHandler() {
    override fun process(exchange: HttpServerExchange, serverData: ServerData) {
       // println("LoginHandler")

        checkType(exchange)

        exchange.requestReceiver.receiveFullBytes{
            _: HttpServerExchange?, message: ByteArray? ->

            if (message == null) throw NotSupportedFormatException()
            val login = try {
                gson.fromJson(String(message), Login::class.java)
            } catch (ex: JsonSyntaxException) {
                throw NotSupportedFormatException()
            }

            try {
                if (serverData.isBusy(login.username)){
                    throw BusyUserException()
                }
            } catch (ex: IllegalArgumentException) {
                throw NotSupportedFormatException()
            }


            val user = serverData.addUser(login.username)
            val body = gson.toJson(user)

            setSuccessState(exchange)
            exchange.responseSender.send(body)
        }
    }
}