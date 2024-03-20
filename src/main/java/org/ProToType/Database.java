package org.ProToType;

import org.ProToType.Static.PrintWithTime;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    public static Connection dbConnection;

    public static void ConnectToDatabase(ConfigFile configFile) throws SQLException {
        if (configFile.localDatabase) {
            PrintWithTime.Print("Connecting to local database.");

            String url = "jdbc:h2:./GaemServerDatabase";

            dbConnection = DriverManager.getConnection(url);
            PrintWithTime.Print("Connected to SQLite successfully");

        } else {
            PrintWithTime.Print("Connecting to remote database");

            String type = "jdbc:mysql://";
            String ipAddress = configFile.remoteDatabaseIpAddress;
            String port = configFile.remoteDatabasePort;
            String databaseName = "/GaemServer";

            String url = type + ipAddress + ":" + port + databaseName;
//                String url = "jdbc:mysql://192.168.2.100:3306/GaemServer";
            String username = configFile.dbUsername;
            String password = configFile.dbPassword;

            dbConnection = DriverManager.getConnection(url, username, password);
            PrintWithTime.Print("Connected to MySQL successfully");
        }
        CreateTableIfNotExists();
    }

    private static void CreateTableIfNotExists() throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS Players " +
                "(ID INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "PlayerName TEXT, " +
                "Password TEXT, " +
                "Wage INTEGER, " +
                "Money INTEGER)";

        dbConnection.createStatement().execute(query);
    }

    public static void LoginPlayer(String username, String hashedPassword) {
        PrintWithTime.Print(String.format("Trying to login player {%s}...", username));
    }

    public static void RegisterPlayer(String username, String password) throws SQLException {
        String query = "INSERT INTO Players (PlayerName, Password, Wage, Money) VALUES (?, ?, ?, ?)";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        preparedStatement.setString(1, username);
        preparedStatement.setString(2, password);
        preparedStatement.setInt(3, 3);
        preparedStatement.setInt(4, 1010);
        preparedStatement.execute();

        PrintWithTime.Print(String.format("Player {%s} has been added to the database", username));
    }

    public static List<String> ListAllRegisteredPlayers() throws SQLException {
        String query = "SELECT PlayerName FROM Players";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        List<String> playerNameList = new ArrayList<>();
        while (resultSet.next()) {
            playerNameList.add(resultSet.getString("PlayerName"));
        }
        return playerNameList;
    }
}
