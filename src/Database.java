import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    public Connection Connect() {
        Connection connection = null;
        try {
            String url = "jdbc:sqlite:sample.db";
            connection = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");

            CreateTable(connection);

            return connection;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
        return null;
    }

    private void CreateTable(Connection dbConnection) throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS Players " +
                "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Username TEXT, " +
                "Password TEXT, " +
                "Wage INTEGER, " +
                "Money INTEGER)";
        dbConnection.createStatement().execute(query);
    }
}
