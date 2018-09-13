import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

class Sender(val socket: MulticastSocket): Thread() {
    override fun run() {
        val msg = "I'm alive!"
        val datagram = DatagramPacket(msg.toByteArray(), msg.length)
        while (true) {
            try {
                socket.send(datagram)
                Thread.sleep(TIME_TO_SEND)
            } catch (ex: InterruptedException) {
                val exit = "Goodbye"
                val exit_data = DatagramPacket(exit.toByteArray(), exit.length)
                socket.send(exit_data)
                socket.close()
            }
        }
    }
}