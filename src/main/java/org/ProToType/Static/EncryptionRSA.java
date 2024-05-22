package org.ProToType.Static;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class EncryptionRSA {
    public static KeyPair keypair;
    private static Cipher cipher;

    public static void Initialize() throws NoSuchPaddingException, NoSuchAlgorithmException {
        cipher = Cipher.getInstance("RSA");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        keypair = keyPairGenerator.generateKeyPair();
    }

    public static byte[] Encrypt(byte[] messageBytes, PublicKey publicKey) throws Exception {
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(messageBytes);
    }

    public static byte[] Decrypt(byte[] encryptedMessage) throws Exception {
        cipher.init(Cipher.DECRYPT_MODE, keypair.getPrivate());
        return cipher.doFinal(encryptedMessage);
    }
}
