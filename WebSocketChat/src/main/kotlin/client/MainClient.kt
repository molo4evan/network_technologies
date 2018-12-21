package client

import client.core.Client

fun main(args: Array<String>) {
    Client(args[0], args[1], args[2].toInt()).start()
}