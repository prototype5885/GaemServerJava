package org.ProToType.Instanceables;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.ProToType.Classes.ConnectedPlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final Logger logger = LogManager.getLogger(Database.class);

    public Connection dbConnection;

    public void ConnectToDatabase(ConfigFile configFile) throws Exception {
        String dbType;
        String dbName;
        String dbUrl;
        String query;

        String ipAddress;
        String port;

        String username;
        String password;

        switch (configFile.databaseType) {
            case "h2":
                logger.debug("Connecting to local H2 database...");
                Class.forName("org.h2.Driver");

                dbType = "jdbc:h2:";
                dbName = "./GaemServerDatabase.h2";
                dbUrl = dbType + dbName;

                dbConnection = DriverManager.getConnection(dbUrl);

                query = InitialQuery("h2");
                dbConnection.createStatement().executeUpdate(query);

                logger.info("Connected to local H2 database successfully");
                return;
            case "h2server":
                logger.debug("Connecting to H2 server database...");
                Class.forName("org.h2.Driver");

                org.h2.tools.Server.createTcpServer("-tcp", "-ifNotExists").start();

                dbType = "jdbc:h2:tcp://localhost/";
                dbName = "./GaemServerDatabase.h2";
                dbUrl = dbType + dbName;

                dbConnection = DriverManager.getConnection(dbUrl);

                query = InitialQuery("h2");
                dbConnection.createStatement().executeUpdate(query);

                logger.info("Connected to H2 server database successfully");
                return;
            case "sqlite":
                logger.debug("Connecting to SQLite database...");
                Class.forName("org.sqlite.JDBC");

                dbType = "jdbc:sqlite:";
                dbName = "GaemServerDatabase.sqlite";
                dbUrl = dbType + dbName;

                dbConnection = DriverManager.getConnection(dbUrl);

                query = InitialQuery("sqlite");
                dbConnection.createStatement().executeUpdate(query);

                logger.info("Connected to SQLite database successfully");
                break;
            case "mysql":
                logger.debug("Connecting to MySQL database...");
//                Class.forName("com.mysql.cj.jdbc.Driver");

                ipAddress = configFile.remoteDatabaseIpAddress;
                port = configFile.remoteDatabasePort;

                dbType = "jdbc:mysql://";
                dbName = "/GaemServer";
                dbUrl = dbType + ipAddress + ":" + port + dbName;

                username = configFile.dbUsername;
                password = configFile.dbPassword;

                dbConnection = DriverManager.getConnection(dbUrl, username, password);

                query = InitialQuery("mysql");

                dbConnection.createStatement().executeUpdate(query);
                logger.info("Connected to MySQL database successfully");
                break;
            case "mariadb":
                logger.debug("Connecting to MariaDB database...");
//                Class.forName("com.mysql.cj.jdbc.Driver");

                ipAddress = configFile.remoteDatabaseIpAddress;
                port = configFile.remoteDatabasePort;

                dbType = "jdbc:mariadb://";
                dbName = "/GaemServer";
                dbUrl = dbType + ipAddress + ":" + port + dbName;

                username = configFile.dbUsername;
                password = configFile.dbPassword;

                dbConnection = DriverManager.getConnection(dbUrl, username, password);

                query = InitialQuery("mariadb");

                dbConnection.createStatement().executeUpdate(query);
                logger.info("Connected to MariaDB database successfully");
                break;
            default:
                String errorMsg = String.format("Database type - %s - could not have been loaded", configFile.databaseType);
                logger.fatal(errorMsg);
                throw new Exception(errorMsg);
        }
    }

    private String InitialQuery(String dbType) {
        logger.debug("Initial query to database, database type: {}", dbType);

        String autoIncrement = "AUTOINCREMENT";

        if (!dbType.equals("sqlite")) {
            autoIncrement = "AUTO_INCREMENT";
        }

        return "CREATE TABLE IF NOT EXISTS Players " +
                "(ID INTEGER PRIMARY KEY " + autoIncrement + "," +
                "PlayerName TEXT," +
                "Password CHAR(64)," +
                "Wage INTEGER," +
                "Money INTEGER," +
                "LastLoginIp TEXT," +
                "LastPosition TEXT)";
    }

    public void RegisterPlayer(String playerName, String hashedPassword) throws SQLException {
        logger.debug("Adding player {} into the database...", playerName);
        String query = "INSERT INTO Players (PlayerName, Password, Wage, Money) VALUES (?, ?, ?, ?)";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        preparedStatement.setString(1, playerName);
        // preparedStatement.setString(2, BCrypt.withDefaults().hashToString(10,
        // hashedPassword.toCharArray()));
        preparedStatement.setString(2, hashedPassword);
        preparedStatement.setInt(3, 3);
        preparedStatement.setInt(4, 1010);
        preparedStatement.execute();

        logger.info("Player {} has been added to the database", playerName);
    }

    public ResultSet SearchForPlayerInDatabase(String playerName) throws SQLException {
        logger.debug("Searching for player {} in the database...", playerName);
        String query = "SELECT * FROM Players WHERE PlayerName = ?";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        preparedStatement.setString(1, playerName);

        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            logger.debug("Found player {} in the database", playerName);
            return resultSet; // returns if found
        }
        logger.debug("Player {} was not found in the database", playerName);
        return null;
    }

    public List<String> ListAllRegisteredPlayers() throws SQLException {
        String query = "SELECT PlayerName FROM Players";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        List<String> playerNameList = new ArrayList<>();
        while (resultSet.next()) {
            playerNameList.add(resultSet.getString("PlayerName").trim());
        }
        return playerNameList;
    }

    public void UpdatePlayerPosition(String playerName, String jsonPlayerPosition) throws SQLException {
        String query = "UPDATE Players SET LastPosition = ? WHERE PlayerName = ?";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        preparedStatement.setObject(1, jsonPlayerPosition);
        preparedStatement.setString(2, playerName);
        preparedStatement.executeUpdate();
    }

    public void UpdateLastLoginIpAddress(ConnectedPlayer connectedPlayer) throws SQLException {
        logger.debug("Updating last login ip address of player {} to {}...", connectedPlayer.playerName, connectedPlayer.ipAddress.getHostAddress());
        String query = "UPDATE Players SET LastLoginIp = ? WHERE PlayerName = ?";

        PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
        preparedStatement.setObject(1, connectedPlayer.ipAddress.getHostAddress());
        preparedStatement.setString(2, connectedPlayer.playerName);
        preparedStatement.executeUpdate();
        logger.debug("Last login ip address of player {} was updated successfully", connectedPlayer.playerName);
    }
}
