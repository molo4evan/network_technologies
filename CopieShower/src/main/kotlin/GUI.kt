import OS.Subscriber
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JTextArea

class GUI(
        private val info: Updater,
        private val shower: Sender
): JFrame("Copies Shower"), Subscriber {
    val copies = JTextArea(16, 16)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE

        val layout = GridBagLayout()
        val c = GridBagConstraints()
        setLayout(layout)
        c.gridx = 0
        c.gridy = GridBagConstraints.RELATIVE

        val addr = JLabel(info.my_addr.hostAddress)
        layout.setConstraints(addr, c)
        add(addr)
        val port = JLabel(info.port.toString())
        layout.setConstraints(port, c)
        add(port)
        update()
        layout.setConstraints(copies, c)
        add(copies)
        pack()
        addWindowListener(object : WindowAdapter(){
            override fun windowClosing(p0: WindowEvent?) {
                info.interrupt()
                shower.interrupt()
                dispose()
            }
        })
        isVisible = true
    }

    override fun update() {
        val text = StringBuilder()
        for (copie in info.copies.keys){
            text.append(copie.hostAddress).append("\n")
        }
        copies.text = text.toString()
    }
}