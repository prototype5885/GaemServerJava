import Classes.ConnectedPlayer;
import Classes.Packet;
import ClassesShared.PlayerPosition;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PacketProcessor {
    private DatagramSocket udpServerSocket;
    private Gson gson;
    private ConnectedPlayer[] connectedPlayers;
    private Encryption encryption;


    public PacketProcessor(ConnectedPlayer[] connectedPlayers, Gson gson, DatagramSocket udpServerSocket, Encryption encryption) {
        this.udpServerSocket = udpServerSocket;
        this.gson = gson;
        this.connectedPlayers = connectedPlayers;
        this.encryption = encryption;
    }

    public void SendTcp(int commandType, String message, ConnectedPlayer connectedPlayer) {
        try {
            byte[] messageBytes = EncodeMessage(commandType, message);
            // Monitoring here
            connectedPlayer.outputStream.write(messageBytes);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void SendUdp(int commandType, String message, ConnectedPlayer connectedPlayer) {
        try {
            if (connectedPlayer.udpPort == 0) return;
            byte[] messageBytes = EncodeMessage(commandType, message);
            // Monitoring here
            DatagramPacket udpPacket = new DatagramPacket(messageBytes, messageBytes.length, connectedPlayer.ipAddress, connectedPlayer.udpPort);
            udpServerSocket.send(udpPacket);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }


    public void ReceiveTcpData(ConnectedPlayer connectedPlayer) {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = connectedPlayer.inputStream.read(buffer);
                byte[] receivedBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, receivedBytes, 0, bytesRead);
                List<Packet> packets = ProcessBuffer(buffer, bytesRead);
                ProcessPackets(packets, connectedPlayer);
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void ReceiveUdpData() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
                udpServerSocket.receive(udpPacket);

                ConnectedPlayer connectedPlayer = null;
                for (ConnectedPlayer player : connectedPlayers) { // check authentication of sender, rejects packet if not actual connected player
                    if (player == null) continue;
                    if (player.ipAddress.equals(udpPacket.getAddress())) {
                        if (player.udpPort == 0) // this runs if sender is an actual player, but havent sent udp packet before
                            player.udpPort = udpPacket.getPort();
                        connectedPlayer = player;
                    }
                }
                if (connectedPlayer != null) { // runs if sender is a connected players
                    List<Packet> packets = ProcessBuffer(udpPacket.getData(), udpPacket.getLength());
                    ProcessPackets(packets, connectedPlayer);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public List<Packet> ProcessBuffer(byte[] buffer, int byteLength) {
        String receivedBytesInString;

        if (encryption.encryptionEnabled) {
            byte[] receivedBytes = new byte[byteLength];
            System.arraycopy(buffer, 0, receivedBytes, 0, byteLength);
            receivedBytesInString = encryption.Decrypt(receivedBytes);
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

    private void ProcessPackets(List<Packet> packets, ConnectedPlayer connectedPlayer) {
        for (Packet packet : packets) {
            ProcessDataSentByPlayer(packet, connectedPlayer);
        }
    }

    private void ProcessDataSentByPlayer(Packet packet, ConnectedPlayer connectedPlayer) {
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
                connectedPlayer.position = gson.fromJson(packet.data, PlayerPosition.class);
                break;
        }
    }

    private byte[] EncodeMessage(int commandType, String message) {
        if (encryption.encryptionEnabled) {
            String messageString = "#" + commandType + "#" + "$" + message + "$";
            return encryption.Encrypt(messageString);
        } else {
            return ("#" + commandType + "#" + "$" + message + "$").getBytes();
        }
    }
}
