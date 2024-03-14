import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.GaemServer.Classes.ConnectedPlayer;
import org.GaemServer.Classes.Packet;
import org.GaemServer.ClassesShared.PlayerPosition;

public class PacketProcessor {
    public static void SendTcp(int commandType, String message, ConnectedPlayer connectedPlayer) {
        try {
            byte[] messageBytes = EncodeMessage(commandType, message);
            // Monitoring here
            connectedPlayer.outputStream.write(messageBytes);
        } catch (Exception ex) {
            System.out.println("Failed sending TCP data");
        }
    }

    public static void SendUdp(int commandType, String message, ConnectedPlayer connectedPlayer) throws IOException {
        if (connectedPlayer.udpPort != 0) {
            byte[] messageBytes = EncodeMessage(commandType, message);
            // Monitoring here
            DatagramPacket udpPacket = new DatagramPacket(messageBytes, messageBytes.length, connectedPlayer.ipAddress, connectedPlayer.udpPort);
            Server.udpServerSocket.send(udpPacket);
        }

    }

    public static void ReceiveTcpData(ConnectedPlayer connectedPlayer) {
        try {
            System.out.printf("(%s) Waiting for Tcp data from client %s:%s %n", LocalDateTime.now(), connectedPlayer.ipAddress, connectedPlayer.tcpPort);

            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = connectedPlayer.inputStream.read(buffer);
                byte[] receivedBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, receivedBytes, 0, bytesRead);
                List<Packet> packets = ProcessBuffer(buffer, bytesRead);
                ProcessPackets(packets, connectedPlayer);
            }
//            connectedPlayer.tcpClientSocket.close();
//            System.out.println("Client disconnected: " + connectedPlayer.ipAddress + ":" + connectedPlayer.tcpPort);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void ReceiveUdpData() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
                Server.udpServerSocket.receive(udpPacket);

                ConnectedPlayer connectedPlayer = Authentication.CheckAuthenticationOfUdpClient(udpPacket.getAddress(), udpPacket.getPort());
                if (connectedPlayer != null) {
                    List<Packet> packets = ProcessBuffer(udpPacket.getData(), udpPacket.getLength());
                    ProcessPackets(packets, connectedPlayer);
                }
            }
        } catch (Exception ex) {
            System.out.println("Error receiving UDP packet");
        }
    }

    public static List<Packet> ProcessBuffer(byte[] buffer, int byteLength) {
        String receivedBytesInString;

        if (Encryption.encryption) {
            byte[] receivedBytes = new byte[byteLength];
            System.arraycopy(buffer, 0, receivedBytes, 0, byteLength);
            receivedBytesInString = Encryption.Decrypt(receivedBytes);
        } else {
            receivedBytesInString = new String(buffer, StandardCharsets.UTF_8);
        }

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

    private static void ProcessPackets(List<Packet> packets, ConnectedPlayer connectedPlayer) {
        for (Packet packet : packets) {
            ProcessDataSentByPlayer(packet, connectedPlayer);
        }
    }

    private static void ProcessDataSentByPlayer(Packet packet, ConnectedPlayer connectedPlayer) {
        switch (packet.type) {
            case 0:
                connectedPlayer.udpPingAnswered = true;
                connectedPlayer.status = 1;
                // Calculate latency here
                break;
            case 2:
                PlayersManager.SendChatMessageToEveryone(connectedPlayer, packet.data);
                break;
            case 3:
                connectedPlayer.position = Server.gson.fromJson(packet.data, PlayerPosition.class);
                break;
        }
    }

    private static byte[] EncodeMessage(int commandType, String message) {
        if (Encryption.encryption) {
            String messageString = "#" + commandType + "#" + "$" + message + "$";
            return Encryption.Encrypt(messageString);
        } else {
            return ("#" + commandType + "#" + "$" + message + "$").getBytes();
        }
    }
}
