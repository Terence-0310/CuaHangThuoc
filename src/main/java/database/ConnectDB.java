package database;


import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectDB {
	private static String URL = "jdbc:sqlserver://";
	private static String SERVERNAME = "GIABAO123\\MSSQLSERVER02";
	private static String PORT = "1433";
	private static String DATABASENAME = "CuaHangThuoc_Batch";
	private static String USERNAME = "sa";
	private static String PASSWORD = "123456";
	private static String SECURITY = "encrypt=true;trustServerCertificate=true;";
	private static Connection con = null;
	
	public static String getURL() {
		return URL 
				+ SERVERNAME + ":" 
				+ PORT + ";databaseName=" 
				+ DATABASENAME + ";user=" 
				+ USERNAME + ";password="
				+ PASSWORD + ";" 
				+ SECURITY;
	}
	public static Connection getCon() {
		try {
			con = DriverManager.getConnection(getURL());
		} catch (Exception e) {
			e.printStackTrace();
			con = null;
		}
		return con;
	}
} 
