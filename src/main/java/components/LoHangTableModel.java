package components;

import javax.swing.table.DefaultTableModel;

/**
 * Table model cho LoHang
 * 
 * @author Generated
 * @version 1.0
 */
public class LoHangTableModel extends DefaultTableModel {

	public LoHangTableModel() {
		addColumn("Mã lô");
		addColumn("Mã SP");
		addColumn("Số lô");
		addColumn("Hạn sử dụng");
		addColumn("Giá nhập");
		addColumn("SL nhập");
		addColumn("SL tồn");
		addColumn("Trạng thái");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return switch (columnIndex) {
			case 0 -> Integer.class;  // Mã lô
			case 1 -> Integer.class;  // Mã SP
			case 2 -> String.class;   // Số lô
			case 3 -> java.time.LocalDate.class; // Hạn sử dụng
			case 4 -> java.math.BigDecimal.class; // Giá nhập
			case 5 -> Integer.class;  // SL nhập
			case 6 -> Integer.class;  // SL tồn
			case 7 -> String.class;   // Trạng thái
			default -> String.class;
		};
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
