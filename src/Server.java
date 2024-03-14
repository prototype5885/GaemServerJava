

import com.google.gson.Gson;
import org.GaemServer.Classes.AuthenticationResult;
import org.GaemServer.Classes.ConnectedPlayer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.time.LocalDateTime;

public class Server {
    public static ServerSocket tcpServerSocket;
    public static DatagramSocket udpServerSocket;

    public int tcpPort;
    public int udpPort;
    public static ConnectedPlayer[] connectedPlayers;
    public int maxPlayers;
    public int tickRate;

    public static Gson gson = new Gson();

    Database database = new Database();
    Connection dbConnection = null;

    public void HostServer() throws IOException {
        maxPlayers = 10;
        tickRate = 10;

        tcpPort = 1942;
        udpPort = tcpPort + 1;

        tcpServerSocket = new ServerSocket(tcpPort); // Starts TCP server
        udpServerSocket = new DatagramSocket(udpPort);// Starts UDP server

        connectedPlayers = new ConnectedPlayer[maxPlayers];
        Encryption.GetEncryptionKey();

        dbConnection = database.Connect();

        new Thread(() -> PacketProcessor.ReceiveUdpData()).start();

        while (true) {
            try {
                // waits for a player to connect
                Socket tcpClientSocket = Authentication.WaitForPlayerToConnect(tcpServerSocket);
                if (tcpClientSocket == null) {
                    System.out.printf("(%s) Error while accepting new client%n", LocalDateTime.now());
                    continue;
                }

                // find which player slot is free, if server is full then return -1
                int slotIndex = Authentication.FindFreeSlotForConnectingPlayer(maxPlayers);
                if (slotIndex == -1) // runs if server is full
                {
                    Authentication.ConnectionRejected(tcpClientSocket, 7); // result 7 means server is full
                    continue;
                }

                // continue the authentication using the login data the player has sent during connection
                System.out.printf("(%s) Assigned slot id %s to connecting player, continuing to authenticate based on login data received%n", LocalDateTime.now(), slotIndex);
                AuthenticationResult authenticationResult = Authentication.AuthenticateConnectingPlayer(tcpClientSocket, slotIndex);

                if (authenticationResult.result != 1) {
                    Authentication.ConnectionRejected(tcpClientSocket, authenticationResult.result);
                    continue;
                }

                // adds the new player to the list of connected players
                Authentication.AddNewPlayerToPlayerList(authenticationResult);

                // reply back to the player about the authentication result
                Authentication.SendResponseToConnectingPlayer(authenticationResult, maxPlayers, tickRate);

                // Start receiving data from the player
                new Thread(() -> PacketProcessor.ReceiveTcpData(connectedPlayers[authenticationResult.slotIndex])).start();
//                PlayersManager.SendPlayerDataToEveryone();

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
