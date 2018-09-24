import OS.Observable
import OS.Subscriber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

const val TIME_TO_SEND = 1000L
const val ALIVE = 1.toByte()
const val DEAD = 0.toByte()

class Sender(
        val group: InetAddress,
        val port: Int,
        private var my_port: String
): Observable, Thread() {
    private val subs = mutableListOf<Subscriber>()

    override fun run() {
        val socket = DatagramSocket(null)
        if (my_port != ""){
            socket.bind(InetSocketAddress(my_port.toInt()))
        }
        val datagram = DatagramPacket(byteArrayOf(ALIVE), 1, group, port)
        while (true) {
            try {
                if (isInterrupted) break
                socket.send(datagram)
                if (isInterrupted) break
                Thread.sleep(TIME_TO_SEND)
            } catch (ex: InterruptedException) {
                break
            }
        }
        val exit = DatagramPacket(byteArrayOf(DEAD), 1, group, port)
        socket.send(exit)
        socket.close()
    }

    override fun addSub(sub: Subscriber) {
        subs.add(sub)
    }

    override fun removeSub(sub: Subscriber) {
        subs.remove(sub)
    }

    override fun notifySubs() {
        for (sub in subs) {
            sub.update()
        }
    }
}