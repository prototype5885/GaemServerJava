

import org.GaemServer.Classes.AuthenticationResult;
import org.GaemServer.Classes.ConnectedPlayer;
import org.GaemServer.Classes.Packet;
import org.GaemServer.ClassesShared.InitialData;
import org.GaemServer.ClassesShared.LoginData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class Authentication {
    public static Socket WaitForPlayerToConnect(ServerSocket tcpServerSocket) {
        try {
            System.out.printf(String.format("(%s) Waiting for a player to connect...%n", LocalDateTime.now()));
            Socket tcpClientSocket = tcpServerSocket.accept();
            return tcpClientSocket;

        } catch (Exception ex) {
            return null;
        }
    }

    public static int FindFreeSlotForConnectingPlayer(int maxPlayers) throws IOException, NoSuchFieldException, InterruptedException, IllegalAccessException, InstantiationException {
        System.out.printf(String.format("(%s) New player is connecting, searching for a free slot...%n", LocalDateTime.now()));
        int freeSlotIndex = 0;
        for (ConnectedPlayer connectedPlayer : Server.connectedPlayers) {
            if (connectedPlayer == null) {
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

    public static AuthenticationResult AuthenticateConnectingPlayer(Socket tcpClientSocket, int freeSlotIndex) throws IOException, InterruptedException, IllegalAccessException, NoSuchFieldException, InstantiationException {
        System.out.printf("(%s) New player has connected, waiting for login data...%n", LocalDateTime.now());

        byte[] buffer = new byte[512];
        int bytesRead = tcpClientSocket.getInputStream().read(buffer);
        byte[] receivedBytes = new byte[bytesRead];
        System.arraycopy(buffer, 0, receivedBytes, 0, bytesRead);
        List<Packet> packets = PacketProcessor.ProcessBuffer(receivedBytes, bytesRead);
        // Monitoring.receivedBytesPerSecond += bytesRead;

        AuthenticationResult authenticationResult = new AuthenticationResult();
        for (Packet packet : packets) {
            if (packet.type == 1) {
                LoginData loginData = Server.gson.fromJson(packet.data, LoginData.class);
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

    public static void AddNewPlayerToPlayerList(AuthenticationResult authenticationResult) throws IOException {
        ConnectedPlayer connectedPlayer = new ConnectedPlayer();

        connectedPlayer.index = authenticationResult.slotIndex;
        connectedPlayer.databaseID = authenticationResult.dbindex;
        connectedPlayer.tcpClientSocket = authenticationResult.tcpClientSocket;
        connectedPlayer.inputStream = authenticationResult.tcpClientSocket.getInputStream();
        connectedPlayer.outputStream = authenticationResult.tcpClientSocket.getOutputStream();
        connectedPlayer.ipAddress = authenticationResult.tcpClientSocket.getInetAddress();
        connectedPlayer.tcpPort = authenticationResult.tcpClientSocket.getPort();
        connectedPlayer.playerName = authenticationResult.playerName;

        Server.connectedPlayers[authenticationResult.slotIndex] = connectedPlayer;
    }

    public static void SendResponseToConnectingPlayer(AuthenticationResult authenticationResult, int maxPlayers, int tickRate) {


        InitialData initialData = new InitialData();
        initialData.lr = 1;
        initialData.i = authenticationResult.slotIndex;
        initialData.mp = maxPlayers;
        initialData.tr = tickRate;
        initialData.pda = PlayersManager.GetDataOfEveryConnectedPlayer(maxPlayers);

        String jsonData = Server.gson.toJson(initialData);

        PacketProcessor.SendTcp(1, jsonData, Server.connectedPlayers[authenticationResult.slotIndex]);
        System.out.printf("(%s) Initial data has been sent to player %s%n", LocalDateTime.now(), authenticationResult.playerName);
    }


    public static void ConnectionRejected(Socket tcpClientSocket, int authenticationResult) throws IOException, InterruptedException, IllegalAccessException {
        switch (authenticationResult) {
            case 7:
                System.out.printf("(%s) Server is full, rejecting connection%n", LocalDateTime.now());
                break;
        }

        ConnectedPlayer connectedPlayer = new ConnectedPlayer();
        connectedPlayer.outputStream = tcpClientSocket.getOutputStream();

        InitialData initialData = new InitialData();
        initialData.lr = authenticationResult;
        initialData.i = 0;
        initialData.mp = 0;
        initialData.tr = 0;

        String jsonData = Server.gson.toJson(initialData);

        PacketProcessor.SendTcp(1, jsonData, connectedPlayer);
        Thread.sleep(1000);
        tcpClientSocket.close();
    }

    public static ConnectedPlayer CheckAuthenticationOfUdpClient(InetAddress ipAddress, int udpPort) {
        for (ConnectedPlayer player : Server.connectedPlayers) {
            if (player == null) continue;
            if (player.ipAddress.equals(ipAddress) && player.udpPort == 0) {
                if (player.udpPort != 0)
                    return player;
                player.udpPort = udpPort;
                return player;
            } else if (player.udpPort != 0 && ipAddress.equals(player.ipAddress)) {
                return player;
            }
        }
        return null;
    }

    public static void DisconnectClient(ConnectedPlayer connectedPlayer) throws IOException {
        connectedPlayer.tcpClientSocket.close();

        System.out.printf("(%s) Player %s was disconnected h %n", LocalDateTime.now(), connectedPlayer.playerName);

        int index = Arrays.binarySearch(Server.connectedPlayers, connectedPlayer);
        Server.connectedPlayers[index] = null;

    }
}
