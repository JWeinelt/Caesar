package de.julianweinelt.caesar.core.storage;

import de.julianweinelt.caesar.core.util.logging.Log;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLStorageProvider extends StorageProvider{
    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void generateTables() {
         try {
             checkConnection();

             String sql1 = "CREATE TABLE caesar_users (" +
                     "                       id            INT AUTO_INCREMENT PRIMARY KEY," +
                     "                       username      VARCHAR(50) UNIQUE NOT NULL," +
                     "                       password_hash VARCHAR(255) NOT NULL," +
                     "                       created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ");";
             String sql2 = "CREATE TABLE caesar_roles (" +
                     "                       id   INT AUTO_INCREMENT PRIMARY KEY," +
                     "                       name VARCHAR(50) UNIQUE NOT NULL" +
                     ");";
             String sql3 = "CREATE TABLE caesar_permissions (" +
                     "id UUID PRIMARY KEY," +
                     "name VARCHAR(255) UNIQUE NOT NULL" +
                     ");";
             String sql4 = "CREATE TABLE caesar_user_roles (" +
                     "user_id UUID REFERENCES caesar_users(id) ON DELETE CASCADE," +
                     "role_id UUID REFERENCES caesar_roles(id) ON DELETE CASCADE," +
                     "PRIMARY KEY (user_id, role_id)" +
                     ");";
             String sql5 = "CREATE TABLE caesar_role_permissions (" +
                     "role_id UUID REFERENCES caesar_roles(id) ON DELETE CASCADE," +
                     "permission_id UUID REFERENCES caesar_permissions(id) ON DELETE CASCADE," +
                     "PRIMARY KEY (role_id, permission_id)" +
                     ");";
             Statement stmt = conn.createStatement();
             stmt.addBatch(sql1);
             stmt.addBatch(sql2);
             stmt.addBatch(sql3);
             stmt.addBatch(sql4);
             stmt.addBatch(sql5);
             stmt.executeBatch();
         } catch (SQLException e) {
            Log.warn(e.getMessage());
         }
     }


    @Override
    public void loadUserWithPermissions() {
        try {
            checkConnection();

            PreparedStatement pS = conn.prepareStatement("SELECT ");
        } catch (SQLException e) {
            log.warn(e.getMessage());
        }
    }
}
