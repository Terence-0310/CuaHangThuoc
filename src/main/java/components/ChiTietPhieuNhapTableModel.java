package components;

import javax.swing.table.DefaultTableModel;

/**
 * Table model cho ChiTietPhieuNhap
 * 
 * @author Generated
 * @version 1.0
 */
public class ChiTietPhieuNhapTableModel extends DefaultTableModel {

	public ChiTietPhieuNhapTableModel() {
		addColumn("Mã SP");
		addColumn("Tên sản phẩm");
		addColumn("Mã số lô hàng");
		addColumn("Hạn sử dụng");
		addColumn("Giá nhập");
		addColumn("Số lượng");
		addColumn("Tổng giá trị mục");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return switch (columnIndex) {
			case 0 -> Integer.class;  // Mã SP
			case 1 -> String.class;   // Tên sản phẩm
			case 2 -> String.class;   // Số lô
			case 3 -> java.time.LocalDate.class; // Hạn sử dụng
			case 4 -> java.math.BigDecimal.class; // Giá nhập
			case 5 -> Integer.class;  // Số lượng
			case 6 -> java.math.BigDecimal.class; // Thành tiền
			default -> String.class;
		};
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
