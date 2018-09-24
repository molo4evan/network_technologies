import OS.Subscriber
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JTextArea

class GUI(
        private val receiver: Receiver,
        private val sender: Sender
): JFrame("Copies Shower"), Subscriber {
    val copies = JTextArea(16, 16)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE

        val layout = GridBagLayout()
        val c = GridBagConstraints()
        setLayout(layout)
        c.gridx = 0
        c.gridy = GridBagConstraints.RELATIVE

        val addr = JLabel("Address: ${sender.group}")
        layout.setConstraints(addr, c)
        add(addr)
        val port = JLabel("Port: ${sender.port}")
        layout.setConstraints(port, c)
        add(port)
        update()
        layout.setConstraints(copies, c)
        add(copies)
        pack()
        addWindowListener(object : WindowAdapter(){
            override fun windowClosing(p0: WindowEvent?) {
                receiver.end = true
                sender.interrupt()
                dispose()
            }
        })
        isVisible = true
    }

    override fun update() {
        val text = StringBuilder()
        for (copy in receiver.copies.keys){
            text.append(copy.first.hostAddress).append(":").append(copy.second).append("\n")
        }
        copies.text = text.toString()
    }
}