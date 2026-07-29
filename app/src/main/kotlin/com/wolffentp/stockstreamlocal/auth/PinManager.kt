package com.wolffentp.stockstreamlocal.auth

import com.wolffentp.stockstreamlocal.security.SecureStorage
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val ITERATIONS = 200_000
private const val KEY_LENGTH_BITS = 256
private const val SALT_BYTES = 32

/**
 * Manages local device PIN for app access.
 *
 * Security design:
 * - PIN is hashed with PBKDF2-SHA256 (200,000 iterations) before storage.
 * - A random 32-byte salt is generated per PIN set.
 * - Hash and salt are stored in Android Keystore-backed [SecureStorage].
 * - The raw PIN is NEVER stored, logged, or transmitted.
 */
@Singleton
class PinManager @Inject constructor(
    private val secureStorage: SecureStorage,
) {
    fun isPinSet(): Boolean = secureStorage.isPinSet()

    /**
     * Saves a new PIN. The raw PIN is cleared from memory after hashing.
     * @param pin Raw PIN digits (must not be logged)
     */
    fun setPin(pin: CharArray) {
        val salt = generateSalt()
        val hash = hash(pin, salt)
        pin.fill('0') // clear raw PIN from memory
        secureStorage.savePinCredentials(hash, salt)
    }

    /**
     * Verifies a PIN attempt.
     * @param attempt Raw PIN digits (must not be logged)
     * @return true if the PIN matches, false otherwise
     */
    fun verifyPin(attempt: CharArray): Boolean {
        val storedHash = secureStorage.getPinHash() ?: return false
        val salt = secureStorage.getPinSalt() ?: return false
        val attemptHash = hash(attempt, salt)
        attempt.fill('0') // clear raw PIN from memory
        return storedHash.contentEquals(attemptHash)
    }

    fun clearPin() = secureStorage.clearPin()

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun hash(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
