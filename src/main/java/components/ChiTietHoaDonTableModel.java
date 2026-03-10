package components;

import javax.swing.table.DefaultTableModel;

/**
 * Table model cho ChiTietHoaDon
 * 
 * @author Generated
 * @version 1.0
 */
public class ChiTietHoaDonTableModel extends DefaultTableModel {

	public ChiTietHoaDonTableModel() {
		addColumn("Mã SP");
		addColumn("Tên sản phẩm");
		addColumn("Số lô");
		addColumn("Số lượng");
		addColumn("Giá bán");
		addColumn("Thành tiền");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return switch (columnIndex) {
			case 0 -> Integer.class;  // Mã SP
			case 1 -> String.class;   // Tên sản phẩm
			case 2 -> String.class;   // Số lô
			case 3 -> Integer.class;  // Số lượng
			case 4 -> java.math.BigDecimal.class; // Giá bán
			case 5 -> java.math.BigDecimal.class; // Thành tiền
			default -> String.class;
		};
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
