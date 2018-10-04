import java.lang.Error
import java.lang.NumberFormatException

fun main(args: Array<String>) {
    if (args.size != 3) throw Error("No file chosen")
    val port = try { args[2].toInt() } catch (ex: NumberFormatException) {
        throw Error("Incorrect port")
    }
    val sender = Client(args[0], args[1], port)
    sender.start()
}