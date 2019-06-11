package ee.ut.mrz.kammkala.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.naming.NamingException;

public class DataBase {
	public static final int SWITCH_ID = 0;
	public static final int SWITCH_IP = 1;
	public static final int SWITCH_COMMUNITY = 2;
	public static final int SWITCH_LASTUPDATE = 3;
	public static final int SWITCH_STATUSSTRING = 4;
	public static final int SWITCH_NAME = 5;
	
	public static final int ROUTER_ID = 0;
	public static final int ROUTER_IP = 1;
	public static final int ROUTER_COMMUNITY = 2;
	public static final int ROUTER_LASTUPDATE = 3;
	public static final int ROUTER_STATUSSTRING = 4;
	public static final int ROUTER_NAME = 5;
	public static final int ROUTER_PASSWORD = 6;
	public static final int ROUTER_TYPE = 7;
	
	public static final int ROUTER_TYPE_SNMP = 1;
	public static final int ROUTER_TYPE_CISCOCLI = 2;
	
	private static Timestamp updated;
	private static List<String[]> cachedSwitches;
	private static List<String[]> cachedRouters;
	private static Timestamp routersUpdated;
	
	public static List<String[]> getSwitchesList() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		Connection conn = DataBaseConnection.getInstance().getConnection();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select * from switches");
		List<String[]> switches = new ArrayList<String[]>();
		while (rs.next()) {
			String[] switchinfo = new String[6];
			int id = rs.getInt("k_id");
			switchinfo[SWITCH_ID] = Integer.toString(id);
			String ip = rs.getString("k_ip");
			switchinfo[SWITCH_IP] = ip;
			String community = rs.getString("k_community");
			switchinfo[SWITCH_COMMUNITY] = community;
			Timestamp ts = rs.getTimestamp("lastupdate");
			if (ts != null) {
				switchinfo[SWITCH_LASTUPDATE] = ts.toString();
			} else {
				switchinfo[SWITCH_LASTUPDATE] = "never runned";
			}
			String status = rs.getString("statusstring");
			switchinfo[SWITCH_STATUSSTRING] = status;
			String name = rs.getString("name");
			switchinfo[SWITCH_NAME] = name;
			
			switches.add(switchinfo);
		}
		
		conn.close();
		
		Collections.sort(switches, new SwitchesByIPComparator());
		
