import OS.Observable
import OS.Subscriber
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException

const val TIME_TO_DEAD = 3000L

class Updater(val my_addr: InetAddress, private val socket: MulticastSocket): Thread(), Observable {
    val copies = mutableMapOf<InetAddress, Long>()
    val port = socket.port
    private val subs = mutableListOf<Subscriber>()

    override fun run() {
        socket.soTimeout = TIME_TO_DEAD.toInt()
        val buffer = ByteArray(15)
        val received = DatagramPacket(buffer, buffer.size)
        while (true){
            try {
                socket.receive(received)
                processCopies(received.data, received.address)
            } catch (ex: SocketTimeoutException) {
                checkUndeads()
            }
        }

    }

    private fun checkUndeads(){
        val cmp_point = System.currentTimeMillis()
        var changed = false
        for (copy in copies) {
            if (copy.value - cmp_point > TIME_TO_DEAD) {
                copies.remove(copy.key)
                changed = true
            }
        }
        if (changed) {
            notifySubs()
        }
    }

    private fun processCopies(data: ByteArray, newbie: InetAddress){
        val msg = data.toString()
        when (msg) {
            "I'm alive!" -> tryAdd(newbie)
            "Goodbye" -> tryRemove(newbie)
            else -> throw Error("Unknown message: $msg")
        }
    }

    private fun tryAdd(addr: InetAddress){
        if (!copies.containsKey(addr)) {
            copies[addr] = System.currentTimeMillis()
            checkUndeads()
            notifySubs()
        } else {
            copies[addr] = System.currentTimeMillis()
            checkUndeads()
        }
    }

    private fun tryRemove(addr: InetAddress){
        if (copies.containsKey(addr)) {
            copies.remove(addr)
            checkUndeads()
            notifySubs()
        } else {
            checkUndeads()
        }
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