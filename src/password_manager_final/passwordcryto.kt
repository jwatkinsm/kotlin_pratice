package password_manager_final

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object passwordcryto {
    private const val AES_ALGO = "AES/GCM/NoPadding"
    private const val KDF_ALGO = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 600_000
    private const val KEY_LENGTH = 256

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KDF_ALGO)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encrypt(plainText: String, key: SecretKeySpec): Pair<String, String> {
        val cipher = Cipher.getInstance(AES_ALGO)
        val ivBytes = ByteArray(12)
        SecureRandom().nextBytes(ivBytes)

        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, ivBytes))
        val cipherBytes = cipher.doFinal(plainText.encodeToByteArray())

        return Pair(
            Base64.getEncoder().encodeToString(ivBytes),
            Base64.getEncoder().encodeToString(cipherBytes)
        )
    }

    fun decrypt(ciphertext: String, iv: String, key: SecretKeySpec): String {
        val cipher = Cipher.getInstance(AES_ALGO)
        val ivBytes = Base64.getDecoder().decode(iv)
        val cipherBytes = Base64.getDecoder().decode(ciphertext)

        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, ivBytes))
        return cipher.doFinal(cipherBytes).decodeToString()
    }
}
/**
// testrunner
fun main() {
    println("🧪 --- TESTING: CRYPTO ISOLATION ---")

    val testPassword = "MyTestMasterPassword123!".toCharArray()
    val testSalt = passwordcryto.generateSalt()

    // Test 1: Key Derivation
    val derivedKey = passwordcryto.deriveKey(testPassword, testSalt)
    println("🔑 Derived Key Specification generated successfully.")

    // Test 2: Encrypt Processing Matrix
    val plainSecret = "SuperSecretToken2026$"
    val (iv, ciphertext) = passwordcryto.encrypt(plainSecret, derivedKey)
    println("🔒 Plaintext successfully transformed. Ciphertext: $ciphertext")

    // Test 3: Decrypt Processing Matrix
    val decryptedResult = passwordcryto.decrypt(ciphertext, iv, derivedKey)
    if (decryptedResult == plainSecret) {
        println("✅ SUCCESS: Decrypted string matches original input exactly!")
    } else {
        println("❌ FAILURE: String corruption detected.")
    }
}*/