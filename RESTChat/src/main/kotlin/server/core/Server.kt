package server.core

import io.undertow.Undertow
import server.errorhandling.ErrorHandler
import server.errorhandling.exceptions.ServerException

class Server(private val port: Int, private val address: String) {
    private val serverData = ServerData()

    private val underServer: Undertow = Undertow.builder().
            addHttpListener(port, address).
            setHandler{
                exchange ->

                //println("Got request")
                try {
                    HandlerFinder.
                            findHandler(exchange.requestPath, exchange.requestMethod).
                            process(exchange, serverData)
                } catch (ex: ServerException) {
                    ErrorHandler().process(exchange, ex.error)
                }
            }.
            build()

    fun start(){
        underServer.start()
        println("Server started on $address:$port")
    }

    data class ByeMessage(val message: String = "bye!")
}