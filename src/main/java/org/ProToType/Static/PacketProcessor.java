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

    public static String Decode(byte[] buffer, int byteLength) {
        // trims the buffer down
        byte[] receivedBytes = new byte[byteLength];
        System.arraycopy(buffer, 0, receivedBytes, 0, byteLength);

        String decodedMessage = "";
        if (Encryption.encryptionEnabled)
            decodedMessage = Encryption.Decrypt(receivedBytes);
        else
            decodedMessage = new String(receivedBytes, StandardCharsets.UTF_8);

        if (decodedMessage != null) {
            logger.trace("Received bytes length: {}, decoded message length: {}", byteLength, decodedMessage.length());
        } else {
            logger.warn("Received message after decoding is null");
        }
        return decodedMessage;
    }

    public static List<Packet> SeparatePackets(String decodedMessage, ConnectedPlayer owner) {
        List<Packet> packets = new ArrayList<>();

        String[] lines = decodedMessage.trim().split("\n");

        int foundLines = 0;
        for (String line : lines) {
            String[] splitLine = line.split("\\\\p");
            try {
                Packet packet = new Packet();
                packet.type = Integer.parseInt(splitLine[0]);
                packet.owner = owner;
                packet.json = splitLine[1];

                logger.trace("Separated packet, line: {}, type: {}, json: {}", foundLines, packet.type, packet.json);
                packets.add(packet);
            } catch (NumberFormatException e) {
                logger.error("Error parsing integer from value: {}, in line {}", splitLine[0], foundLines);
            }
            foundLines++;
        }
        if (foundLines > 1) {
            logger.debug("Multiple packets were received as one");
        }
        return packets;
    }

    public static byte[] MakePacketForSending(int type, Object object) throws JsonProcessingException {
        String messageString = type + "\\p" + Main.jackson.writeValueAsString(object) + "\n";

        if (Encryption.encryptionEnabled) {
            return Encryption.Encrypt(messageString);
        } else {
            return (messageString).getBytes();
        }
    }
}
