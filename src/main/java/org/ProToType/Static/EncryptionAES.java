package org.ProToType.Static;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class EncryptionAES {
    private static final Logger logger = LogManager.getLogger(EncryptionAES.class);

    private static final byte keyLength = 16;
    public static boolean encryptionEnabled = true;

    private static Cipher cipher;

    public static void Initialize() throws NoSuchPaddingException, NoSuchAlgorithmException {
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    }

    public static byte[] GenerateRandomKey() {
        byte[] randomKey = new byte[keyLength];
        new SecureRandom().nextBytes(randomKey);
        return randomKey;
    }

    public static String DecryptString(byte[] encryptedMessageWithIV, byte[] encryptionKey) {
        byte[] bytes = Decrypt(encryptedMessageWithIV, encryptionKey);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] Encrypt(byte[] messageBytes, byte[] encryptionKey) {
        try {
            // creates a random IV byte array
            byte[] randomIV = new byte[keyLength];
            new SecureRandom().nextBytes(randomIV);

            // encrypt the  message
            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(randomIV));
            byte[] encryptedBytes = cipher.doFinal(messageBytes);

            // combines the IV byte array and the encrypted message byte array, the IV array is first, message is second
            byte[] encryptedBytesWithIV = new byte[encryptedBytes.length + keyLength];
            System.arraycopy(randomIV, 0, encryptedBytesWithIV, 0, keyLength);
            System.arraycopy(encryptedBytes, 0, encryptedBytesWithIV, keyLength, encryptedBytes.length);

            // return the encrypted message as byte array
            return encryptedBytesWithIV;
        } catch (Exception e) {
            System.out.println(e.toString());
            return null;
        }
    }

    public static byte[] Decrypt(byte[] encryptedMessageWithIV, byte[] encryptionKey) {
        try {
            // reads the first 16 bytes as IV
            byte[] extractedIV = new byte[keyLength];
            System.arraycopy(encryptedMessageWithIV, 0, extractedIV, 0, keyLength);

            // reads the rest of the byte array as message
            byte[] encryptedMessage = new byte[encryptedMessageWithIV.length - keyLength];
            System.arraycopy(encryptedMessageWithIV, keyLength, encryptedMessage, 0, encryptedMessage.length);

            // decrypt using IV and key
            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(extractedIV));
            byte[] decryptedBytes = cipher.doFinal(encryptedMessage);

//            String decryptedMessage = new String(decryptedBytes, StandardCharsets.UTF_8);

            return decryptedBytes;
        } catch (Exception e) {
            System.out.println(e.toString());
            return null;
        }
    }
}
