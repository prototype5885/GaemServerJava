package org.ProToType.Static;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Classes.LoginResult;
import org.ProToType.Classes.Packet;
import org.ProToType.ClassesShared.InitialData;
import org.ProToType.ClassesShared.LoginData;
import org.ProToType.Main;
import org.ProToType.Threaded.ReceiveTcpPacket;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class PlayerAuthenticator {

    public void StartAuthentication(Socket tcpClientSocket) {
        try {
            PrintWithTime.print("Started a new thread for new player");

            // find which player slot is free
            ConnectedPlayer connectedPlayer = FindFreeSlotForConnectingPlayer();
            if (connectedPlayer == null) { // runs if server is full
                SendNegativeResponseAndDisconnect(tcpClientSocket, 7);
                PrintWithTime.print("Server is full");
                return;
            }

            // process the login data packet sent by the connecting player
            LoginData loginData = ProcessLoginData(tcpClientSocket);
            if (loginData == null) {
                SendNegativeResponseAndDisconnect(tcpClientSocket, 7);
                PrintWithTime.print("Error receiving login data from connecting player");
                return;
            }

            // runs if player wants to register first
            int registrationResult = 1;
            if (loginData.reg) {
                registrationResult = RegisterPlayer(loginData);
            }
            if (registrationResult != 1) {
                SendNegativeResponseAndDisconnect(tcpClientSocket, 7);
                PrintWithTime.print("Registration failed");
                return;
            }

            // logs the player in
            LoginResult loginResult = LoginPlayer(loginData);
            if (loginResult.resultValue != 1) {
                PrintWithTime.print("Login failed");
                SendNegativeResponseAndDisconnect(tcpClientSocket, loginResult.resultValue);
                return;
            }
            connectedPlayer.databaseID = loginResult.dbindex;
            connectedPlayer.playerName = loginResult.playerName;

            // adds the new player to the list of connected players
            PrintWithTime.print(String.format("Adding player {%s} to the list of connected players...", loginResult.playerName));

            // finishes the ConnectedPlayer
            connectedPlayer.tcpClientSocket = tcpClientSocket;
            connectedPlayer.inputStream = tcpClientSocket.getInputStream();
            connectedPlayer.outputStream = tcpClientSocket.getOutputStream();
            connectedPlayer.ipAddress = tcpClientSocket.getInetAddress();
            connectedPlayer.tcpPort = tcpClientSocket.getPort();
            PlayersManager.connectedPlayers[connectedPlayer.index] = connectedPlayer;

            // reply back to the player about the authentication success
            InitialData initialData = new InitialData();
            initialData.rv = loginResult.resultValue;
            initialData.i = connectedPlayer.index;
            initialData.mp = Main.maxPlayers;
            initialData.tr = Main.tickRate;
            initialData.pda = PlayersManager.GetDataOfEveryConnectedPlayer(Main.maxPlayers);
            SendPositiveResponseToPlayer(connectedPlayer, initialData);

            // sets last login ip address
            Database.UpdateLastLoginIpAddress(connectedPlayer);

            // starts a loop on a thread where the server listens to the player client's future packets
            PrintWithTime.print(String.format("Authentication for player {%s} was success, starts listening for tcp stream...", connectedPlayer.playerName));
            ReceiveTcpPacket playerClientHandler = new ReceiveTcpPacket(connectedPlayer); // handles authentication of connecting client
            Thread thread = new Thread(playerClientHandler);
            thread.start();

        } catch (Exception e) {
            PrintWithTime.print(e.toString());
        }
    }

    private ConnectedPlayer FindFreeSlotForConnectingPlayer() {
        PrintWithTime.print("Searching a free slot for the new player...");
        for (int i = 0; i < PlayersManager.connectedPlayers.length; i++) {
            if (PlayersManager.connectedPlayers[i] == null) {
                PrintWithTime.print(String.format("Assigning new player to slot %s", i));

                ConnectedPlayer connectedPlayer = new ConnectedPlayer();
                connectedPlayer.index = i;
                return connectedPlayer;
            }
        }
        return null;
    }

    private LoginData ProcessLoginData(Socket tcpClientSocket) throws IOException {
        PrintWithTime.print("Processing received LoginData...");

        byte[] buffer = new byte[512];
        int bytesRead = tcpClientSocket.getInputStream().read(buffer);
        byte[] receivedBytes = new byte[bytesRead];
        System.arraycopy(buffer, 0, receivedBytes, 0, bytesRead);

        String receivedBytesInString = ProcessPacket.Decode(receivedBytes);
        List<Packet> packets = ProcessPacket.SeparatePackets(receivedBytesInString);

        // Monitoring.receivedBytesPerSecond += bytesRead;

        for (Packet packet : packets) {
            PrintWithTime.print("Received data: " + packet.data);
            if (packet.type == 1) {
                LoginData loginData = Main.gson.fromJson(packet.data, LoginData.class);
                return loginData;
            }
        }
        return null;
    }

    private LoginResult LoginPlayer(LoginData loginData) throws SQLException {
        PrintWithTime.print(String.format("Logging in player {%s}...", loginData.un));

        LoginResult loginResult = new LoginResult();

        // searches if player is already connected to the server
        PrintWithTime.print(String.format("Checking if player {%s} is connected already...", loginData.un));
        for (ConnectedPlayer connectedPlayer : PlayersManager.connectedPlayers) {
            if (connectedPlayer == null) continue;
            if (loginData.un.equals(connectedPlayer.playerName)) {
                PrintWithTime.print(String.format("Player {%s} is already connected", loginData.un));
                loginResult.resultValue = 4;
                return loginResult;
            }
        }

        // searches for the player in the database
        PrintWithTime.print(String.format("Searching for player {%s} in the database...", loginData.un));
        try (ResultSet resultSet = Database.SearchForPlayerInDatabase(loginData.un)) {
            // runs if no such
            if (resultSet == null) {
                PrintWithTime.print(String.format("Player {%s} not found in database...", loginData.un));
                loginResult.resultValue = 3;
                return loginResult;
            }

            // reads password from database
            PrintWithTime.print(String.format("Comparing entered password for player {%s}...", loginData.un));
            String storedHashedPassword = resultSet.getString("Password");
            System.out.println(storedHashedPassword);
            System.out.println(loginData.pw);
            if (!loginData.pw.equals(storedHashedPassword)) {
                PrintWithTime.print(String.format("Player {%s} has entered wrong password", loginData.un));
                loginResult.resultValue = 2;
                return loginResult;
            }

            // login was a success, finish the LoginResult object
            PrintWithTime.print(String.format("Successful login for player {%s}", loginData.un));
            loginResult.resultValue = 1;
            loginResult.dbindex = resultSet.getInt("id");
            loginResult.playerName = resultSet.getString("PlayerName");
        }
        return loginResult;
    }

    private int RegisterPlayer(LoginData loginData) throws SQLException {
        PrintWithTime.print(String.format("Registering player {%s}...", loginData.un));

        // Checks if chosen name is longer than 16 or shorter than 2 characters
        PrintWithTime.print(String.format("Checking chosen name's length for player {%s}...", loginData.un));
        if (loginData.un.length() < 2 || loginData.un.length() > 16) {
            PrintWithTime.print(String.format("Player has {%s} chosen too long or too short name", loginData.un));
            return 5;
        }

        // Checks if the chosen name is already registered
        PrintWithTime.print(String.format("Checking if chosen name {%s} is already registered...", loginData.un));
        try (ResultSet resultSet = Database.SearchForPlayerInDatabase(loginData.un)) {
            if (resultSet != null) {
                PrintWithTime.print(String.format("Player name {%s} already exists in the database", loginData.un));
                return 6;
            }
        }

        // Adds the new player to the database
        PrintWithTime.print(String.format("Successful registration, adding new player {%s} to the database", loginData.un));
        Database.RegisterPlayer(loginData.un, loginData.pw);
        return 1;
    }

    private void PrintAuthenticationFailedReason(int authenticationResult) {
        switch (authenticationResult) {
            case 7:
                PrintWithTime.print("Server is full, rejecting connection");
                break;
        }
    }

    private void SendPositiveResponseToPlayer(ConnectedPlayer connectedPlayer, InitialData initialData) {
        PrintWithTime.print(String.format("Sending positive reply back to player {%s}...", connectedPlayer.playerName));
        String jsonData = Main.gson.toJson(initialData);
        SendPacket.SendTcp(1, jsonData, connectedPlayer);
    }

    private void SendNegativeResponseAndDisconnect(Socket tcpClientSocket, int resultValue) throws IOException {
        PrintWithTime.print("Sending negative initial data to the failed player...");

        InitialData initialData = new InitialData();
        initialData.rv = resultValue;

        ConnectedPlayer connectedPlayer = new ConnectedPlayer();
        connectedPlayer.tcpClientSocket = tcpClientSocket;
        connectedPlayer.outputStream = tcpClientSocket.getOutputStream();

        String jsonData = Main.gson.toJson(initialData);
        SendPacket.SendTcp(1, jsonData, connectedPlayer);

        PrintWithTime.print("Closing connection of the failed player...");
        try { // waits 1 second before disconnecting
            Thread.sleep(1000);
        } catch (InterruptedException e) { // if for some reason there is exception, just force disconnect
            PrintWithTime.print(e.toString());
        }
        connectedPlayer.tcpClientSocket.close();
        PrintWithTime.print("Connection closed with the failed player");
    }
}
