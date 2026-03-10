package components;

import javax.swing.table.DefaultTableModel;

/**
 * Table model cho KhachHang
 * 
 * @author Generated
 * @version 1.0
 */
public class KhachHangTableModel extends DefaultTableModel {

	public KhachHangTableModel() {
		addColumn("Mã KH");
		addColumn("Họ tên");
		addColumn("Số điện thoại");
		addColumn("Email");
		addColumn("Địa chỉ");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return switch (columnIndex) {
			case 0 -> Integer.class;  // Mã KH
			case 1 -> String.class;   // Họ tên
			case 2 -> String.class;   // Số điện thoại
			case 3 -> String.class;   // Email
			case 4 -> String.class;   // Địa chỉ
			default -> String.class;
		};
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
