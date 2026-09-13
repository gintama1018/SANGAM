package com.frameroom.app.core

import java.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    data class SessionKeyPair(
        val publicKeyBase64: String,
        val privateKeyBase64: String
    )

    /**
     * Generates an ephemeral key pair for session establishment.
     * Uses X25519 if available on the runtime, or EC secp256r1 as standard fallback.
     */
    fun generateKeyPair(): SessionKeyPair {
        val keyPair = try {
            val kpg = KeyPairGenerator.getInstance("X25519")
            kpg.generateKeyPair()
        } catch (e: Exception) {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            kpg.generateKeyPair()
        }

        return SessionKeyPair(
            publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded),
            privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        )
    }

    /**
     * Derives a shared symmetric AES-256 key from a local private key and remote public key.
     */
    fun deriveSharedSecret(myPrivateKeyB64: String, peerPublicKeyB64: String): SecretKey {
        val myPrivateBytes = Base64.getDecoder().decode(myPrivateKeyB64)
        val peerPublicBytes = Base64.getDecoder().decode(peerPublicKeyB64)

        val (privateKey, publicKey) = try {
            val kf = KeyFactory.getInstance("X25519")
            val priv = kf.generatePrivate(PKCS8EncodedKeySpec(myPrivateBytes))
            val pub = kf.generatePublic(X509EncodedKeySpec(peerPublicBytes))
            Pair(priv, pub)
        } catch (e: Exception) {
            val kf = KeyFactory.getInstance("EC")
            val priv = kf.generatePrivate(PKCS8EncodedKeySpec(myPrivateBytes))
            val pub = kf.generatePublic(X509EncodedKeySpec(peerPublicBytes))
            Pair(priv, pub)
        }

        val agreement = try {
            KeyAgreement.getInstance("X25519")
        } catch (e: Exception) {
            KeyAgreement.getInstance("ECDH")
        }
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val rawSharedSecret = agreement.generateSecret()

        // Derive 256-bit AES key via SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = digest.digest(rawSharedSecret)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    /**
     * Encrypts payload with AES-GCM.
     */
    fun encrypt(data: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val ciphertext = cipher.doFinal(data)

        // Prepend IV to ciphertext
        val output = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, output, 0, iv.size)
        System.arraycopy(ciphertext, 0, output, iv.size, ciphertext.size)
        return output
    }

    /**
     * Decrypts payload with AES-GCM.
     */
    fun decrypt(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
        require(encryptedData.size > GCM_IV_LENGTH) { "Invalid encrypted payload" }
        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH)
        val ciphertext = ByteArray(encryptedData.size - GCM_IV_LENGTH)
        System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(ciphertext)
    }
}
