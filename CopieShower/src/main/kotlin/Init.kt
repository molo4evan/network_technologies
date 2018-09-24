import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.net.InetAddress
import javax.swing.*

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

        val mult_addr = JLabel("Multicast group:")
        layout.setConstraints(mult_addr, c)
        add(mult_addr)

        val ins_mult_addr = JTextField(15)
        layout.setConstraints(ins_mult_addr, c)
        add(ins_mult_addr)

        val port = JLabel("Multicast port:")
        layout.setConstraints(port, c)
        add(port)

        val ins_port = JTextField(7)
        layout.setConstraints(ins_port, c)
        add(ins_port)

        val ins_your_port = JTextField(7)
        ins_your_port.isEnabled = false

        val use_user_port = JCheckBox("Use custom send port")
        use_user_port.addItemListener { ins_your_port.isEnabled = use_user_port.isSelected }
        layout.setConstraints(use_user_port, c)
        add(use_user_port)

        layout.setConstraints(ins_your_port, c)
        add(ins_your_port)

        val toDispose = this
        val submit = JButton("Submit")
        submit.addActionListener {
            val group = ins_mult_addr.text
            val remote_port = ins_port.text
            val my_port = ins_your_port.text
            val updater = Receiver(InetAddress.getByName(group), remote_port.toInt())
            val sender = Sender(InetAddress.getByName(group), remote_port.toInt(), my_port)
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