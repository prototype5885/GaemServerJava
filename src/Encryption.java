

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class Encryption
{
    static String keystring = "0123456789ABCDEF0123456789ABCDEF";
    static byte ivLength = 16;
    static byte[] encryptionKey;
    public static boolean encryption = true;

    public static void GetEncryptionKey()
    {
        encryptionKey = keystring.getBytes(StandardCharsets.UTF_8);
    }

    public static String Decrypt(byte[] encryptedMessageWithIV)
    {
        try
        {
            // Reads the IV from byte array
            byte[] extractedIV = new byte[ivLength];
            System.arraycopy(encryptedMessageWithIV, 0, extractedIV, 0, ivLength);

            // Reads the message part from the byte array
            byte[] encryptedMessage = new byte[encryptedMessageWithIV.length - ivLength];
            System.arraycopy(encryptedMessageWithIV, ivLength, encryptedMessage, 0, encryptedMessage.length);

            // Decrypt using IV and key
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(extractedIV));
            byte[] decryptedBytes = cipher.doFinal(encryptedMessage);

            // Convert decrypted bytes to UTF-8 string
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public static byte[] Encrypt(String message)
    {
        try
        {
            byte[] unencryptedBytes = message.getBytes();
            byte[] randomIV = new byte[ivLength];

            new SecureRandom().nextBytes(randomIV);

            // Initialize the Cipher for encryption
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(randomIV));

            byte[] encryptedBytes = cipher.doFinal(message.getBytes());

            byte[] encryptedBytesWithIV = new byte[encryptedBytes.length + ivLength];
            System.arraycopy(randomIV, 0, encryptedBytesWithIV, 0, ivLength);
            System.arraycopy(encryptedBytes, 0, encryptedBytesWithIV, ivLength, encryptedBytes.length);

            return encryptedBytesWithIV;
        }
        catch (Exception ex)
        {
            System.out.println("Encryption was failure");
            return null;
        }

    }
}
