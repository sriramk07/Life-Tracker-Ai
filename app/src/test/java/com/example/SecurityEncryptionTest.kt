package com.example

import com.example.security.SecurityManager
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.AEADBadTagException

/**
 * ============================================================================
 * UNIT TESTS: SECURITY ENCRYPTION, SALTED PIN HASHING & INTEGRITY DIGESTS
 * ============================================================================
 */
class SecurityEncryptionTest {

    @Test
    fun pinHashing_verifiesMatchingPinAndRejectsWrongPin() {
        val salt = SecurityManager.generateSaltHex()
        assertEquals(32, salt.length)

        val hash = SecurityManager.hashPin("2468", salt)
        assertEquals(64, hash.length) // SHA-256 hex = 64 chars

        assertTrue(SecurityManager.verifyPin("2468", hash, salt))
        assertFalse(SecurityManager.verifyPin("0000", hash, salt))
    }

    @Test
    fun sha256Integrity_detectsPayloadModification() {
        val payload = "{\"tasks\":3,\"exportedAt\":1700000}"
        val checksum1 = SecurityManager.computeSha256Integrity(payload)
        val checksum2 = SecurityManager.computeSha256Integrity(payload)
        val tampered = SecurityManager.computeSha256Integrity("{\"tasks\":999,\"exportedAt\":1700000}")

        assertEquals(checksum1, checksum2)
        assertNotEquals(checksum1, tampered)
    }

    @Test
    fun aesGcmPassphraseEncryption_roundTripsCleanly() {
        val secret = "Private LifeOS Journal: Achieving 100% completion!"
        val passphrase = "MonarchVaultPassword!"

        val encrypted = SecurityManager.encryptWithPassphrase(secret, passphrase)
        assertNotEquals(secret, encrypted)
        assertTrue(encrypted.contains(":"))

        val decrypted = SecurityManager.decryptWithPassphrase(encrypted, passphrase)
        assertEquals(secret, decrypted)
    }

    @Test(expected = AEADBadTagException::class)
    fun aesGcmPassphraseDecryption_failsWithWrongPassphrase() {
        val encrypted = SecurityManager.encryptWithPassphrase("Sensitive Data", "CorrectPass")
        SecurityManager.decryptWithPassphrase(encrypted, "WrongPass")
    }
}
