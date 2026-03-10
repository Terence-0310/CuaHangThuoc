package components;

import javax.swing.table.DefaultTableModel;

/**
 * Table model cho NguoiDung
 * 
 * @author Generated
 * @version 1.0
 */
public class NguoiDungTableModel extends DefaultTableModel {

	public NguoiDungTableModel() {
		addColumn("Mã ND");
		addColumn("Tên đăng nhập");
		addColumn("Vai trò");
		addColumn("Họ tên");
		addColumn("Email");
		addColumn("Số điện thoại");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return switch (columnIndex) {
			case 0 -> Integer.class;  // Mã ND
			case 1 -> String.class;   // Tên đăng nhập
			case 2 -> String.class;   // Vai trò
			case 3 -> String.class;   // Họ tên
			case 4 -> String.class;   // Email
			case 5 -> String.class;   // Số điện thoại
			default -> String.class;
		};
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
