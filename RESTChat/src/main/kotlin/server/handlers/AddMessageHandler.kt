package server.handlers

import com.google.gson.JsonSyntaxException
import io.undertow.server.HttpServerExchange
import server.core.ServerData
import client.json.MessageText
import client.json.MessageToResponse
import server.errorhandling.exceptions.NotSupportedFormatException
import java.lang.IllegalArgumentException

class AddMessageHandler: RequestHandler() {
    override fun process(exchange: HttpServerExchange, serverData: ServerData) {
        //println("AddMessageHandler")

        val userToken =  authorize(exchange, serverData)
        checkType(exchange)

        exchange.requestReceiver.receiveFullBytes{
            _: HttpServerExchange?, message: ByteArray? ->

            if (message == null) throw NotSupportedFormatException()
            val messageText = try {
                gson.fromJson(String(message), MessageText::class.java)
            } catch (ex: JsonSyntaxException) {
                throw NotSupportedFormatException()
            }

            val fullMessage = try {
                serverData.addMessage(messageText.message, userToken)
            } catch (ex: IllegalArgumentException) {
                throw NotSupportedFormatException()
            }


            setSuccessState(exchange)
            val body = gson.toJson(MessageToResponse(fullMessage.id, fullMessage.message))
            exchange.responseSender.send(body)
        }
    }
}