package client

import client.core.MainClient

fun main(args: Array<String>) {
    MainClient(args[0], args[1], args[2]).start()
}