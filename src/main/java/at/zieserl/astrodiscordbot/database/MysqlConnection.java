package at.zieserl.astrodiscordbot.database;

import com.mysql.cj.jdbc.result.ResultSetImpl;
import net.dv8tion.jda.internal.JDAImpl;

import java.sql.*;
import java.util.Map;
import java.util.Optional;

public final class MysqlConnection {
    private Connection connection = null;
    private final String host;
    private final String port;
    private final String database;
    private final String user;
    private final String password;

    private MysqlConnection(String host, String port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public static MysqlConnection establish(String host, String port, String database, String user, String password) {
        MysqlConnection connection = new MysqlConnection(host, port, database, user, password);
        connection.openConnection();
        if (connection.isConnected()) {
            connection.testConnection();
            JDAImpl.LOG.info("Successfully connected to database!");
            return connection;
        } else {
            throw new RuntimeException("Could not connect to database!");
        }
    }

    public void disconnect() {
        closeConnection();
    }

    private void openConnection() {
        if (!host.isEmpty() && !database.isEmpty()) {
            try {
                if (connection != null && !connection.isClosed()) {
                    // Connection already attemted
                    return;
                }
                synchronized (this) {
                    if (connection != null && !connection.isClosed()) {
                        return;
                    }
                    connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?connectTimeout=3000&autoReconnect=true&useSSL=false", user, password);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                connection = null;
            }
        }
    }

    public boolean isConnected() {
        return (connection != null);
    }

    private void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connection = null;
    }

    private void testConnection() {
        try {
            Statement statement = connection.createStatement();
        } catch (SQLException ex) {
            closeConnection();
            throw new RuntimeException(ex);
        }
    }

    public Optional<ResultSet> executeQuery(String query, String... parameters) {
        if (isConnected()) {
            try {
                PreparedStatement statement = connection.prepareStatement(query);
                if (parameters.length > 0) {
                    for (int i = 0; i < parameters.length; i++) {
                        statement.setString(i + 1, parameters[i]);
                    }
                }
                if (query.toLowerCase().startsWith("update") || query.toLowerCase().startsWith("insert") || query.toLowerCase().startsWith("delete")) {
                    statement.execute();
                    return Optional.empty();
                } else {
                    return Optional.of(statement.executeQuery());
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        throw new RuntimeException("Can't execute query when no connection is established!");
    }

    public int executeInsertWithReturnNewID(String query, Map<Integer, String> parameters, String idField) {
        if (isConnected()) {
            try {
                String[] generatedColumns = {idField};
                PreparedStatement statement = connection.prepareStatement(query, generatedColumns);
                if (parameters != null && parameters.size() > 0) {
                    for (Map.Entry<Integer, String> param : parameters.entrySet()) {
                        statement.setString(param.getKey(), param.getValue());
                    }
                }
                statement.execute();
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        throw new RuntimeException("Can't execute insert when no connection is established!");
    }
}