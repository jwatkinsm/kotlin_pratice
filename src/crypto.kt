import java.io.File
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
//data class
data class EncryptedPayload(
    val iv: String,
    val ciphertext: String
)

//cryptographic
object MilestoneCryptoEngine{
    private const val AlGO= "AES/GCM/NoPadding"

    fun generateKey(): SecretKey{
        val keyGen= KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun loadOrCreateKey(keyFile: File): SecretKey {
        if (keyFile.exists()) {
            val keyBytes = Base64.getDecoder().decode(keyFile.readText(Charsets.UTF_8).trim())
            if (keyBytes.size != 32) {
                throw IOException("Invalid AES key length in ${keyFile.path}")
            }
            return SecretKeySpec(keyBytes, "AES")
        }

        val parent = keyFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
            throw IOException("Unable to create key directory: ${parent.path}")
        }

        val key = generateKey()
        keyFile.writeText(Base64.getEncoder().encodeToString(key.encoded), Charsets.UTF_8)
        return key
    }

    fun encrypt(plainText: String, key: SecretKey): EncryptedPayload{
        val cipher= Cipher.getInstance(AlGO)

        val ivBytes= ByteArray(12)
        SecureRandom().nextBytes(ivBytes)

        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, ivBytes))
        val cipherBytes= cipher.doFinal(plainText.encodeToByteArray())

        return EncryptedPayload(
            iv = Base64.getEncoder().encodeToString(ivBytes),
            ciphertext = Base64.getEncoder().encodeToString(cipherBytes)
        )
    }

    fun decrypt(payload: EncryptedPayload, key: SecretKey): String{
        val cipher= Cipher.getInstance(AlGO)
        val ivBytes= Base64.getDecoder().decode(payload.iv)
        val cipherBytes= Base64.getDecoder().decode(payload.ciphertext)

        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, ivBytes))
        return cipher.doFinal(cipherBytes).decodeToString()
    }
}