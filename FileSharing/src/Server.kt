import java.io.File
import java.lang.Error
import java.net.*

class Server(port: Int): Thread() {
    private val serSock: ServerSocket
    val address: InetAddress

    init {
        val dir = File("./uploads")
        if (!dir.exists()){
            dir.mkdir()
        }

        address = getInterfaceAddress()
        serSock = ServerSocket(port, 0, address)
    }

    override fun run() {
        while (!isInterrupted) {
            try {
                val socket = serSock.accept()
                val servant = Downloader(socket)
                servant.start()
            } catch (ex: SocketException){
                break
            }
        }
        serSock.close()
    }

    fun endWork(){
        interrupt()
        serSock.close()
    }

    private fun getInterfaceAddress(): InetAddress {
        for (inter in NetworkInterface.getNetworkInterfaces()) {
            for (addr in inter.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) return addr
            }
        }
        throw Error("No interface address found")
    }
}