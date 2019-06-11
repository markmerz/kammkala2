package ee.ut.merzin.kammkala2.controller;

import ee.ut.merzin.kammkala2.helpers.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Handles connections to database.
 * @author markko
 */
public class DataBaseConnection {

    private static DataBaseConnection dbconn;
    private String connectionString;
    private String dbuser;
    private String dbpassword;
    
    private DataBaseConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        String dbdriver = Config.getConfig().getProperties().getProperty("kammkala.dbdriver");
        Class.forName(dbdriver).newInstance();
        this.connectionString = Config.getConfig().getProperties().getProperty("kammkala.dbstring");
        this.dbuser = Config.getConfig().getProperties().getProperty("kammkala.dbuser");
        this.dbpassword = Config.getConfig().getProperties().getProperty("kammkala.dbpassword");
    }

    /** Returns singleton instance. */
    public static synchronized DataBaseConnection getInstance() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        if (dbconn == null) {
            dbconn = new DataBaseConnection();
        }
        return dbconn;
    }

    /** Returns collention to database. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionString, dbuser, dbpassword);
    }


}
