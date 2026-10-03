package password_manager_final

import java.io.File
import java.io.IOException
import javax.crypto.spec.SecretKeySpec
import kotlin.uuid.Uuid

//data classes
data class encryptionCredential(
    val id: String = Uuid.random().toString(),
    val serviceName: String,
    val username: String,
    val iv: String,
    val ciphertext: String
)
data class DecryptedAccountView(
    val id: String,
    val serviceName: String,
    val username: String,
    val decryptedPassword: String
)

//encrypton file handling
class EncryptedFileVault(private val storageFile: File, private val masterKey: SecretKeySpec) {
    private val credentialsTable = mutableListOf<encryptionCredential>()

    init {
        loadFromFile()
    }

    fun addAccount(serviceName: String, username: String, plainTextPassword: String) {
        val (iv, ciphertext) = passwordcryto.encrypt(plainTextPassword, masterKey)
        val newRecord = encryptionCredential(
            serviceName = serviceName,
            username = username,
            iv = iv,
            ciphertext = ciphertext
        )
        credentialsTable.add(newRecord)
        saveToFile()
    }

    fun getDecryptedList(): List<DecryptedAccountView> {
        return credentialsTable.map { record ->
            val plainTextPassword = passwordcryto.decrypt(record.ciphertext, record.iv, masterKey)
            DecryptedAccountView(record.id, record.serviceName, record.username, plainTextPassword)
        }
    }
//file handling
    private fun saveToFile() {
        val fileContent = StringBuilder()
        for (item in credentialsTable) {
            val safeService = item.serviceName.replace(",", "\\,")
            val safeUser = item.username.replace(",", "\\,")
            fileContent.append("${item.id},$safeService,$safeUser,${item.iv},${item.ciphertext}\n")
        }
        try {
            storageFile.writeText(fileContent.toString(), Charsets.UTF_8)
        } catch (e: IOException) {
            println("❌ Storage Write Error: ${e.message}")
        }
    }

    private fun loadFromFile() {
        if (!storageFile.exists()) return
        try {
            val lines = storageFile.readLines(Charsets.UTF_8)
            credentialsTable.clear()
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split(",")
                if (parts.size >= 5) {
                    credentialsTable.add(
                        encryptionCredential(
                            id = parts[0],
                            serviceName = parts[1].replace("\\,", ","),
                            username = parts[2].replace("\\,", ","),
                            iv = parts[3],
                            ciphertext = parts[4]
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("❌ Storage Read Error: ${e.message}")
        }
    }

    //search function
    fun searchList(query: String): List<DecryptedAccountView> {
        val fullList = getDecryptedList()
        if (query.isBlank()) return fullList

        return fullList.filter { account ->
            account.serviceName.contains(query, ignoreCase = true) ||
                    account.username.contains(query, ignoreCase = true)
        }
    }
}
// test runner

/**
fun main() {
println("🧪 --- STEP 3 TESTING: INTEGRATED STORAGE LAYER ---")

val mockVaultFile = File("./vault_test_run.txt")
val salt = passwordcryto.generateSalt()
val derivedKey = passwordcryto.deriveKey("vaultpass".toCharArray(), salt)

// Test 1: Initialize Vault and Write Data
val writeVault = EncryptedFileVault(mockVaultFile, derivedKey)
writeVault.addAccount("TestService", "test_user", "SecretPass99!")
println("💾 Record committed to mockVaultFile structure loop.")

// Test 2: Read Data Back from New Context Instance
val readVault = EncryptedFileVault(mockVaultFile, derivedKey)
val readList = readVault.getDecryptedList()

if (readList.isNotEmpty() && readList[0].decryptedPassword == "SecretPass99!") {
println("✅ SUCCESS: File rehydrated, verified, and correctly decrypted from disk!")
} else {
println("❌ FAILURE: File could not be parsed or read correctly.")
}

// Cleanup temporary mock asset files
mockVaultFile.delete()
}*/
