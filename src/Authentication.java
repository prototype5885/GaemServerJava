import Classes.AuthenticationResult;
import Classes.ConnectedPlayer;
import Classes.Packet;
import ClassesShared.InitialData;
import ClassesShared.LoginData;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class Authentication {
    private final ConnectedPlayer[] connectedPlayers;
    private final PacketProcessor packetProcessor;
    private final Gson gson;
    private final PlayersManager playersManager;
    private ServerSocket tcpServerSocket;
    private int maxPlayers;
    private int tickRate;

    public Authentication(ConnectedPlayer[] connectedPlayers, Gson gson, PacketProcessor packetProcessor, PlayersManager playersManager, ServerSocket tcpServerSocket, int maxPlayers, int tickRate) {
        this.connectedPlayers = connectedPlayers;
        this.packetProcessor = packetProcessor;
        this.gson = gson;
        this.playersManager = playersManager;
        this.tcpServerSocket = tcpServerSocket;
        this.maxPlayers = maxPlayers;
        this.tickRate = tickRate;
    }

    public ConnectedPlayer HandleNewConnections() {
        try {
            // waits for a player to connect
            Socket tcpClientSocket = WaitForPlayerToConnect();
            if (tcpClientSocket == null) {
                System.out.printf("(%s) Error while accepting new client%n", LocalDateTime.now());
                return null;
            }

            // find which player slot is free, if server is full then return -1
            ConnectedPlayer connectedPlayer;
            int slotIndex = FindFreeSlotForConnectingPlayer();
            if (slotIndex == -1) // runs if server is full
            {
                InitialData initialData = PlayerRejected(tcpClientSocket, 7); // result 7 means server is full
                connectedPlayer = new ConnectedPlayer();
                connectedPlayer.tcpClientSocket = tcpClientSocket;
                SendResponseToPlayer(connectedPlayer, initialData);
                return null;
            }

            // continue the authentication using the login data the player has sent during connection
            AuthenticationResult authenticationResult = AuthenticateConnectingPlayer(tcpClientSocket, slotIndex);

            if (authenticationResult.result != 1) { // runs if authentication failed for any reason
                InitialData initialData = PlayerRejected(tcpClientSocket, authenticationResult.result);
                connectedPlayer = new ConnectedPlayer();
                connectedPlayer.tcpClientSocket = tcpClientSocket;
                SendResponseToPlayer(connectedPlayer, initialData);
                return null;
            }

            // adds the new player to the list of connected players
            connectedPlayer = AddNewPlayerToPlayerList(authenticationResult);
            connectedPlayers[authenticationResult.slotIndex] = connectedPlayer;

            // reply back to the player about the authentication success
            InitialData initialData = PlayerAccepted(authenticationResult, maxPlayers, tickRate);
            SendResponseToPlayer(connectedPlayer, initialData);

            return connectedPlayer;

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    public Socket WaitForPlayerToConnect() {
        try {
            System.out.println("Waiting for a player to connect...");
            Socket tcpClientSocket = tcpServerSocket.accept();
            return tcpClientSocket;

        } catch (Exception ex) {
            return null;
        }
    }

    public int FindFreeSlotForConnectingPlayer() {
        System.out.println("New player is connecting, searching for a free slot...");
        int freeSlotIndex = 0;
        for (ConnectedPlayer connectedPlayer : connectedPlayers) {
            if (connectedPlayer == null) {
                System.out.printf("Assigned slot id %s to connecting player, continuing to authenticate based on login data received%n", freeSlotIndex);
                return freeSlotIndex;
            } else {
                freeSlotIndex++;
            }
            if (freeSlotIndex > maxPlayers - 1) {
                break;
            }
        }
        return -1;
    }

    public AuthenticationResult AuthenticateConnectingPlayer(Socket tcpClientSocket, int freeSlotIndex) throws IOException, InterruptedException, IllegalAccessException, NoSuchFieldException, InstantiationException {
        byte[] buffer = new byte[512];
        int bytesRead = tcpClientSocket.getInputStream().read(buffer);
        byte[] receivedBytes = new byte[bytesRead];
        System.arraycopy(buffer, 0, receivedBytes, 0, bytesRead);
        List<Packet> packets = packetProcessor.ProcessBuffer(receivedBytes, bytesRead);
        // Monitoring.receivedBytesPerSecond += bytesRead;

        AuthenticationResult authenticationResult = new AuthenticationResult();
        for (Packet packet : packets) {
            if (packet.type == 1) {
                LoginData loginData = gson.fromJson(packet.data, LoginData.class);
//                System.out.printf("(%s) Login data has arrived from player %s%n", LocalDateTime.now(), loginData.un);

                if (loginData.lr) // Runs if client wants to log in
                {
//                    authenticationResult = Database.LoginUser(username, hashedPassword, Server.connectedPlayers);
                } else // Runs if client wants to register
                {
//                    authenticationResult = Database.RegisterUser(username, hashedPassword);
                    if (authenticationResult.result == 1) // runs if registration was successful
                    {
//                        authenticationResult = Database.LoginUser(username, hashedPassword, Server.connectedPlayers);
                    }
                }
                authenticationResult.result = 1;
                authenticationResult.slotIndex = freeSlotIndex;
                authenticationResult.tcpClientSocket = tcpClientSocket;
                authenticationResult.playerName = loginData.un;
                break;

            }
        }
        return authenticationResult;
    }

    public ConnectedPlayer AddNewPlayerToPlayerList(AuthenticationResult authenticationResult) throws IOException {
        ConnectedPlayer connectedPlayer = new ConnectedPlayer();

        connectedPlayer.index = authenticationResult.slotIndex;
        connectedPlayer.databaseID = authenticationResult.dbindex;
        connectedPlayer.tcpClientSocket = authenticationResult.tcpClientSocket;
        connectedPlayer.inputStream = authenticationResult.tcpClientSocket.getInputStream();
        connectedPlayer.outputStream = authenticationResult.tcpClientSocket.getOutputStream();
        connectedPlayer.ipAddress = authenticationResult.tcpClientSocket.getInetAddress();
        connectedPlayer.tcpPort = authenticationResult.tcpClientSocket.getPort();
        connectedPlayer.playerName = authenticationResult.playerName;

        connectedPlayers[authenticationResult.slotIndex] = connectedPlayer;

        System.out.println("Added new player to the list of connected players");
        return connectedPlayer;
    }

    public InitialData PlayerAccepted(AuthenticationResult authenticationResult, int maxPlayers, int tickRate) {
        InitialData initialData = new InitialData();
        initialData.lr = 1;
        initialData.i = authenticationResult.slotIndex;
        initialData.mp = maxPlayers;
        initialData.tr = tickRate;
        initialData.pda = playersManager.GetDataOfEveryConnectedPlayer(maxPlayers);

        System.out.println("Player was accepted successfully");
        return initialData;
    }


    public InitialData PlayerRejected(Socket tcpClientSocket, int authenticationResult) throws IOException, InterruptedException, IllegalAccessException {
        switch (authenticationResult) {
            case 7:
                System.out.println("Server is full, rejecting connection");
                break;
        }
        InitialData initialData = new InitialData();
        initialData.lr = authenticationResult;
        initialData.i = 0;
        initialData.mp = 0;
        initialData.tr = 0;

        System.out.println("Player rejected");
        return initialData;
    }

    public void SendResponseToPlayer(ConnectedPlayer connectedPlayer, InitialData initialData) throws IOException, InterruptedException {
        String jsonData = gson.toJson(initialData);
        packetProcessor.SendTcp(1, jsonData, connectedPlayer);
        System.out.println("Initial data has been sent to player.");
        if (initialData.lr != 1) {
            Thread.sleep(1000);
            connectedPlayer.tcpClientSocket.close();
        }
    }

    public void DisconnectClient(ConnectedPlayer connectedPlayer) throws IOException {
        connectedPlayer.tcpClientSocket.close();

        System.out.println("Player %s was disconnected.");

        int index = Arrays.binarySearch(connectedPlayers, connectedPlayer);
        connectedPlayers[index] = null;

    }
}
