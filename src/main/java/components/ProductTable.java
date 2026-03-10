package components;

import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

public class ProductTable extends DefaultTableModel {

	public ProductTable() {
		addColumn("proid");
		addColumn("proname");
		addColumn("prostatus");
		addColumn("promfg");
		addColumn("proimg");
	}

	// sửa lại cột theo đúng kiểu dữ liệu

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return switch (columnIndex) {
			case 0 -> Integer.class; //id = kieu int
			case 1 -> String.class;  //name = kieu string
			case 2 -> Boolean.class; //status = kieu boolean
			case 3 -> String.class;  //mfg = kieu string
			case 4 -> ImageIcon.class;  //img = kieu string
			default -> String.class;
		};
	}

}








