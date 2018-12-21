package server

import server.core.Server

fun main(args: Array<String>) {
    val address = args[0]
    val port = args[1].toInt()

    Server(port, address).start()
}