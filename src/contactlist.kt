import java.io.File
import java.io.IOException
import kotlin.uuid.Uuid

data class Contact(
    val id: String = Uuid.random().toString(),
    val name: String,
    val email: String
)

class ContactNotebook(
    private val storageFile: File,
    private val keyFile: File = File(storageFile.parentFile, "${storageFile.name}.key")
) {
    // Mutable Collection Contacts
    private val contacts = mutableListOf<Contact>()
    private val encryptionKey = MilestoneCryptoEngine.loadOrCreateKey(keyFile)

        init {
            loadFromFile()
        }
    /**
     * Adds new contact data class to list collection.
     */
    fun addContact(name: String, email: String) {
        val newContact = Contact(name = name, email = email)
        contacts.add(newContact)
        saveToFile()
        println("✅ Successfully added contact: $name")
    }
        fun getAllContacts(): List<Contact> = contacts

        fun searchContacts(query: String): List<Contact> {
                return contacts.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.email.contains(query, ignoreCase = true)
                }
            }

            /**
             * Serializes our structures into basic CSV lines and writes them out.
             * Note: In a larger app, you would swap this for JSON frameworks like kotlinx.serialization.
             */
            private fun saveToFile() {
                val parent = storageFile.parentFile
                if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
                    throw IOException("Unable to create storage directory: ${parent.path}")
                }

                val fileContent = contacts.joinToString(
                    separator = "\n",
                    postfix = if (contacts.isEmpty()) "" else "\n"
                ) { contact ->
                    val encryptedName = MilestoneCryptoEngine.encrypt(contact.name, encryptionKey)
                    val encryptedEmail = MilestoneCryptoEngine.encrypt(contact.email, encryptionKey)
                    listOf(
                        contact.id,
                        encryptedName.iv,
                        encryptedName.ciphertext,
                        encryptedEmail.iv,
                        encryptedEmail.ciphertext
                    ).joinToString(",")
                }
                storageFile.writeText(fileContent, Charsets.UTF_8)
                println("✅ Successfully saved encrypted contacts to disk storage.")
            }
        /**
         * Checks if a file exists, reads lines, and recreates our data classes.
         */
        private fun loadFromFile() {
            if (!storageFile.exists()) {
                println("ℹ️ No previous save file discovered. Starting fresh.")
                return
            }

            val lines = storageFile.readLines(Charsets.UTF_8)
            var foundLegacyRecords = false
            for ((lineIndex, line) in lines.withIndex()) {
                if (line.isBlank()) continue
                val parts = splitEscapedFields(line)
                when (parts.size) {
                    3 -> {
                        contacts.add(Contact(parts[0], parts[1], parts[2]))
                        foundLegacyRecords = true
                    }
                    5 -> {
                        val name = MilestoneCryptoEngine.decrypt(
                            EncryptedPayload(parts[1], parts[2]),
                            encryptionKey
                        )
                        val email = MilestoneCryptoEngine.decrypt(
                            EncryptedPayload(parts[3], parts[4]),
                            encryptionKey
                        )
                        contacts.add(Contact(parts[0], name, email))
                    }
                    else -> throw IOException("Invalid contact record at line ${lineIndex + 1}")
                }
            }
            if (foundLegacyRecords) saveToFile()
            println("💾 Successfully loaded ${contacts.size} records from local disk.")
        }

        private fun splitEscapedFields(line: String): List<String> {
            val fields = mutableListOf<String>()
            val field = StringBuilder()
            var index = 0
            while (index < line.length) {
                when (val character = line[index]) {
                    '\\' -> {
                        if (index + 1 < line.length && (line[index + 1] == '\\' || line[index + 1] == ',')) {
                            field.append(line[++index])
                        } else {
                            field.append(character)
                        }
                    }
                    ',' -> {
                        fields.add(field.toString())
                        field.setLength(0)
                    }
                    else -> field.append(character)
                }
                index++
            }
            fields.add(field.toString())
            return fields
        }
    }
fun main() {
    val datafile = File("contacts.txt")
    val notebook = ContactNotebook(datafile)

    // Seed contact list
    notebook.addContact("Alice Developer", "alice@kotlin.org")
    notebook.addContact("Bob Cryptographer", "bob@security.net")
    notebook.addContact("Charlie Manager", "charlie@company.com")

    var running = true

    while (running) {
        println("\n=== 📓 MILESTONE 1: LOCAL CONTACT NOTEBOOK ===")
        println("1. View All Contacts")
        println("2. Add New Contact")
        println("3. Search Contacts")
        println("4. Exit Application")
        print("Select an option: ")

        when (readLine()?.trim()) {
            "1" -> {
                println("\n📋 ALL CONTACTS:")
                printTable(notebook.getAllContacts())
            }
            "2" -> {
                print("Enter Name: ")
                val name = readln()?.trim().orEmpty()
                print("Enter Email: ")
                val email = readln()?.trim().orEmpty()

                if (name.isNotBlank() && email.isNotBlank()) {
                    notebook.addContact(name, email)
                } else {
                    println("❌ Error: Name and Email cannot be empty!")
                }
            }
            "3" -> {
                print("Enter search term (name or email): ")
                val query = readln()?.trim().orEmpty()
                val results = notebook.searchContacts(query)
                println("\n🔍 SEARCH RESULTS FOR \"$query\":")
                printTable(results)
            }
            "4" -> {
                println("Closing application. Milestone 1 Complete!")
                running = false
            }
            else -> {
                println("⚠️ Invalid option, please choose between 1 and 4.")
            }
        }
    }
}
//table format for terminal1
fun printTable(contactsList: List<Contact>) {
    if (contactsList.isEmpty()) {
        println("   (No contact records discovered)")
        return
    }
    println("--------------------------------------------------------------------------------")
    System.out.printf("%-38s | %-20s | %-25s\n", "UNIQUE ID", "NAME", "EMAIL")
    println("--------------------------------------------------------------------------------")
    for (contact in contactsList) {
        System.out.printf("%-38s | %-20s | %-25s\n", contact.id, contact.name, contact.email)
    }
    println("--------------------------------------------------------------------------------")
}