import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.net.InetAddress
import java.net.MulticastSocket
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JTextField

class Init: JFrame() {
    init {
        defaultCloseOperation = EXIT_ON_CLOSE

        val layout = GridBagLayout()
        val c = GridBagConstraints()
        setLayout(layout)
        c.insets = Insets(5, 10, 5, 10)
        c.anchor = GridBagConstraints.WEST
        c.gridx = 0
        c.gridy = GridBagConstraints.RELATIVE

        val ins_mult_addr = JTextField(15)
        layout.setConstraints(ins_mult_addr, c)
        add(ins_mult_addr)

        val ins_port = JTextField(7)
        layout.setConstraints(ins_port, c)
        add(ins_port)

        val ins_your_addr = JTextField(15)
        ins_your_addr.isEnabled = false

        val use_user_addr = JCheckBox("Use custom address")
        use_user_addr.addItemListener { ins_your_addr.isEnabled = use_user_addr.isSelected }
        layout.setConstraints(use_user_addr, c)
        add(use_user_addr)

        layout.setConstraints(ins_your_addr, c)
        add(ins_your_addr)

        val toDispose = this
        val submit = JButton("Submit")
        submit.addActionListener {
            val group = ins_mult_addr.text
            val port = ins_port.text
            val addr = ins_your_addr.text
            val updater = Updater(InetAddress.getByName(group), port.toInt())
            val sender = Sender(InetAddress.getByName(group), port.toInt(), addr)
            val gui = GUI(updater, sender)
            updater.addSub(gui)
            sender.addSub(gui)
            updater.start()
            sender.start()
            toDispose.dispose()
        }
        layout.setConstraints(submit, c)
        add(submit)

        pack()
        isVisible = true
    }
}