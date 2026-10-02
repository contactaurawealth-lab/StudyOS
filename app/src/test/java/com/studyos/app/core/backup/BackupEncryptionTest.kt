package com.studyos.app.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupEncryptionTest {

    @Test
    fun testAes256GcmRoundTripEncryption() {
        val payload = "{\"app\":\"StudyOS\",\"version\":1,\"data\":{\"students\":[{\"id\":\"1\",\"name\":\"Alex\"}]}}"
        val passphrase = "test_studyos_passphrase"
        val salt = ByteArray(16) { 0x42.toByte() }
        val iv = ByteArray(12) { 0x13.toByte() }

        // 1. Key Derivation
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 1000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        // 2. Encryption
        val encCipher = Cipher.getInstance("AES/GCM/NoPadding")
        encCipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = encCipher.doFinal(payload.toByteArray(Charsets.UTF_8))

        // 3. Decryption
        val decCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decCipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val decryptedBytes = decCipher.doFinal(ciphertext)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)

        assertEquals(payload, decryptedText)
    }

    @Test(expected = javax.crypto.AEADBadTagException::class)
    fun testAes256GcmTamperDetectionFails() {
        val payload = "{\"app\":\"StudyOS\",\"secret\":\"offline\"}"
        val passphrase = "test_passphrase"
        val salt = ByteArray(16) { 0x11.toByte() }
        val iv = ByteArray(12) { 0x22.toByte() }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 1000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val encCipher = Cipher.getInstance("AES/GCM/NoPadding")
        encCipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = encCipher.doFinal(payload.toByteArray(Charsets.UTF_8))

        // Tamper with one single byte
        ciphertext[ciphertext.size / 2] = (ciphertext[ciphertext.size / 2].toInt() xor 0xFF).toByte()

        val decCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decCipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        // Must throw AEADBadTagException because GCM authentication tag validation fails!
        decCipher.doFinal(ciphertext)
    }
}
