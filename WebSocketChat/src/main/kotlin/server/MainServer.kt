package server

import server.core.Server

fun main(args: Array<String>) {
    Server(args[0], args[1].toInt()).start()
}