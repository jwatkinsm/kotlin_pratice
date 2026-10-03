package password_manager_final

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

private class PasswordManagerApp : JFrame("Secure PassManager Suite") {
    private val vaultFile = File("./vault_secure.txt")
    private val saltFile = File("./vault_salt.txt")
    private var viewModel: VaultViewModel? = null
    private var revealedAccountId: String? = null
    private var visibleAccounts: List<DecryptedAccountView> = emptyList()
    private lateinit var accountSearchField: JTextField
    private val accountTableModel = object : DefaultTableModel(
        arrayOf("Service", "Username", "Password"),
        0
    ) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val accountTable = JTable(accountTableModel)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(600, 450)
        setSize(750, 600)
        setLocationRelativeTo(null)
        showLockedScreen()
    }

    //lock screen
    private fun showLockedScreen(message: String = "") {
        val passwordField = JPasswordField(24)
        val panel = JPanel(GridLayout(0, 1, 8, 8)).apply {
            border = BorderFactory.createEmptyBorder(32, 48, 32, 48)
            add(JLabel("Vault is Locked"))
            add(JLabel("Master Password"))
            add(passwordField)
            if (message.isNotBlank()) add(JLabel(message))
            add(JButton("Unlock Vault Engine").apply {
                addActionListener { unlock(passwordField.password) }
            })
        }
        contentPane = panel
        revalidate()
        repaint()
    }

    //checkfor master password
    private fun unlock(password: CharArray) {
        try {
            val salt = if (saltFile.exists()) {
                saltFile.readBytes()
            } else {
                passwordcryto.generateSalt().also(saltFile::writeBytes)
            }
            val key = passwordcryto.deriveKey(password, salt)
            val unlockedVault = EncryptedFileVault(vaultFile, key)
            unlockedVault.getDecryptedList()
            viewModel = VaultViewModel(unlockedVault)
            revealedAccountId = null
            showDashboard()
        } catch (exception: Exception) {
            showLockedScreen("Could not unlock vault: ${exception.message ?: exception.javaClass.simpleName}")
        } finally {
            password.fill('\u0000')
        }
    }

    //password dashboard
    private fun showDashboard() {
        val serviceField = JTextField()
        val usernameField = JTextField()
        val passwordField = JPasswordField()
        accountSearchField = JTextField(24)

        val entryForm = JPanel(GridLayout(0, 2, 8, 8)).apply {
            border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
            add(JLabel("Service"))
            add(serviceField)
            add(JLabel("Username"))
            add(usernameField)
            add(JLabel("Password"))
            add(passwordField)
            add(JLabel())
            add(JButton("Encrypt & Save").apply {
                addActionListener {
                    try {
                        viewModel?.saveEntry(
                            serviceField.text,
                            usernameField.text,
                            String(passwordField.password)
                        )
                        serviceField.text = ""
                        usernameField.text = ""
                        passwordField.text = ""
                        refreshAccounts()
                    } catch (exception: Exception) {
                        showError(exception)
                    }
                }
            })
        }

        accountTable.autoCreateRowSorter = true
        val searchPanel = JPanel(FlowLayout(FlowLayout.LEADING)).apply {
            add(JLabel("Search service or username"))
            add(accountSearchField)
            add(JButton("Search").apply {
                addActionListener { refreshAccounts() }
            })
            add(JButton("Clear").apply {
                addActionListener {
                    accountSearchField.text = ""
                    refreshAccounts()
                }
            })
        }
        val actions = JPanel().apply {
            add(JButton("Reveal / Hide Selected").apply {
                addActionListener { toggleSelectedPassword() }
            })
            add(JButton("Lock Vault").apply {
                addActionListener {
                    viewModel = null
                    revealedAccountId = null
                    accountTableModel.rowCount = 0
                    showLockedScreen()
                }
            })
        }

        contentPane = JPanel(BorderLayout()).apply {
            add(entryForm, BorderLayout.NORTH)
            add(JPanel(BorderLayout()).apply {
                add(searchPanel, BorderLayout.NORTH)
                add(JScrollPane(accountTable), BorderLayout.CENTER)
            }, BorderLayout.CENTER)
            add(actions, BorderLayout.SOUTH)
        }
        refreshAccounts()
        revalidate()
        repaint()
    }

    //password reveal
    private fun toggleSelectedPassword() {
        val row = accountTable.selectedRow
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an account first.", "No account selected", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val account = visibleAccounts.getOrNull(accountTable.convertRowIndexToModel(row)) ?: return
        revealedAccountId = if (revealedAccountId == account.id) null else account.id
        refreshAccounts()
        accountTable.setRowSelectionInterval(row, row)
    }

    private fun refreshAccounts() {
        accountTableModel.rowCount = 0
        visibleAccounts = viewModel?.searchAccounts(accountSearchField.text) ?: emptyList()
        visibleAccounts.forEach { account ->
            val password = if (account.id == revealedAccountId) account.decryptedPassword else "••••••••"
            accountTableModel.addRow(arrayOf(account.serviceName, account.username, password))
        }
    }

    private fun showError(exception: Exception) {
        JOptionPane.showMessageDialog(
            this,
            "Could not save credential: ${exception.message ?: exception.javaClass.simpleName}",
            "Vault Error",
            JOptionPane.ERROR_MESSAGE
        )
    }
}

private class VaultViewModel(private val vault: EncryptedFileVault) {
    fun searchAccounts(query: String): List<DecryptedAccountView> = vault.searchList(query)

    fun saveEntry(service: String, username: String, password: String) {
        require(service.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
            "Enter a service, username, and password."
        }
        vault.addAccount(service, username, password)
    }
}

fun main() {
    SwingUtilities.invokeLater {
        PasswordManagerApp().isVisible = true
    }
}
