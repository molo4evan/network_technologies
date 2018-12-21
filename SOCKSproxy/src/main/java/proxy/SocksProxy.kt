package proxy

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class SocksProxy(listenPort: Int): Thread() {
    companion object {
        const val SOCKS_PROTO: Byte = 0x05
        const val RESERVED: Byte = 0x00
        const val NO_AUTH: Byte = 0x00
        const val UNHANDLED_AUTH: Byte = 0xFF.toByte()
        const val TCP_ESTABLISH: Byte = 0x01
        const val IPV4: Byte = 0x01
        const val DOMAIN: Byte = 0x03
        const val IPV6: Byte = 0x04
        const val SUCCESS: Byte = 0x00
        const val PROXY_ERROR: Byte = 0x01
        const val HOST_UNREACHABLE: Byte = 0x04
        const val PROTOCOL_ERROR: Byte = 0x07
    }

    class Attachment(var partnerKey: SelectionKey? = null) {
        companion object {
        var response_code: Byte = SUCCESS
            const val BUFSIZE = 8192
        }

        enum class ConnectState {
            SERVER,
            NEWBIE,
            SENT_HELLO,
            GOT_HELLO_ANSWER,
            SENT_REQUEST,
            CONNECTED,
            ESTABLISHED,
            AUTH_ERROR,
            REQUEST_ERROR
        }

        val inBuffer = ByteBuffer.allocate(BUFSIZE)
        var outBuffer = ByteBuffer.allocate(BUFSIZE)
        var state = ConnectState.NEWBIE
    }

    private val selector = Selector.open()
    private val channel = ServerSocketChannel.open()

    init {
        channel.configureBlocking(false)
        channel.socket().bind(InetSocketAddress(listenPort))
        channel.register(selector, SelectionKey.OP_ACCEPT)
    }

    override fun run() {
        while (true) {
            val count = selector.select()
            if (count <  0) continue
            else {
                val iterator = selector.selectedKeys().iterator()
                while (iterator.hasNext()){
                    val key = iterator.next()
                    if (key.isValid){
                        try {
                            when {
                                key.isAcceptable -> accept(key)
                                key.isReadable -> read(key)
                                key.isWritable -> write(key)
                                key.isConnectable -> connect(key)
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            close(key)
                        }
                    }
                    iterator.remove()
                }
            }
        }
    }

    private fun accept(key: SelectionKey){
        val clientChannel = (key.channel() as ServerSocketChannel).accept()
        val clientKey = clientChannel.register(selector, 0)

        val attachment = Attachment()
        clientKey.attach(attachment)
        clientKey.interestOps(SelectionKey.OP_READ)

        println("Client with key $clientKey accepted")
    }

    private fun read(key: SelectionKey){
        val channel = key.channel() as SocketChannel
        val attachment = key.attachment() as Attachment
        val count = channel.read(attachment.inBuffer)
        if (count > 1){
            println("Read $count bytes from $key")
            when (attachment.state){
                Attachment.ConnectState.NEWBIE -> processTypes(key)
                Attachment.ConnectState.GOT_HELLO_ANSWER -> processRequest(key)
                else -> processRead(key)
            }
        } else {
            close(key)
        }
    }

    private fun setupKeyFromRead(key: SelectionKey, state: Attachment.ConnectState, status: Byte){
        val attachment = key.attachment() as Attachment
        attachment.state = state
        setStatusMessage(attachment.outBuffer, status)
        key.interestOps(SelectionKey.OP_WRITE)
        attachment.inBuffer.flip()
    }

    private fun setStatusMessage(buf: ByteBuffer, status: Byte){
        buf.compact()
        buf.put(SOCKS_PROTO)
        buf.put(status)
    }

    private fun processTypes(key: SelectionKey) {
        val attachment = key.attachment() as Attachment
        val buffer = attachment.inBuffer
        if (buffer.get() != SOCKS_PROTO) {
            setupKeyFromRead(key, Attachment.ConnectState.AUTH_ERROR, PROTOCOL_ERROR)
            return
        }

        val authNum = buffer.get()
        var correct = false
        for (i in 0..authNum){
            if (buffer.get() == NO_AUTH){
                correct = true
                break
            }
        }

        if (!correct){
            setupKeyFromRead(key, Attachment.ConnectState.AUTH_ERROR, UNHANDLED_AUTH)
            return
        }

        setupKeyFromRead(key, Attachment.ConnectState.SENT_HELLO, NO_AUTH)
    }

    private fun processRequest(key: SelectionKey) {
        val answerBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)

        answerBuffer.put(SOCKS_PROTO)
        answerBuffer.put(SUCCESS)
        answerBuffer.put(RESERVED)

        val attachment = key.attachment() as Attachment
        val buffer = attachment.inBuffer
        if (buffer.get() != SOCKS_PROTO || buffer.get() != TCP_ESTABLISH || buffer.get() != RESERVED) {
            setupKeyFromRead(key, Attachment.ConnectState.REQUEST_ERROR, PROTOCOL_ERROR)
            return
        }

        lateinit var serverAddress: InetSocketAddress

        try {
            when (buffer.get()) {
                IPV4 -> {
                    val ip = ByteArray(4)
                    buffer.get(ip)
                    val portBytes = ByteArray(2)
                    buffer.get(portBytes)
                    val port = portBytes[0].toInt().shl(8) + portBytes[1].toInt()
                    serverAddress = InetSocketAddress(InetAddress.getByAddress(ip), port)

                    answerBuffer.put(ip)
                    answerBuffer.put(portBytes)
                }
                DOMAIN -> {
                    val length = buffer.get()
                    val domain = ByteArray(length.toInt())
                    buffer.get(domain)
                    val portBytes = ByteArray(2)
                    buffer.get(portBytes)
                    val port = portBytes[0].toInt().shl(8) + portBytes[1].toInt()
                    val addr = InetAddress.getByName(String(domain))
                    serverAddress = InetSocketAddress(addr, port)

                    answerBuffer.put(addr.address)
                    answerBuffer.put(portBytes)
                }
                IPV6 -> {
                    val ip = ByteArray(16)
                    buffer.get(ip)
                    val portBytes = ByteArray(2)
                    buffer.get(portBytes)
                    val port = portBytes[0].toInt().shl(8) + portBytes[1].toInt()
                    serverAddress = InetSocketAddress(InetAddress.getByAddress(ip), port)

                    answerBuffer.put(ip)
                    answerBuffer.put(portBytes)
                }
                else -> {
                    setupKeyFromRead(key, Attachment.ConnectState.AUTH_ERROR, PROTOCOL_ERROR)
                    return
                }
            }
        } catch (ex: BufferUnderflowException) {
            setupKeyFromRead(key, Attachment.ConnectState.AUTH_ERROR, PROTOCOL_ERROR)
            return
        }

        val serverChannel = SocketChannel.open()
        serverChannel.configureBlocking(false)
        serverChannel.connect(serverAddress)
        val serverKey = serverChannel.register(selector, SelectionKey.OP_CONNECT)

        val serverAttachment = Attachment(key)
        serverAttachment.state = Attachment.ConnectState.SERVER
        serverKey.attach(serverAttachment)
        (key.attachment() as Attachment).partnerKey = serverKey

        attachment.state = Attachment.ConnectState.SENT_REQUEST

        attachment.outBuffer.compact()
        attachment.outBuffer.put(answerBuffer)
        key.interestOps(0)
        attachment.inBuffer.flip()
    }

    private fun processRead(key: SelectionKey) {
        val att = key.attachment() as Attachment
        if (att.partnerKey == null) throw Exception("processRead: other end is null")
        att.partnerKey!!.interestOps(att.partnerKey!!.interestOps() or SelectionKey.OP_WRITE)
        key.interestOps(key.interestOps() xor SelectionKey.OP_READ)
        att.inBuffer.flip()
    }

    private fun write(key: SelectionKey){
        val attachment = key.attachment() as Attachment
        when (attachment.state) {
            Attachment.ConnectState.SENT_HELLO -> sendAuthInfo(key)
            Attachment.ConnectState.CONNECTED -> sendResponse(key)
            Attachment.ConnectState.AUTH_ERROR -> sendError(key)
            Attachment.ConnectState.REQUEST_ERROR -> sendError(key)
            else -> sendData(key)
        }
    }

    private fun sendAuthInfo(key: SelectionKey){
        val channel = key.channel() as SocketChannel
        val attachment = key.attachment() as Attachment

        val count = channel.write(attachment.outBuffer)
        if (count > 0) {
            attachment.outBuffer.compact()
            if (attachment.outBuffer.hasRemaining()){
                key.interestOps(key.interestOps() xor SelectionKey.OP_WRITE)
            } else {
                attachment.state = Attachment.ConnectState.GOT_HELLO_ANSWER
                key.interestOps(SelectionKey.OP_READ)
            }
        } else {
            close(key)
        }
    }

    private fun sendResponse(key: SelectionKey){
        val channel = key.channel() as SocketChannel
        val attachment = key.attachment() as Attachment

        val count = channel.write(attachment.outBuffer)
        if (count > 0) {
            attachment.outBuffer.compact()
            if (attachment.outBuffer.hasRemaining()){
                key.interestOps(key.interestOps() xor SelectionKey.OP_WRITE)
            } else {
                attachment.state = Attachment.ConnectState.ESTABLISHED
                key.interestOps(SelectionKey.OP_READ)
            }
        } else {
            close(key)
        }
    }

    private fun sendError(key: SelectionKey){
        val channel = key.channel() as SocketChannel
        val attachment = key.attachment() as Attachment

        val count = channel.write(attachment.outBuffer)
        if (count > 0) {
            attachment.outBuffer.compact()
            if (attachment.outBuffer.hasRemaining()){
                key.interestOps(key.interestOps() xor SelectionKey.OP_WRITE)
            } else {
                close(key)
            }
        } else {
            close(key)
        }
    }

    private fun sendData(key: SelectionKey){
        val channel = key.channel() as SocketChannel
        val attachment = key.attachment() as Attachment

        val count = channel.write(attachment.outBuffer)
        if (count > 0) {
            if (attachment.partnerKey == null) {
                close(key)
            }
            attachment.outBuffer.compact()
            println("Write $count bytes to $key")
            attachment.partnerKey!!.interestOps(attachment.partnerKey!!.interestOps() or SelectionKey.OP_READ)
            if (attachment.outBuffer.hasRemaining()){
                key.interestOps(key.interestOps() xor SelectionKey.OP_WRITE)
            }
        } else {
            close(key)
        }
    }

    private fun connect(key: SelectionKey){
        val channel = key.channel() as SocketChannel
        val attachment = key.attachment() as Attachment

        try {
            if (channel.finishConnect()){
                if (attachment.partnerKey == null) throw Exception("connect: other end is null")
                attachment.outBuffer = (attachment.partnerKey!!.attachment() as Attachment).inBuffer
                (attachment.partnerKey!!.attachment() as Attachment).outBuffer = attachment.inBuffer
                (attachment.partnerKey!!.attachment() as Attachment).state = Attachment.ConnectState.CONNECTED
                attachment.partnerKey!!.interestOps(SelectionKey.OP_WRITE)
                key.interestOps(SelectionKey.OP_READ)
            } else {
                setupKeyFromRead(attachment.partnerKey!!, Attachment.ConnectState.REQUEST_ERROR, HOST_UNREACHABLE)
            }
        } catch (ex: IOException) {
            setupKeyFromRead(attachment.partnerKey!!, Attachment.ConnectState.REQUEST_ERROR, PROXY_ERROR)
        }
    }

    private fun close(key: SelectionKey){
        key.channel().close()
        key.cancel()
        val partner = (key.attachment() as Attachment).partnerKey
        if (partner != null) {
            (partner.attachment() as Attachment).partnerKey = null
        }
    }
}