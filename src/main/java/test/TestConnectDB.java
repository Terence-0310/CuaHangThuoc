package test;

import java.sql.Connection;

import database.ConnectDB;

public class TestConnectDB {

	public static void main(String[] args) {
		Connection con = null;
		try {
//			con = DriverManager.getConnection(ConnectSQL.getURL());
			con = ConnectDB.getCon();
			System.out.println("Kết nối thành công");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
