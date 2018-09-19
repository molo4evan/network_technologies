import OS.Observable
import OS.Subscriber
import java.net.*

const val TIME_TO_DEAD = 3000L

class Updater(
        private val group: InetAddress,
        private val port: Int
): Thread(), Observable {
    val copies = mutableMapOf<InetAddress, Long>()
    private val subs = mutableListOf<Subscriber>()
    var end = false

    override fun run() {
        val socket = MulticastSocket(port)
        socket.joinGroup(group)

        socket.soTimeout = TIME_TO_DEAD.toInt()
        val buffer = ByteArray(15)
        val received = DatagramPacket(buffer, buffer.size)
        while (true){
            try {
                if (end) return
                socket.receive(received)
                if (end) return
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
        when (data[0]) {
            ALIVE -> tryAdd(newbie)
            DEAD -> tryRemove(newbie)
            else -> throw Error("Unknown message: ${data[0]}")
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
        for (copy in copies) {
            println(copy.key)
        }
        println("----------------------")
    }
}