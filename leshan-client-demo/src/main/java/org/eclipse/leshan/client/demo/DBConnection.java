package org.eclipse.leshan.client.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    
    // DATABASE details
    private static final String USERNAME = "nikhil";
    private static final String PASSWORD = "xylem123";
    private static final String CONN = "jdbc:mysql://localhost/xylem";

    public static Connection getConnection() throws SQLException {
        
        return DriverManager.getConnection(CONN, USERNAME, PASSWORD);   
    }
}
