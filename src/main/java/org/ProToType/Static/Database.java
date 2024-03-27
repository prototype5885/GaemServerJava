package org.ProToType.Static;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.ClassesShared.PlayerPosition;
import org.ProToType.Instanceables.ConfigFile;
import org.ProToType.Main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    public static Connection dbConnection;

    public static void ConnectToDatabase(ConfigFile configFile) throws Exception {
        switch (configFile.databaseType) {
            case "h2":
                ConnectToLocalH2();
                return;
            case "h2server":
                ConnectToH2Server();
                return;
            case "sqlite":
                ConnectToSQLite();
                return;
            case "mysql":
                ConnectToMySQL(configFile);
                return;
        }
        throw new Exception("No database could have been loaded");
    }

    private static void ConnectToLocalH2() throws SQLException, ClassNotFoundException {
        Shortcuts.PrintWithTime("Connecting to local H2 database...");
        Class.forName("org.h2.Driver");

//        String user = "user";
//        String password = "password";

        String dbType = "jdbc:h2:";
        String dbName = "./db/GaemServerDatabase";
        String url = dbType + dbName;

        dbConnection = DriverManager.getConnection(url);

        String query = InitialQuery("h2");
        Statement statement = dbConnection.createStatement();
        statement.executeUpdate(query);
        Shortcuts.PrintWithTime("Connected to local H2 database successfully");
    }

    private static void ConnectToH2Server() throws SQLException, ClassNotFoundException {
        Shortcuts.PrintWithTime("Connecting to H2 server database...");
        Class.forName("org.h2.Driver");

        org.h2.tools.Server.createTcpServer("-tcp", "-ifNotExists").start();

//        String user = "user";
//        String password = "password";

        String dbType = "jdbc:h2:tcp://localhost/";
        String dbName = "./db/GaemServerDatabase";
        String url = dbType + dbName;

        dbConnection = DriverManager.getConnection(url);

        String query = InitialQuery("h2");
        Statement statement = dbConnection.createStatement();
        statement.executeUpdate(query);
        Shortcuts.PrintWithTime("Connected to H2 server database successfully");
    }

    private static void ConnectToSQLite() throws SQLException, ClassNotFoundException {
        Shortcuts.PrintWithTime("Connecting to SQLite database...");
        Class.forName("org.sqlite.JDBC");

        String dbType = "jdbc:sqlite:";
        String dbName = "./db/GaemServerDatabase.sqlite";
        String url = dbType + dbName;

        dbConnection = DriverManager.getConnection(url);

        String query = InitialQuery("sqlite");
        Statement statement = dbConnection.createStatement();
        statement.executeUpdate(query);
        Shortcuts.PrintWithTime("Connected to SQLite database successfully");
    }

    private static void ConnectToMySQL(ConfigFile configFile) throws SQLException, ClassNotFoundException {
        Shortcuts.PrintWithTime("Connecting to MySQL database...");
        Class.forName("com.mysql.cj.jdbc.Driver");

        String type = "jdbc:mysql://";
        String ipAddress = configFile.remoteDatabaseIpAddress;
        String port = configFile.remoteDatabasePort;
        String dbName = "/GaemServer";

        String url = type + ipAddress + ":" + port + dbName;
        String username = configFile.dbUsername;
        String password = configFile.dbPassword;

        dbConnection = DriverManager.getConnection(url, username, password);

        String query = InitialQuery("mysql");
        Statement statement = dbConnection.createStatement();
        statement.executeUpdate(query);
        Shortcuts.PrintWithTime("Connected to MySQL database successfully");
    }

    private static String InitialQuery(String dbType) {
        String autoIncrement = "AUTO_INCREMENT";
        if (dbType.equals("sqlite")) {
            autoIncrement = "AUTOINCREMENT";
        }

        return "CREATE TABLE IF NOT EXISTS Players " +
                "(ID INTEGER PRIMARY KEY " + autoIncrement + "," +
                "PlayerName TEXT," +
                "Password CHAR(64)," +
                "Wage INTEGER," +
                "Money INTEGER," +
                "LastLoginIp CHAR(15)," +
                "LastPosition TEXT)";
    }

    public static void RegisterPlayer(String playerName, String hashedPassword) throws SQLException {
        String query = "INSERT INTO Players (PlayerName, Password, Wage, Money) VALUES (?, ?, ?, ?)";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        preparedStatement.setString(1, playerName);
//        preparedStatement.setString(2, BCrypt.withDefaults().hashToString(10, hashedPassword.toCharArray()));
        preparedStatement.setString(2, hashedPassword);
        preparedStatement.setInt(3, 3);
        preparedStatement.setInt(4, 1010);
        preparedStatement.execute();

        Shortcuts.PrintWithTime(String.format("Player {%s} has been added to the database", playerName));
    }

    public static ResultSet SearchForPlayerInDatabase(String playerName) throws SQLException {
        String query = "SELECT * FROM Players WHERE PlayerName = ?";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        preparedStatement.setString(1, playerName);

        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            return resultSet; // returns if found
        }
        return null;
    }

    public static List<String> ListAllRegisteredPlayers() throws SQLException {
        String query = "SELECT PlayerName FROM Players";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        List<String> playerNameList = new ArrayList<>();
        while (resultSet.next()) {
            playerNameList.add(resultSet.getString("PlayerName").trim());
        }
        return playerNameList;
    }

    public static void UpdatePlayerPosition(String playerName, PlayerPosition playerPosition) {
        try {
            String query = "UPDATE Players SET LastPosition = ? WHERE PlayerName = ?";

            PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
            preparedStatement.setObject(1, Main.gson.toJson(playerPosition));
            preparedStatement.setString(2, playerName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void UpdateLastLoginIpAddress(ConnectedPlayer connectedPlayer) {
        try {
            String query = "UPDATE Players SET LastLoginIp = ? WHERE PlayerName = ?";

            PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
            preparedStatement.setObject(1, connectedPlayer.ipAddress.getHostAddress());
            preparedStatement.setString(2, connectedPlayer.playerName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
