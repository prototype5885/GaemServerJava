package org.ProToType.Static;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.ClassesShared.Packet;
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

        logger.trace("Received bytes length: {}", byteLength);

        if (Encryption.encryptionEnabled)
            return Encryption.Decrypt(receivedBytes);
        else
            return new String(receivedBytes, StandardCharsets.UTF_8);
    }
    
    public static List<Packet> SeparatePackets(String decodedMessage) {
        List<Packet> packets = new ArrayList<>();

        String[] lines = decodedMessage.split("\n");

        int foundLines = 0;
        for (String line : lines) {
            try {
                Packet packet = Main.jackson.readValue(line, Packet.class);
                logger.trace("Separated packet line: {}, type: {}, json: {}", foundLines, packet.type, packet.json);
                packets.add(packet);
            } catch (JsonProcessingException e) {
                logger.error("Error deserializing json line {}, json: {}", foundLines, line);
            }
            foundLines++;
        }
        if (foundLines > 1) {
            logger.debug("Multiple packets were received as one");
        }
        return packets;
    }
}
