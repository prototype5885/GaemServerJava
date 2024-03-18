import Classes.ConfigFileContent;
import Classes.ConnectedPlayer;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static final Gson gson = new Gson();
    public static final ServerSocket tcpServerSocket;
    public static final DatagramSocket udpServerSocket;
    public static final int maxPlayers;
    public static final int tickRate;
    public static final int tcpPort;
    public static final int udpPort;
    public static final ConnectedPlayer[] connectedPlayers;
    public static final Database database = new Database();

    // reads config file and parses content
    static {
        ConfigFileContent configFileContent = ConfigFile.readConfigFile();

        maxPlayers = configFileContent.maxPlayers;
        tickRate = configFileContent.tickRate;
        tcpPort = configFileContent.tcpPort;
        udpPort = tcpPort + 1;
        connectedPlayers = new ConnectedPlayer[configFileContent.maxPlayers];

        Encryption.encryptionKey = configFileContent.encryptionKey.getBytes(StandardCharsets.UTF_8);

        try {
            tcpServerSocket = new ServerSocket(tcpPort); // starts the TCP server
            udpServerSocket = new DatagramSocket(udpPort); // starts the UDP server
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
//        database.AddPlayer("User", "testpassword");

        new Thread(ReceivePacket::ReceiveUdpPacket).start(); // starts thread that listens to incoming udp connections from anyone
        new Thread(SendPacket::SendPlayerPositions).start(); // starts thread that sends the current player positions to each player

        // Handle new players joining
        Authentication authentication = new Authentication(); // handles authentication of connecting client
        while (true) {
            ConnectedPlayer connectedPlayer = authentication.HandleNewConnections();
            if (connectedPlayer != null) { // runs if the authentication of connecting player was successful
                new Thread(() -> ReceivePacket.ReceiveTcpPacket(connectedPlayer)).start();
            }
        }
    }
}