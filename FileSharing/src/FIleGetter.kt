import java.lang.NumberFormatException
import java.util.*

fun main(args: Array<String>) {
    if (args.size != 1) throw Error("incorrect arguments amount")
    val port = try{
        args[0].toInt()
    } catch (ex: NumberFormatException) {
        throw Error("Incorrect port format")
    }
    val server = Server(port)
    println(server.address.hostAddress)
    server.start()
    val scan = Scanner(System.`in`)
    while (true){
        if (scan.nextLine() == "exit"){
            server.endWork()
            break
        }
    }
}