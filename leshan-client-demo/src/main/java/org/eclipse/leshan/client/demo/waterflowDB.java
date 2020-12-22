package org.eclipse.leshan.client.demo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class waterflowDB {
    private static final String SQL_between_A_B = "SELECT value from testdb WHERE timestamp BETWEEN ? AND ?";
    private static final String SQL_add_value = "INSERT INTO testdb (timestamp, value) VALUES (?,?)";

    public static void getWaterFlows(ResultSet rs) throws SQLException {
        
        while (rs.next()) {
            String values = rs.getTimestamp("datetime")+ " " + rs.getFloat("value");

            System.out.println(values);
        }
        
    }

    public static String getWaterFlowsBetweenA_B(Timestamp starttime, Timestamp stoptime) throws SQLException {
        
        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement stmt = con.prepareStatement(SQL_between_A_B, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ) {
            stmt.setTimestamp(1, starttime);
            stmt.setTimestamp(2, stoptime);
            try (ResultSet rs = stmt.executeQuery()) {
                
                String values = new String();
                while (rs.next()) {                    
                    values += rs.getFloat("value")+",";        
                }
                if (values != null && values.length() > 0 && values.charAt(values.length() - 1) == ',') {
                    values = values.substring(0, values.length() - 1);
                }
                return values;
            }
        } catch (SQLException e) {
            System.err.println(e);
            return null;
        }
    }
    
    public static void addnewmeasurement(Timestamp ts, Float value) throws SQLException {
        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement stmt = con.prepareStatement(SQL_add_value, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ) {
            stmt.setTimestamp(1,ts);
            stmt.setFloat(2, value);
            stmt.execute(); 
        } catch (SQLException e) {
            System.err.println(e);
        }
    }
}
