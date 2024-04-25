package org.ProToType.Static;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Encryption {
    private static final Logger logger = LogManager.getLogger(Encryption.class);

    private static final byte ivLength = 16;
    public static byte[] encryptionKey;
    public static boolean encryptionEnabled = true;

    public static void SetEncryptionKey(String encryptionKeyString) {
        encryptionKey = encryptionKeyString.getBytes(StandardCharsets.UTF_8);
    }

    public static String Decrypt(byte[] encryptedMessageWithIV) {
        try {
            // reads the first 16 bytes as IV
            byte[] extractedIV = new byte[ivLength];
            System.arraycopy(encryptedMessageWithIV, 0, extractedIV, 0, ivLength);

            // reads the rest of the byte array as message
            byte[] encryptedMessage = new byte[encryptedMessageWithIV.length - ivLength];
            System.arraycopy(encryptedMessageWithIV, ivLength, encryptedMessage, 0, encryptedMessage.length);

            // decrypt using IV and key
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(extractedIV));
            byte[] decryptedBytes = cipher.doFinal(encryptedMessage);

            String decryptedMessage = new String(decryptedBytes, StandardCharsets.UTF_8);
            logger.trace("Decrpyted message: {}", decryptedMessage.trim());

            // return as string
            return decryptedMessage;
        } catch (Exception e) {
            logger.error("Decryption of a received message failed: {}", e.toString());
            return null;
        }
    }

    public static byte[] Encrypt(String messageString) {
        try {
            logger.trace("Encrypting message ...");
            // creates a random IV byte array
            byte[] randomIV = new byte[ivLength];
            new SecureRandom().nextBytes(randomIV);

            // encrypt the string message
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(randomIV));
            byte[] encryptedBytes = cipher.doFinal(messageString.getBytes());

            // combines the IV byte array and the encrypted message byte array, the IV array is first, message is second
            byte[] encryptedBytesWithIV = new byte[encryptedBytes.length + ivLength];
            System.arraycopy(randomIV, 0, encryptedBytesWithIV, 0, ivLength);
            System.arraycopy(encryptedBytes, 0, encryptedBytesWithIV, ivLength, encryptedBytes.length);

            logger.trace("Encryption of message {} was success, byte length: {}", messageString, encryptedBytesWithIV.length);

            // return the encrypted message as byte array
            return encryptedBytesWithIV;
        } catch (Exception e) {
            logger.error("Encryption of a message {} failed: {}", messageString, e.toString());
            return null;
        }
    }
}
