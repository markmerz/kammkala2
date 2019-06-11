/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ee.ut.merzin.kammkala2.helpers;

import ee.ut.merzin.kammkala2.controller.DataBaseConnection;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 *
 * @author markko
 */
public class Config {
    private static Config config = null;
   
    private final Properties prop;
    
    private Config(Properties prop) {
        this.prop = prop;
    }
    
    public static void loadBootstrapConfig(String filename) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        InputStream is = new FileInputStream(filename);
        prop.load(is);
        config = new Config(prop);
    }
    

    public static Config getConfig() {
        return config;
    }
    
    public Properties getProperties() {
        return prop;
    }

    public void loadConfig() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        Connection conn = DataBaseConnection.getInstance().getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select k_option, k_value from configuration");
        while (rs.next()) {
            String option = rs.getString("k_option");
            String value = rs.getString("k_value");
            prop.setProperty(option, value);
        }
        
        String loglevels = prop.getProperty("kammkala.loglevel");
        if (loglevels != null) {
            try {
                int llevel = Integer.parseInt(loglevels);
                KammkalaLogger.loglevel = llevel;
            } catch (NumberFormatException e) {
                KammkalaLogger.loglevel = KammkalaLogger.WARNING;
            }
        }
    }
}
