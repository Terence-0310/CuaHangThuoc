package components;

import javax.swing.table.DefaultTableModel;

/**
 * Table model cho NhaCungCap
 * 
 * @author Generated
 * @version 1.0
 */
public class NhaCungCapTableModel extends DefaultTableModel {

	public NhaCungCapTableModel() {
		addColumn("Mã NCC");
		addColumn("Tên NCC");
		addColumn("Số điện thoại");
		addColumn("Email");
		addColumn("Địa chỉ");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return switch (columnIndex) {
			case 0 -> Integer.class;  // Mã NCC
			case 1 -> String.class;   // Tên NCC
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
