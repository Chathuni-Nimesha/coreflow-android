package com.chathuninimesha.coreflow.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2 password hashing for offline local storage.
 * Never logs or returns the plaintext password.
 */
class PasswordHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom()
) {

    fun hash(password: CharArray): String {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        val algorithm = availableAlgorithm()
        val hash = derive(password, salt, iterations, algorithm)
        return "$algorithm$$iterations$${salt.toHex()}$${hash.toHex()}"
    }

    fun verify(password: CharArray, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4) return false
        val algorithm = parts[0]
        val storedIterations = parts[1].toIntOrNull() ?: return false
        val salt = parts[2].hexToBytes() ?: return false
        val expected = parts[3].hexToBytes() ?: return false
        if (salt.isEmpty() || expected.isEmpty() || storedIterations <= 0) return false
        val actual = derive(password, salt, storedIterations, algorithm)
        return MessageDigest.isEqual(actual, expected)
    }

    fun isEncodedHash(value: String): Boolean {
        val parts = value.split("$")
        if (parts.size != 4) return false
        if (parts[0] != ALG_SHA256 && parts[0] != ALG_SHA1) return false
        return parts[1].toIntOrNull() != null &&
            parts[2].hexToBytes() != null &&
            parts[3].hexToBytes() != null
    }

    private fun derive(
        password: CharArray,
        salt: ByteArray,
        iter: Int,
        algorithm: String
    ): ByteArray {
        val spec = PBEKeySpec(password, salt, iter, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun availableAlgorithm(): String {
        return try {
            SecretKeyFactory.getInstance(ALG_SHA256)
            ALG_SHA256
        } catch (_: Exception) {
            ALG_SHA1
        }
    }

    companion object {
        const val DEFAULT_ITERATIONS = 120_000
        private const val SALT_BYTES = 16
        private const val KEY_LENGTH_BITS = 256
        private const val ALG_SHA256 = "PBKDF2WithHmacSHA256"
        private const val ALG_SHA1 = "PBKDF2WithHmacSHA1"
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun String.hexToBytes(): ByteArray? {
    if (length % 2 != 0 || isEmpty()) return null
    return try {
        ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    } catch (_: NumberFormatException) {
        null
    }
}
