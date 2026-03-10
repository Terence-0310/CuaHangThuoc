package components;

import javax.swing.table.DefaultTableModel;

/**
 * Table model cho SanPham
 * 
 * @author Generated
 * @version 1.0
 */
public class SanPhamTableModel extends DefaultTableModel {

	public SanPhamTableModel() {
		addColumn("Mã SP");
		addColumn("Tên sản phẩm");
		addColumn("Đơn vị tính");
		addColumn("Giá bán");
		addColumn("Loại");
		addColumn("Mô tả");
		addColumn("Mức tồn tối thiểu");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return switch (columnIndex) {
			case 0 -> Integer.class; // Mã SP
			case 1 -> String.class; // Tên sản phẩm
			case 2 -> String.class; // Đơn vị tính
			case 3 -> java.math.BigDecimal.class; // Giá bán
			case 4 -> String.class; // Loại
			case 5 -> String.class; // Mô tả
			case 6 -> Integer.class; // Mức tồn tối thiểu
			default -> String.class;
		};
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false; // Không cho edit trực tiếp trên table
	}

	/**
	 * Đổi tên cột động (dùng cho hiển thị mũi tên sắp xếp ▲ ▼)
	 */
	public void setColumnName(int columnIndex, String name) {
		if (columnIndex >= 0 && columnIndex < columnIdentifiers.size()) {
			columnIdentifiers.set(columnIndex, name);
		}
	}
}
