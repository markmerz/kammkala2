package ee.ut.merzin.kammkala2.controller;

import ee.ut.merzin.kammkala2.driver.Device;
import ee.ut.merzin.kammkala2.driver.Router;
import ee.ut.merzin.kammkala2.driver.RouterCiscoCLI;
import ee.ut.merzin.kammkala2.driver.RouterSNMP;
import ee.ut.merzin.kammkala2.driver.Switch;
import ee.ut.merzin.kammkala2.driver.SwitchCiscoCli;
import ee.ut.merzin.kammkala2.driver.SwitchSNMP;
import ee.ut.merzin.kammkala2.helpers.KammkalaLogger;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages device (switches and routers) statuses. Reads device descriptions from
 * database and writes possible errosr back to database. Also keeps track of running
 * times of information gathering threads.
 * @author markko
 */
public class DeviceController {

    private static List<Router> currentRouters;

    /** Reads router descriptions from database and returns List of instances. */
    public static List<Router> getRouters() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        Connection conn = DataBaseConnection.getInstance().getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from routers order by runningtime desc");
        List<Router> routers = new ArrayList<Router>();
        while (rs.next()) {
            int type = rs.getInt("k_type");
            int id = rs.getInt("k_id");
            String ip = rs.getString("k_ip");
            String community = rs.getString("k_community");
            String password = rs.getString("k_password");
            int readmymacs = rs.getInt("readmymacs");
            int ipv4 = rs.getInt("k_ipv4");
            int ipv6 = rs.getInt("k_ipv6");

            Router rr = null;
            if (type == Router.TYPE_SNMP) {
                rr = new RouterSNMP(ip, community);
            } else if (type == Router.TYPE_CISCOCLI) {
                rr = new RouterCiscoCLI(ip, community, password);
                if (ipv6 > 0) {
                    rr.setIpv6(true);
                }
                if (ipv4 > 0) {
                    rr.setIpv4(true);
                }
            }

            rr.setId(id);

            routers.add(rr);
        }

        return routers;
    }

    /** Reads switch descriptions from database and returns List of instances. */
    public static List<Switch> getSwitches() throws Exception {
        Connection conn = DataBaseConnection.getInstance().getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from switches order by runningtime desc");
        List<Switch> switches = new ArrayList<Switch>();
        while (rs.next()) {
            int id = rs.getInt("k_id");
            String ip = rs.getString("k_ip");
            String community = rs.getString("k_community");
            int type = rs.getInt("k_type");
            String password = rs.getString("k_password");
            Switch sr = null;
            if (type == 0 || type == Switch.TYPE_SNMP) {
                sr = new SwitchSNMP(ip, community);
            } else if (type == Switch.TYPE_CISCOCLI) {
                sr = new SwitchCiscoCli(ip, community, password);
            }
            sr.setId(id);

            switches.add(sr);

        }

        // conn.close();

        return switches;
    }

    /** Updates routers runningtime statuses to database. */
    public static void updateRouterStatus(List<Router> devices) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Connection conn = DataBaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        PreparedStatement pstmt_router = conn.prepareStatement("update routers set name=?, statusstring=?, lastupdate=?, runningtime=? where k_id=?");

        Iterator<Router> di = devices.iterator();
        while (di.hasNext()) {
            Router d = di.next();
            String status;
            String name = d.getName();
            int id = d.getId();
            long runningTime = d.getRunningTime();
            Timestamp date = new Timestamp(System.currentTimeMillis());
            if (d.getStatus() == Device.STATUS_RESULT_READY) {
                status = "OK";
            } else {
                status = d.getStatusString();
            }

            pstmt_router.setString(1, name);
            pstmt_router.setString(2, status);
            pstmt_router.setTimestamp(3, date);
            pstmt_router.setLong(4, runningTime);
            pstmt_router.setInt(5, id);
            pstmt_router.executeUpdate();           

        }

        conn.commit();
        
    }

    /** Updates switches runningtime statuses to database. */
    public static void updateSwitchStatus(List<Switch> devices) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Connection conn = DataBaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);

        PreparedStatement pstmt_switch = conn.prepareStatement("update switches set name=?, statusstring=?, lastupdate=?, runningtime=? where k_id=?");

        Iterator<Switch> di = devices.iterator();
        while (di.hasNext()) {
            Switch d = di.next();
            String status;
            String name = d.getName();
            int id = d.getId();
            long runningTime = d.getRunningTime();
            Timestamp date = new Timestamp(System.currentTimeMillis());
            if (d.getStatus() == Device.STATUS_RESULT_READY) {
                status = "OK";
            } else {
                status = d.getStatusString();
            }

            pstmt_switch.setString(1, name);
            pstmt_switch.setString(2, status);
            pstmt_switch.setTimestamp(3, date);
            pstmt_switch.setLong(4, runningTime);
            pstmt_switch.setInt(5, id);
            pstmt_switch.executeUpdate();

        }

        conn.commit();


    }
    
    /** Returns lists of banned vlans which are excluded from topology calculations. */
    public static List<String> getBannedVLANs() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        List<String> result = new ArrayList();
        Connection conn = DataBaseConnection.getInstance().getConnection();
        PreparedStatement pstmt = conn.prepareStatement("select k_value from configuration where k_option=?");
        pstmt.setString(1, "badvlan");
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            String vlan = rs.getString("k_value");
            result.add(vlan);
        }
        return result;
    }

    /** Pings devices which ip-addresses are in list. */
    public static void pingDevices(List ds) {
        try {
            ExecutorService pool = Executors.newFixedThreadPool(20);

            Iterator di = ds.iterator();
            while (di.hasNext()) {
                Device d = (Device) di.next();
                Pinger p = new Pinger(d);
                pool.execute(p);
            }

            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
        }

    }

    public static void setCurrentRouters(List<Router> rs) {
        DeviceController.currentRouters = rs;
    }

    public static List<Router> getCurrentRouter() {
        return DeviceController.currentRouters;
    }


}

class Pinger implements Runnable {

    private Device d;

    public Pinger(Device d) {
        this.d = d;
    }

    @Override
    public void run() {
        try {
            InetAddress address = InetAddress.getByName(d.getIp());
            address.isReachable(3000);
        } catch (Exception ex) {
            KammkalaLogger.log(KammkalaLogger.WARNING, Pinger.class.getName(), ex);
        }
    }
}