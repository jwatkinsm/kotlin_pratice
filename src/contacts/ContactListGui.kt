package contacts

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

class ContactListGui(private val notebook: ContactNotebook) {
    private val tableModel = object : DefaultTableModel(
        arrayOf("ID", "Name", "Email"),
        0
    ) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val table = JTable(tableModel)
    private val nameField = JTextField(18)
    private val emailField = JTextField(18)
    private val searchField = JTextField(22)

    fun show() {
        check(SwingUtilities.isEventDispatchThread()) { "GUI must be started on the Swing event thread." }

        val frame = JFrame("contacts.Contact Notebook")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.minimumSize = Dimension(700, 420)
        frame.setLocationRelativeTo(null)

        val content = JPanel(BorderLayout(12, 12))
        content.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)

        val addPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = GridBagConstraints.WEST
        }
        constraints.gridx = 0
        constraints.gridy = 0
        addPanel.add(JLabel("Name"), constraints)
        constraints.gridx = 1
        addPanel.add(nameField, constraints)
        constraints.gridx = 2
        addPanel.add(JLabel("Email"), constraints)
        constraints.gridx = 3
        addPanel.add(emailField, constraints)
        constraints.gridx = 4
        addPanel.add(JButton("Add contacts.Contact").apply { addActionListener { addContact() } }, constraints)

        val searchPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        searchPanel.add(JLabel("Search"))
        searchPanel.add(searchField)
        searchPanel.add(JButton("Search").apply { addActionListener { refreshTable() } })
        searchPanel.add(JButton("Show All").apply {
            addActionListener {
                searchField.text = ""
                refreshTable()
            }
        })

        val controls = JPanel(BorderLayout(0, 8))
        controls.add(addPanel, BorderLayout.NORTH)
        controls.add(searchPanel, BorderLayout.SOUTH)

        table.autoCreateRowSorter = true
        content.add(controls, BorderLayout.NORTH)
        content.add(JScrollPane(table), BorderLayout.CENTER)
        frame.contentPane = content
        refreshTable()
        frame.isVisible = true
    }

    private fun addContact() {
        val name = nameField.text.trim()
        val email = emailField.text.trim()
        if (name.isBlank() || email.isBlank()) {
            JOptionPane.showMessageDialog(
                table,
                "Enter both a name and an email address.",
                "Missing contact details",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        try {
            notebook.addContact(name, email)
            nameField.text = ""
            emailField.text = ""
            refreshTable()
        } catch (exception: Exception) {
            showError("Could not save contact", exception)
        }
    }

    private fun refreshTable() {
        try {
            val query = searchField.text.trim()
            val contacts = if (query.isEmpty()) {
                notebook.getAllContacts()
            } else {
                notebook.searchContacts(query)
            }
            tableModel.rowCount = 0
            contacts.forEach { contact ->
                tableModel.addRow(arrayOf(contact.id, contact.name, contact.email))
            }
        } catch (exception: Exception) {
            showError("Could not load contacts", exception)
        }
    }

    private fun showError(action: String, exception: Exception) {
        JOptionPane.showMessageDialog(
            table,
            "$action: ${exception.message ?: exception.javaClass.simpleName}",
            "contacts.Contact Notebook Error",
            JOptionPane.ERROR_MESSAGE
        )
    }
}
