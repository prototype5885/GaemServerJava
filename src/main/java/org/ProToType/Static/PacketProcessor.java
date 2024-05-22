package org.ProToType.Static;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.Player;
import org.ProToType.Classes.Packet;
import org.ProToType.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class PacketProcessor {
    private static final Logger logger = LogManager.getLogger(PacketProcessor.class);

    public static byte[] MakePacketForSending(int type, Object obj, byte[] aesKey) throws JsonProcessingException {
        byte[] jsonBytes = AppendPacketType(type, Main.jackson.writeValueAsBytes(obj));
        if (EncryptionAES.encryptionEnabled) jsonBytes = EncryptionAES.Encrypt(jsonBytes, aesKey);
        return AppendLengthToBeginning(jsonBytes);
    }

    private static byte[] AppendPacketType(int packetType, byte[] jsonBytes) {
        // stores int in a 1 byte length array
        byte[] arrayThatHoldsPacketType = new byte[1];
        arrayThatHoldsPacketType[0] = (byte) packetType;

        // creates an array that will hold both the length and the encrypted message
        byte[] jsonWithType = new byte[1 + jsonBytes.length];

        // copies the 1 byte packet type value array to the beginning
        System.arraycopy(arrayThatHoldsPacketType, 0, jsonWithType, 0, 1);
        // copies the encrypted message to after the second byte
        System.arraycopy(jsonBytes, 0, jsonWithType, 1, jsonBytes.length);

        return jsonWithType;
    }

    private static byte[] AppendLengthToBeginning(byte[] encryptedMessageBytes) {
        // stores int in a 2 byte length array
        byte[] arrayThatHoldsLength = new byte[2];
        arrayThatHoldsLength[0] = (byte) (encryptedMessageBytes.length >> 8);
        arrayThatHoldsLength[1] = (byte) encryptedMessageBytes.length;

        // creates an array that will hold both the length and the encrypted message
        byte[] mergedArray = new byte[2 + encryptedMessageBytes.length];

        // copies the 2 byte length holder array to the beginning
        System.arraycopy(arrayThatHoldsLength, 0, mergedArray, 0, 2);

        // copies the encrypted message to after the second byte
        System.arraycopy(encryptedMessageBytes, 0, mergedArray, 2, encryptedMessageBytes.length);

        return mergedArray;
    }

    public static List<Packet> ProcessReceivedBytes(byte[] receivedBytes, Player packetOwner) {
        // the list that will hold the separated packets
        List<Packet> packets = new ArrayList<>();

        int currentIndex = 0;
        int foundPackets = 0;
        while (currentIndex < receivedBytes.length) {
            Packet packet = new Packet();
            packet.owner = packetOwner;

            // creates a 2 byte length array that stores the value read from the first 2 bytes of the given array
            byte[] arrayThatHoldsLength = new byte[2];
            System.arraycopy(receivedBytes, currentIndex, arrayThatHoldsLength, 0, 2);

            // reads the int from the 2 byte length holder array
            int length = 0;
            length |= (arrayThatHoldsLength[0] & 0xFF) << 8;
            length |= (arrayThatHoldsLength[1] & 0xFF);
            logger.trace("Received packet length: {}", length);

            // separate the packet part from the length
            byte[] packetBytes = new byte[length];
            System.arraycopy(receivedBytes, currentIndex + 2, packetBytes, 0, length);
            logger.trace("Separated packet from length:");
            ByteProcessor.PrintByteArrayAsHex(packetBytes);

            // decrypt if encrypted
            if (EncryptionAES.encryptionEnabled) { // if encryption is enabled
                packetBytes = EncryptionAES.Decrypt(packetBytes, packetOwner.aesKey);
            }

            logger.trace("Decrypted packet:");
            ByteProcessor.PrintByteArrayAsHex(packetBytes);

            // read the first byte to get packet type
            packet.type = packetBytes[0] & 0xFF;
            int packetLength = packetBytes.length - 1;
            logger.trace("Packet type is: {}", packet.type);

            // read the rest of the byte array
            byte[] jsonBytes = new byte[packetLength];
            System.arraycopy(packetBytes, 1, jsonBytes, 0, packetLength);

            // decode into json string
            packet.json = new String(jsonBytes, StandardCharsets.UTF_8);
            logger.trace("Json: {}", packet.json);

            packets.add(packet);

            logger.trace("Separated packet, start index: {}, length: {}", currentIndex, length);
            currentIndex += length + 2;
            foundPackets++;
        }
        if (foundPackets > 1) {
            logger.debug("Multiple packets were received as one, packets: {}", foundPackets);
        }
        return packets;
    }

    public static byte[] ReceiveBytes(Socket tcpClientSocket) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead = tcpClientSocket.getInputStream().read(buffer);
        byte[] receivedBytes = new byte[bytesRead];
        System.arraycopy(buffer, 0, receivedBytes, 0, bytesRead);

        return receivedBytes;
    }
}
