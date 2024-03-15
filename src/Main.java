import Classes.ConnectedPlayer;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) throws IOException {
        int maxPlayers = 10;
        int tickRate = 10;

        int tcpPort = 1942;
        int udpPort = tcpPort + 1;

        Gson gson = new Gson();
        Database database = new Database();
        Encryption encryption = new Encryption();
        ConnectedPlayer[] connectedPlayers = new ConnectedPlayer[maxPlayers];

        ServerSocket tcpServerSocket = new ServerSocket(tcpPort); // Starts TCP server
        DatagramSocket udpServerSocket = new DatagramSocket(udpPort); // Starts UDP server

        PacketProcessor packetProcessor = new PacketProcessor(connectedPlayers, gson, udpServerSocket, encryption);
        PlayersManager playersManager = new PlayersManager(connectedPlayers, gson, packetProcessor);
        Authentication authentication = new Authentication(connectedPlayers, gson, packetProcessor, playersManager, tcpServerSocket, maxPlayers, tickRate);

        database.AddPlayer("User", "testpassword");

        new Thread(() -> packetProcessor.ReceiveUdpData()).start(); // starts thread that listens to incoming udp connections from anyone

        // Handle new players joining
        while (true) {
            ConnectedPlayer connectedPlayer = authentication.HandleNewConnections();
            if (connectedPlayer != null) {
                new Thread(() -> packetProcessor.ReceiveTcpData(connectedPlayer)).start();
            }
        }
    }
}