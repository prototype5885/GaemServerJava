package org.ProToType.Static;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Classes.Packet;
import org.ProToType.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class PacketProcessor {
    private static final Logger logger = LogManager.getLogger(PacketProcessor.class);

    public static byte[] MakePacketForSending(int type, Object object) throws JsonProcessingException {
        String messageString = type + "\\p" + Main.jackson.writeValueAsString(object);

        if (Encryption.encryptionEnabled) {
            byte[] encryptedMessageBytes = Encryption.Encrypt(messageString);
            byte[] encryptedMessageBytesWithLength = AppendLengthToBeginning(encryptedMessageBytes);
            return encryptedMessageBytesWithLength;
        } else {
            byte[] messageBytes = messageString.getBytes();
            byte[] messageBytesWithlength = AppendLengthToBeginning(messageBytes);
            return messageBytesWithlength;
        }
    }

    public static List<Packet> ProcessReceivedBytes(byte[] buffer, int byteLength, ConnectedPlayer packetOwner) {
        List<String> jsonPackets = new ArrayList<>();

        byte[] receivedBytes = new byte[byteLength];
        System.arraycopy(buffer, 0, receivedBytes, 0, byteLength);


        // the list that will hold the separated encrypted packets
        List<byte[]> separatedPacketBytes = new ArrayList<>();

        int currentIndex = 0;
        while (currentIndex < receivedBytes.length) {
            // creates a 2 byte length array that stores the value read from the first 2 bytes of the given array
            byte[] arrayThatHoldsLength = new byte[2];
            System.arraycopy(receivedBytes, currentIndex, arrayThatHoldsLength, 0, 2);

            // reads the int from the 2 byte length holder array
            int length = 0;
            length |= (arrayThatHoldsLength[0] & 0xFF) << 8;
            length |= (arrayThatHoldsLength[1] & 0xFF);

            byte[] encryptedPacket = new byte[length];
            System.arraycopy(receivedBytes, currentIndex + 2, encryptedPacket, 0, length);

            separatedPacketBytes.add(encryptedPacket);
            logger.trace("Separated encrypted packet, start index: {}, length: {}", currentIndex, length);
            currentIndex += length + 2;
        }
        if (Encryption.encryptionEnabled) { // if encryption is enabled
            for (byte[] encryptedPacketBytes : separatedPacketBytes) {
                jsonPackets.add(Encryption.Decrypt(encryptedPacketBytes));
            }
        } else { // if encryption is disabled
            for (byte[] packetBytes : separatedPacketBytes) {
                jsonPackets.add(new String(packetBytes, StandardCharsets.UTF_8));
            }
        }
        List<Packet> packets = new ArrayList<>();

        int foundLines = 0;
        for (String jsonPacket : jsonPackets) {
            String[] splitLine = jsonPacket.split("\\\\p");
            try {
                Packet packet = new Packet();
                packet.type = Integer.parseInt(splitLine[0]);
                packet.owner = packetOwner;
                packet.json = splitLine[1];

                logger.trace("Separated packet, line: {}, type: {}, json: {}", foundLines, packet.type, packet.json);
                packets.add(packet);
            } catch (NumberFormatException e) {
                logger.error("Error parsing integer from value: {}, in line {}", splitLine[0], foundLines);
            }
            foundLines++;
        }
        if (foundLines > 1) {
            logger.debug("Multiple packets were received as one, packets: {}", foundLines);
        }
        return packets;
    }

    private static byte[] AppendLengthToBeginning(byte[] encryptedMessageBytes) {
        // stores int in a 2 byte length array
        byte[] arrayThatHoldsLength = new byte[2];
        arrayThatHoldsLength[0] = (byte) (encryptedMessageBytes.length >> 8);
        arrayThatHoldsLength[1] = (byte) encryptedMessageBytes.length;

        // creates an array that will hold both the length and the encrypted message
        byte[] mergedArray = new byte[encryptedMessageBytes.length + 2];

        // copies the 2 byte length holder array to the beginning
        System.arraycopy(arrayThatHoldsLength, 0, mergedArray, 0, 2);

        // copies the encrypted message to after the second byte
        System.arraycopy(encryptedMessageBytes, 0, mergedArray, 2, encryptedMessageBytes.length);

        return mergedArray;
    }
}
