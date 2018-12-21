package server.handlers

import com.google.gson.JsonSyntaxException
import io.undertow.server.HttpServerExchange
import server.core.ServerData
import server.errorhandling.exceptions.*
import data.json.*
import io.undertow.util.Methods

class LoginHandler(data: ServerData): BaseHandler(data) {
    override fun process(exchange: HttpServerExchange?) {
        if (exchange == null) throw ServerException()
        if (exchange.requestMethod != Methods.POST) throw WrongMethodException()

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
                if (data.isBusy(login.username)){
                    throw BusyUserException()
                }
            } catch (ex: IllegalArgumentException) {
                throw NotSupportedFormatException()
            }


            val user = data.addUser(login.username)
            val users = mutableListOf<ClientUser>()
            for (serverUser in data.users){
                users.add(ClientUser(serverUser.id, serverUser.username, serverUser.online))
            }
            val logData = LoginData(user.id, user.token, users, data.messages)
            val body = gson.toJson(logData)

            setSuccessState(exchange)
            exchange.responseSender.send(body)
        }
    }

}