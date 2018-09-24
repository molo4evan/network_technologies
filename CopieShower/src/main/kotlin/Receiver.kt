import OS.Observable
import OS.Subscriber
import java.net.*

const val TIME_TO_DIE = 3000

class Receiver(
        private val group: InetAddress,
        private val port: Int
): Thread(), Observable {
    val copies = mutableMapOf<Pair<InetAddress, Int>, Long>()
    private val subs = mutableListOf<Subscriber>()
    var end = false

    override fun run() {
        val socket = MulticastSocket(port)
        socket.joinGroup(group)
        socket.soTimeout = TIME_TO_DIE
        val buffer = ByteArray(1)
        val received = DatagramPacket(buffer, buffer.size)
        while (true){
            try {
                if (isInterrupted) break
                socket.receive(received)
                if (isInterrupted) break
                processCopies(received.data, received.address, received.port)
            } catch (ex: SocketTimeoutException) {
                checkUndeads()
            }
        }
        socket.close()
    }

    private fun checkUndeads(){
        val cmp_point = System.currentTimeMillis()
        println("Current time: $cmp_point")
        var changed = false
        for (copy in copies) {
            val difference = cmp_point - copy.value
            println("${copy.key.first.hostAddress}:${copy.key.second} age is $difference")
            if (difference > TIME_TO_DIE) {
                copies.remove(copy.key)
                changed = true
            }
        }
        if (changed) {
            notifySubs()
        }
    }

    private fun processCopies(data: ByteArray, newbie_addr: InetAddress, newbie_port: Int){
        when (data[0]) {
            ALIVE -> tryAdd(newbie_addr, newbie_port)
            DEAD -> tryRemove(newbie_addr, newbie_port)
            else -> throw Error("Unknown message: ${data[0]}")
        }
    }

    private fun tryAdd(addr: InetAddress, port: Int){
        val check = Pair(addr, port)
        if (!copies.containsKey(check)) {
            copies[check] = System.currentTimeMillis()
            checkUndeads()
            notifySubs()
        } else {
            copies[check] = System.currentTimeMillis()
            checkUndeads()
        }
    }

    private fun tryRemove(addr: InetAddress, port: Int){
        val toDelete = Pair(addr, port)
        if (copies.containsKey(toDelete)) {
            copies.remove(toDelete)
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