package dao;

import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.Product;

public class ProductDao {

	public int countProduct() {
		var count = 0;
		try(
			var con = ConnectDB.getCon();
			var cs = con.prepareCall("{call countpro()}");
			var rs = cs.executeQuery();
			) {
			while(rs.next()) {
				count = rs.getInt("total");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return count;
	}

	public List<Product> showProducts(int pagenumber, int rowofpage) {
		List<Product> list = new ArrayList<>();

		try(
			var con = ConnectDB.getCon();
			var cs = con.prepareCall("{call selectpro(?,?)}");
			) {
			cs.setInt(1, pagenumber);
			cs.setInt(2, rowofpage);
			var rs = cs.executeQuery(); //tập kết quả resulset trả về
			while(rs.next()) {
				var pro = new Product();
				pro.setProid(rs.getInt("proid"));
				pro.setProname(rs.getString("proname"));
				pro.setProstatus(rs.getBoolean("prostatus"));
				pro.setPromfg(rs.getDate("promfg").toLocalDate());
				pro.setProimg(rs.getString("proimg"));
				list.add(pro);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}
}