		return switches;
	}
	
	public static void delSwitch(int id) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		Connection conn = DataBaseConnection.getInstance().getConnection();
		conn.setAutoCommit(false);
		PreparedStatement stmt = conn.prepareStatement("delete from switches where k_id = ?");
		stmt.setInt(1, id);
		stmt.executeUpdate();
		conn.commit();
		conn.close();
		deValidateSwitchesCache();
	}
	
	public static List<String[]> getSwitchesCached() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		if (updated == null || cachedSwitches == null) {
			updated = new Timestamp(System.currentTimeMillis());
			cachedSwitches = getSwitchesList();
			return cachedSwitches;
		}
		
		if (System.currentTimeMillis() - updated.getTime() > (10 * 1000)) {
			cachedSwitches = getSwitchesList();
			updated = new Timestamp(System.currentTimeMillis());
		}
		
		return cachedSwitches;
	}
	
	private static void deValidateSwitchesCache() {
		cachedSwitches = null;
	}
	
	public static void addSwitch(String ip, String community) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		Connection conn = DataBaseConnection.getInstance().getConnection();
		conn.setAutoCommit(false);
		PreparedStatement stmt = conn.prepareStatement("insert into switches (k_id, k_ip, k_community) select max(k_id)+1, ?, ? from switches");
		stmt.setString(1, ip);
		stmt.setString(2, community);
		stmt.executeUpdate();
		conn.commit();
		conn.close();
		deValidateSwitchesCache();		
	}


	public static List<String[]> getRoutersList() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		Connection conn = DataBaseConnection.getInstance().getConnection();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select * from routers");
		List<String[]> routers = new ArrayList<String[]>();
		while (rs.next()) {
			String[] routerinfo = new String[8];
			int id = rs.getInt("k_id");
			routerinfo[ROUTER_ID] = Integer.toString(id);
			String ip = rs.getString("k_ip");
			routerinfo[ROUTER_IP] = ip;
			String community = rs.getString("k_community");
			routerinfo[ROUTER_COMMUNITY] = community;
			Timestamp ts = rs.getTimestamp("lastupdate");
			if (ts != null) {
				routerinfo[ROUTER_LASTUPDATE] = ts.toString();
			} else {
				routerinfo[ROUTER_LASTUPDATE] = "never runned";
			}
			String status = rs.getString("statusstring");
			routerinfo[ROUTER_STATUSSTRING] = status;
			
			String name = rs.getString("name");			
			routerinfo[ROUTER_NAME] = name;
			
			String password = rs.getString("k_password");
			routerinfo[ROUTER_PASSWORD] = password;
			
			int type = rs.getInt("k_type");			
			routerinfo[ROUTER_TYPE] = Integer.toString(type);
			
			
			routers.add(routerinfo);
		}
		
		conn.close();
		
		Collections.sort(routers, new SwitchesByIPComparator());
		
		return routers;
	}

	public static List<String[]> getRoutersCached() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		if (routersUpdated == null || cachedRouters == null) {
			routersUpdated = new Timestamp(System.currentTimeMillis());
			cachedRouters = getRoutersList();
			return cachedRouters;
		}
		
		if (System.currentTimeMillis() - updated.getTime() > (10 * 1000)) {
			cachedRouters = getRoutersList();
			updated = new Timestamp(System.currentTimeMillis());
		}
		
		return cachedRouters;
	}
	
	private static void deValidateRoutersCache() {
		cachedRouters = null;
	}
	
	public static void addRouter(String ip, String community, String password, int type, int ipv4, int ipv6) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		Connection conn = DataBaseConnection.getInstance().getConnection();
		conn.setAutoCommit(false);
		PreparedStatement stmt = conn.prepareStatement("insert into routers (k_id, k_ip, k_community, k_password, k_type, k_ipv4, k_ipv6) select max(k_id)+1, ?, ?, ?, ?, ?, ? from routers");
		stmt.setString(1, ip);
		stmt.setString(2, community);
		stmt.setString(3, password);
		stmt.setInt(4, type);
		stmt.setInt(5, ipv4);
		stmt.setInt(6, ipv6);
		stmt.executeUpdate();
		conn.commit();
		conn.close();
		deValidateRoutersCache();		
	}
	
	public static void delRouter(int id) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		Connection conn = DataBaseConnection.getInstance().getConnection();
		conn.setAutoCommit(false);
		PreparedStatement stmt = conn.prepareStatement("delete from routers where k_id = ?");
		stmt.setInt(1, id);
		stmt.executeUpdate();
		conn.commit();
		conn.close();
		deValidateRoutersCache();
	}
	
	public static List<String> getTopologyTimes() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		Connection conn = DataBaseConnection.getInstance().getConnection();		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select time from topology order by time desc");
		List<String> ttimes = new ArrayList<String>();
		while (rs.next()) {
			Timestamp time = rs.getTimestamp("time");
			ttimes.add(time.toString());
		}
		conn.close();
		return ttimes;
	}
	
	public static String getTopology(String time) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, ParseException, NamingException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
		Date parsedDate = dateFormat.parse(time);
		java.sql.Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
		
		Connection conn = DataBaseConnection.getInstance().getConnection();
		PreparedStatement stmt = conn.prepareStatement("select topology from topology where time = ?");				
		stmt.setTimestamp(1, timestamp);
		
		ResultSet rs = stmt.executeQuery();
		rs.next();
		
		String topology = rs.getString("topology");
		conn.close();
		return topology;
		
	}

	public static List<List<String>> getSearchResult(Timestamp timestamp, String macText, String ipText, String nameText) throws SQLException, NamingException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Connection conn = DataBaseConnection.getInstance().getConnection();
		
		java.sql.Timestamp aproxtime = null;
		if (timestamp != null) {
			PreparedStatement stmt = conn.prepareStatement("select time from correlations where time <= ? order by time desc limit 1");
			stmt.setTimestamp(1, timestamp);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			java.sql.Timestamp lowtime = rs.getTimestamp("time");
			stmt = conn.prepareStatement("select time from correlations where time >= ? order by time asc limit 1");
			stmt.setTimestamp(1, timestamp);
			rs = stmt.executeQuery();
			rs.next();
			java.sql.Timestamp uptime = rs.getTimestamp("time");
			
			long updiff = uptime.getTime() - timestamp.getTime();
			long lowdiff = timestamp.getTime() - lowtime.getTime();
			if (updiff < lowdiff) {
				aproxtime = uptime;
			} else {
				aproxtime = lowtime;
			}
		}
		
		String querybase = "select time, mac, ip, switch, port from correlations where ";
		
		if (timestamp == null) {
			String queryadd = "time=(select max(time) from correlations) ";
			querybase = querybase + queryadd;
		} else {
			String queryadd = "time = ? ";
			querybase = querybase + queryadd;
		}
		
		if (macText != null) {
			querybase = querybase + "and mac='" + macText + "' ";
		}
		
		if (ipText != null) {
			querybase = querybase + "and ip='" + ipText + "' ";
		}
		
		if (nameText != null) {
			querybase = querybase + "and name='" + nameText + "' ";
		}
		
		PreparedStatement stmt = conn.prepareStatement(querybase);
		if (timestamp != null) {
			stmt.setTimestamp(1, aproxtime);
		}
		
		ResultSet rs = stmt.executeQuery();
		List<List<String>> ret = new ArrayList<List<String>>();
		
		while (rs.next()) {
			List<String> row = new ArrayList<String>();
			java.sql.Timestamp rtimestamp = rs.getTimestamp("time");
			String time = rtimestamp.toString();
			String ip = rs.getString("ip");
			String mac = rs.getString("mac");
			String sw = rs.getString("switch");
			String port = rs.getString("port");
			row.addAll(Arrays.asList(time, mac, ip, sw, port));
			ret.add(row);
		}
		conn.close();
		return ret;
	}
	
	
}

class SwitchesByIPComparator implements Comparator<String[]> {

	@Override
	public int compare(String[] o1, String[] o2) {
		if (o1[DataBase.SWITCH_IP].matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && o2[DataBase.SWITCH_IP].matches("\\d+\\.\\d+\\.\\d+\\.\\d+") ) {
			String[] o1s = o1[DataBase.SWITCH_IP].split("\\.");
			String[] o2s = o2[DataBase.SWITCH_IP].split("\\.");
			for (int c = 0; c <= 3; c++) {
				int o1i = Integer.parseInt(o1s[c]);
				int o2i = Integer.parseInt(o2s[c]);
				if (o1i > o2i) {
					return 1;
				} else if (o1i < o2i) {
					return -1;
				}
			}
			return 0;
		}
		
		return o1[DataBase.SWITCH_IP].compareTo(o2[DataBase.SWITCH_IP]);
	}
	
}
