import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    public Connection dbConnection;

    public Database() {
        try {
            String url = "jdbc:h2:./GaemServerDatabase";
            dbConnection = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");
            CreateTable();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void CreateTable() {
        String query = "CREATE TABLE IF NOT EXISTS Players " +
                "(ID INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "PlayerName TEXT, " +
                "Password TEXT, " +
                "Wage INTEGER, " +
                "Money INTEGER)";
        try {
            dbConnection.createStatement().execute(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void AddPlayer(String username, String password) {
        String query = "INSERT INTO Players (PlayerName, Password, Wage, Money) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            preparedStatement.setInt(3, 3);
            preparedStatement.setInt(4, 1010);
            preparedStatement.execute();
            System.out.println("Player " + username + " has been added to the database");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> ListAllRegisteredPlayers() {
        String query = "SELECT PlayerName FROM Players";
        try {
            PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            List<String> playerNameList = new ArrayList<>();
            while (resultSet.next()) {
                playerNameList.add(resultSet.getString("PlayerName"));
            }
            return playerNameList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
