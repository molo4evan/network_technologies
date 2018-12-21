import java.lang.Exception
import java.util.*

fun main(args: Array<String>) {
    val scan = Scanner(System.`in`)
    while (true){
        val text = scan.nextLine()
        if (text == "exit") return
        try {
            val lang = Resolver.resolve(text)
            println("Язык текста - $lang")
        } catch (ex: Exception) {
            println(ex.message)
        }
    }
}