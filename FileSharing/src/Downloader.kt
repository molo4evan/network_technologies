import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.util.concurrent.TimeoutException

const val MSG_SIZE = 4096
const val MAX_FILENAME_LENGTH = 4096
const val MAX_FILE_SIZE = 1099511627776
const val TIME_TO_WAIT = 3000

val BUFFER_SIZE = if (MSG_SIZE > MAX_FILENAME_LENGTH) MSG_SIZE else MAX_FILENAME_LENGTH

class Downloader(private val socket: Socket): Thread() {
    private var received_size = 0L
    var header = ""

    override fun run() {
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())
        val buffer = ByteArray(BUFFER_SIZE)
        socket.soTimeout = TIME_TO_WAIT

        val filename: String
        val file_size: Long
        try {
            val name_size = input.readInt()
            if (name_size > MAX_FILENAME_LENGTH) {
                socket.close()
                return
            }

            if (input.read(buffer, 0, name_size) != name_size) {
                println("Incorrect filename receiving")
                socket.close()
                return
            }
            filename = String(buffer.sliceArray(0 until name_size), Charset.forName("UTF-8"))
            file_size = input.readLong()
            if (file_size > MAX_FILE_SIZE) {
                socket.close()
                return
            }
        } catch (ex: TimeoutException) {
            socket.close()
            return
        }

        header = filename
        println("$header: File size - ${file_size.toDouble() / 1024} KB")

        val file = File("./uploads/$filename")
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        val writer = FileOutputStream(file)
        val speedometer = Speeder(this)
        speedometer.start()
        while (received_size < file_size) {
            try {
                val received = input.read(buffer, 0, MSG_SIZE)
                writer.write(buffer, 0, received)
                updateSize(received.toLong())
            } catch (ex: TimeoutException) {
                writer.close()
                file.delete()
                socket.close()
                speedometer.interrupt()
                return
            }
        }
        speedometer.interrupt()
        output.write("OK".toByteArray())
        socket.close()
        println("$header: Downloading done")
    }

    private fun updateSize(add: Long) {
        synchronized(received_size) {
            received_size += add
        }
    }

    fun getRecvSize(): Long {
        synchronized(received_size) {
            return received_size
        }
    }
}