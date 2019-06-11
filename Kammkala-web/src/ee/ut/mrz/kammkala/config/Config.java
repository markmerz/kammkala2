package ee.ut.mrz.kammkala.config;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import ee.ut.mrz.kammkala.web.DataBaseConnection;

public class Config {

	private static Config instance;
	private int switchpollinterval = 30; // seconds
	private int routerpollinterval = 30; // seconds
	private int delaybetweenthreads = 500; // ms
	private int switchtimeout = 1200; // seconds
	private int routertimeout = 86400; // seconds
	private int scriptport = 6666;
	private int humanport = 5555;

	public static Config getInstance() {
		if (instance == null) {
			instance = new Config();
		}
		return instance;
	}

	public Config() {
		loadConfig();
	}

	public void setVariable(String param, String value)
			throws NumberFormatException {

		if (param != null && value != null) {
			int val = Integer.parseInt(value);

			if (param.equalsIgnoreCase("switchpollinterval")) {
				this.switchpollinterval = val;
			} else if (param.equalsIgnoreCase("routerpollinterval")) {
				this.routerpollinterval = val;
			} else if (param.equalsIgnoreCase("delaybetweenthreads")) {
				this.delaybetweenthreads = val;
			} else if (param.equalsIgnoreCase("switchtimeout")) {
				this.switchtimeout = val;
			} else if (param.equalsIgnoreCase("routertimeout")) {
				this.routertimeout = val;
			} else if (param.equalsIgnoreCase("scriptport")) {
				this.scriptport = val;
			} else if (param.equalsIgnoreCase("humanport")) {
				this.humanport = val;
			}				
			
		}
	}

	/**
	 * @return the switchpollinterval
	 */
	public int getSwitchpollinterval() {
		return switchpollinterval;
	}

	/**
	 * @return the routerpollinterval
	 */
	public int getRouterpollinterval() {
		return routerpollinterval;
	}

	/**
	 * @return the delaybetweenthreads
	 */
	public int getDelaybetweenthreads() {
		return delaybetweenthreads;
	}

	/**
	 * @return the switchtimeout
	 */
	public int getSwitchtimeout() {
		return switchtimeout;
	}

	/**
	 * @return the routertimeout
	 */
	public int getRoutertimeout() {
		return routertimeout;
	}

	public int getScriptport() {
		return scriptport;
	}

	public int getHumanport() {
		return humanport;
	}

	public void saveConfig() throws Exception {
		Connection conn = DataBaseConnection.getInstance().getConnection();
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("delete from configuration");

		PreparedStatement pstmt = conn
				.prepareStatement("insert into configuration (k_option, k_value) values (?, ?)");

		Method[] m = getClass().getDeclaredMethods();
		for (int c = 0; c < m.length; c++) {
			String mname = m[c].getName();
			if (mname.startsWith("get") && !mname.startsWith("getInstance")) {

				String paramname = mname.replaceFirst("get", "").toLowerCase();

				if (m[c].getReturnType().equals(Integer.TYPE)) {
					Integer paramvalue = (Integer) m[c].invoke(this,
							(Object[]) null);
					pstmt.setString(1, paramname);
					pstmt.setString(2, paramvalue.toString());
					pstmt.executeUpdate();
				} else if (m[c].getReturnType().equals(String.class)) {
					String paramvalue = (String) m[c].invoke(this,
							(Object[]) null);
					pstmt.setString(1, paramname);
					pstmt.setString(2, paramvalue);
					pstmt.executeUpdate();
				}
			}
		}

		conn.commit();
		conn.close();
	}

	private void loadConfig() {

		try {
			Connection conn = DataBaseConnection.getInstance().getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt
					.executeQuery("select k_option, k_value from configuration");
			while (rs.next()) {
				String option = rs.getString("k_option");
				String value = rs.getString("k_value");
				setVariable(option, value);
			}
		} catch (SQLException e) {

		} catch (InstantiationException e) {
			
		} catch (IllegalAccessException e) {
			
		} catch (ClassNotFoundException e) {
			
		} catch (NamingException e) {
			
		}
	}

}
