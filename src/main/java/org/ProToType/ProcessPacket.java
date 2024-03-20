package org.ProToType;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Classes.Packet;
import org.ProToType.ClassesShared.PlayerPosition;
import org.ProToType.Static.Encryption;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ProcessPacket {
    public static void ProcessReceivedBytes(ConnectedPlayer connectedPlayer, byte[] buffer, int byteLength) {
        // trims the buffer down
        byte[] receivedBytes = new byte[byteLength];
        System.arraycopy(buffer, 0, receivedBytes, 0, byteLength);

        // Decrypts or decodes the message
        String receivedBytesInString = Decode(receivedBytes);

        // Separate if multiple packets were read as one
        List<Packet> packets = SeparatePackets(receivedBytesInString);

        // Processes each packet
        for (Packet packet : packets) {
            ProcessDataSentByPlayer(packet, connectedPlayer);
        }
    }

    public static String Decode(byte[] receivedBytes) {
        String receivedBytesInString;
        if (Encryption.encryptionEnabled)
            receivedBytesInString = Encryption.Decrypt(receivedBytes);
        else
            receivedBytesInString = new String(receivedBytes, StandardCharsets.UTF_8);
        return receivedBytesInString;
    }

    public static List<Packet> SeparatePackets(String receivedBytesInString) {
        String packetTypePattern = "#(.*?)#"; // pattern to read the packet type
        String packetDataPattern = "\\$(.*?)\\$"; // pattern to read the packet data

        Pattern typePattern = Pattern.compile(packetTypePattern);
        Pattern dataPattern = Pattern.compile(packetDataPattern);

        Matcher typeMatcher = typePattern.matcher(receivedBytesInString);
        Matcher dataMatcher = dataPattern.matcher(receivedBytesInString);

        List<Packet> packets = new ArrayList<>();

        while (typeMatcher.find() && dataMatcher.find()) {
            byte typeOfPacket = Byte.parseByte(typeMatcher.group(1));

            Packet packet = new Packet();
            packet.type = typeOfPacket;
            packet.data = dataMatcher.group(1);

            packets.add(packet);
        }
        return packets;
    }

    private static void ProcessDataSentByPlayer(Packet packet, ConnectedPlayer connectedPlayer) {
        switch (packet.type) {
            case 0:
                connectedPlayer.udpPingAnswered = true;
                connectedPlayer.status = 1;
                // Calculate latency here
                break;
            case 2:
//                playersManager.SendChatMessageToEveryone(connectedPlayer, packet.data);
                break;
            case 3:
                connectedPlayer.position = Main.gson.fromJson(packet.data, PlayerPosition.class);
                break;
        }
    }
}
