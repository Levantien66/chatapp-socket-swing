
package com.raven.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private static final String SERVER = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE = "chatapp1";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "982606thu";
    private static final String CONNECTION_URL = "jdbc:mysql://" + SERVER + ":" + PORT + "/" + DATABASE;

    private DatabaseConnection() {
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public void connectToDatabase() {
        if (connection != null) {
            LOGGER.log(Level.INFO, "Already connected to the database.");
            return;
        }

        try {
            connection = DriverManager.getConnection(CONNECTION_URL, USERNAME, PASSWORD);
            LOGGER.log(Level.INFO, "Successfully connected to the database.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to the database.", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public static void main(String[] args) {
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();
        dbConnection.connectToDatabase();

        if (dbConnection.getConnection() != null) {
            System.out.println("Connection established.");
        } else {
            System.out.println("Failed to establish connection.");
        }
    }
}

