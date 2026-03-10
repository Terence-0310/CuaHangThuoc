package dao;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.NhaCungCap;

/**
 * DAO class cho NhaCungCap
 * 
 * @author Generated
 * @version 1.0
 */
public class NhaCungCapDao {

	public List<NhaCungCap> getAll() {
		List<NhaCungCap> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var stmt = con.createStatement();
			var rs = stmt.executeQuery(
				"SELECT * FROM NhaCungCap WHERE DaXoa = 0 ORDER BY TenNCC"
			);
		) {
			while (rs.next()) {
				list.add(mapResultSet(rs));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public NhaCungCap findById(int maNCC) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement("SELECT * FROM NhaCungCap WHERE MaNCC = ? AND DaXoa = 0");
		) {
			ps.setInt(1, maNCC);
			var rs = ps.executeQuery();
			if (rs.next()) {
				return mapResultSet(rs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean insert(NhaCungCap ncc) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"INSERT INTO NhaCungCap(TenNCC, SoDienThoai, Email, DiaChi) VALUES (?, ?, ?, ?)"
			);
		) {
			ps.setString(1, ncc.getTenNCC());
			ps.setString(2, ncc.getSoDienThoai());
			ps.setString(3, ncc.getEmail());
			ps.setString(4, ncc.getDiaChi());
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean update(NhaCungCap ncc) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"UPDATE NhaCungCap SET TenNCC=?, SoDienThoai=?, Email=?, DiaChi=? WHERE MaNCC=?"
			);
		) {
			ps.setString(1, ncc.getTenNCC());
			ps.setString(2, ncc.getSoDienThoai());
			ps.setString(3, ncc.getEmail());
			ps.setString(4, ncc.getDiaChi());
			ps.setInt(5, ncc.getMaNCC());
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean delete(int maNCC) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement("UPDATE NhaCungCap SET DaXoa=1 WHERE MaNCC=?");
		) {
			ps.setInt(1, maNCC);
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private NhaCungCap mapResultSet(ResultSet rs) throws Exception {
		var ncc = new NhaCungCap();
		ncc.setMaNCC(rs.getInt("MaNCC"));
		ncc.setTenNCC(rs.getString("TenNCC"));
		ncc.setSoDienThoai(rs.getString("SoDienThoai"));
		ncc.setEmail(rs.getString("Email"));
		ncc.setDiaChi(rs.getString("DiaChi"));
		ncc.setDaXoa(rs.getBoolean("DaXoa"));
		if (rs.getTimestamp("NgayTao") != null) {
			ncc.setNgayTao(rs.getTimestamp("NgayTao").toLocalDateTime());
		}
		return ncc;
	}
}
