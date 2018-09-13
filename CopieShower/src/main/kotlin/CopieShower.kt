import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

const val TIME_TO_SEND = 1000L

fun main(args: Array<String>) {
    val group = InetAddress.getByName("228.13.37.1")
    val socket = MulticastSocket(13269)
    socket.joinGroup(group)
    val updater = Updater(group, socket)   // ???
    val sender = Sender(socket)
    val gui = GUI(updater, sender)
    updater.addSub(gui)
    updater.run()
    sender.run()
}