package ee.ut.mrz.kammkala.web;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class DataBaseConnection {

	private static DataBaseConnection dbconn;
	private DataSource dataSource;

	private DataBaseConnection() throws NamingException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException {

		InitialContext context = new InitialContext();
		dataSource = (DataSource) context.lookup("java:comp/env/jdbc/kammkala");

	}

	private DataBaseConnection(int standalone) throws NamingException,
			NameNotFoundException, InstantiationException,
			IllegalAccessException, ClassNotFoundException {

	}

	/**
	 * Database connection from application server.
	 * 
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static DataBaseConnection getInstance() throws NamingException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException {
		if (dbconn == null) {
			dbconn = new DataBaseConnection();
		}
		return dbconn;
	}

	public Connection getConnection() throws SQLException {

		Connection conn = dataSource.getConnection();
		conn.setAutoCommit(false);
		return conn;

	}

	public void generateSchema() throws SQLException {
		Connection conn = getConnection();

		conn.createStatement()
				.executeUpdate(
						"create table configuration (k_option varchar(128), k_value varchar(128))");
		conn.createStatement()
				.executeUpdate(
						"create table routers (k_ip varchar(128), k_community varchar(128), k_password varchar(128), k_id bigint, k_type int, k_ipv4 int, k_ipv6 int)");
		conn.createStatement()
				.executeUpdate(
						"create table switches (k_id bigint, k_ip varchar(128), k_community varchar(128))");

		conn.commit();
		conn.close();
	}

	public boolean checkSchema() { // TODO: it should be more deep check. Later.
		try {
			Connection conn = getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt
					.executeQuery("select k_option, k_value from configuration");
			rs.close();
			stmt.close();

			conn.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

}
