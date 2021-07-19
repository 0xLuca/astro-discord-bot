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

    private MysqlConnection(final String host, final String port, final String database, final String user, final String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public static MysqlConnection establish(final String host, final String port, final String database, final String user, final String password) {
        final MysqlConnection connection = new MysqlConnection(host, port, database, user, password);
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
            } catch (final SQLException ex) {
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
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        connection = null;
    }

    private void testConnection() {
        try {
            final Statement statement = connection.createStatement();
        } catch (final SQLException ex) {
            closeConnection();
            throw new RuntimeException(ex);
        }
    }

    public Optional<ResultSet> executeQuery(final String query, final String... parameters) {
        if (isConnected()) {
            try {
                final PreparedStatement statement = connection.prepareStatement(query);
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
            } catch (final SQLException ex) {
                ex.printStackTrace();
            }
        }
        throw new RuntimeException("Can't execute query when no connection is established!");
    }

    public int executeInsertWithReturnNewID(final String idField, final String query, final String... parameters) {
        if (isConnected()) {
            try {
                final String[] generatedColumns = {idField};
                final PreparedStatement statement = connection.prepareStatement(query, generatedColumns);
                if (parameters.length > 0) {
                    for (int i = 0; i < parameters.length; i++) {
                        statement.setString(i + 1, parameters[i]);
                    }
                }
                statement.execute();
                final ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            } catch (final SQLException ex) {
                ex.printStackTrace();
            }
        }
        throw new RuntimeException("Can't execute insert when no connection is established!");
    }
}
