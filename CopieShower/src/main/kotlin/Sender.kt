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
        private var addr: String
): Thread(), Observable {
    private val subs = mutableListOf<Subscriber>()
    var end = false

    override fun run() {
        val socket = if (addr == ""){
            DatagramSocket()
        } else {
            DatagramSocket(InetSocketAddress(addr, port))
        }
        val datagram = DatagramPacket(byteArrayOf(ALIVE), 1, group, port)
        while (true) {
            try {
                if (end) break
                socket.send(datagram)
                if (end) break
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