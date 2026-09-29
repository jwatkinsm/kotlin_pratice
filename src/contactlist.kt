import java.io.File
import java.io.IOException
import kotlin.uuid.Uuid

data class Contact(
    val id: String = Uuid.random().toString(),
    val name: String,
    val email: String
)

    class ContactNotebook(private val storageFile: File) {
    // Mutable Collection Contacts
    private val contacts = mutableListOf<Contact>()
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
                val fileContent = StringBuilder()
                for (contact in contacts) {
                    val safeName = contact.name.replace(",", "\\,")
                    val safeEmail = contact.email.replace(",", "\\,")
                    fileContent.append("${contact.id},$safeName,$safeEmail\n")
                }

                try {
                    storageFile.writeText(fileContent.toString(), Charsets.UTF_8)
                    println("✅ Successfully saved updates to disk storage.")
                } catch (e: IOException) {
                    println("❌ Critical File Error: Unable to save data. Reason: ${e.message}")
                }
            }
        /**
         * Checks if a file exists, reads lines, and recreates our data classes.
         */
        private fun loadFromFile() {
            if (!storageFile.exists()) {
                println("ℹ️ No previous save file discovered. Starting fresh.")
                return
            }

            try {
                val lines = storageFile.readLines(Charsets.UTF_8)
                contacts.clear()

                for (line in lines) {
                    if (line.isBlank()) continue
                    val parts = line.split(",")
                    if (parts.size >= 3) {
                        val id = parts[0]
                        val name = parts[1].replace("\\,", ",")
                        val email = parts[2].replace("\\,", ",")
                        contacts.add(Contact(id, name, email))
                    }
                }
                println("💾 Successfully loaded ${contacts.size} records from local disk.")
            } catch (e: IOException) {
                println("❌ Critical File Error: Could not read the database file. Reason: ${e.message}")
            } catch (e: Exception) {
                println("⚠️ Data Error: Save file structure is corrupted. Resetting notebook buffer.")
            }
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