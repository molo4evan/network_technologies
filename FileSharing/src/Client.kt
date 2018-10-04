import java.io.*
import java.lang.Error
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.Charset

class Client(private val filepath: String, private val address: String, private val port: Int): Thread() {
    override fun run() {
        val file = File(filepath)
        if (!file.exists()) throw Error("No such file")
        if (file.length() > MAX_FILE_SIZE) throw Error("Too big file")
        if (file.name.length > MAX_FILENAME_LENGTH) throw Error("Too long filename")
        val reader = FileInputStream(file)

        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(address, port))
        } catch (ex: IllegalArgumentException) {
            throw Error("Address or port is incorrect")
        }
        val output = DataOutputStream(socket.getOutputStream())
        val input = DataInputStream(socket.getInputStream())

        try {
            output.writeInt(file.name.length)

            output.write(file.name.toByteArray())

            output.writeLong(file.length())

            val buf_array = ByteArray(BUFFER_SIZE)
            var size = reader.read(buf_array, 0, MSG_SIZE)
            while (size > 0) {
                output.write(buf_array, 0, size)
                size = reader.read(buf_array, 0, MSG_SIZE)
            }

            val answer_size = input.read(buf_array, 0, MSG_SIZE)
            if (String(buf_array.sliceArray(0 until 2), Charset.forName("UTF-8")) != "OK") {
                println("Upload error: incorrect server answer")
            } else {
                println("Upload complete successfully")
            }
        } catch (ex: IOException) {
            println("Upload error: server closed connection")
        }
    }
}