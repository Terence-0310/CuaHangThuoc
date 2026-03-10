package panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JFormattedTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.MaskFormatter;
import javax.swing.DefaultListCellRenderer;
import java.text.DecimalFormat;

import common.ColorScheme;
import common.UIHelper;
import components.ChiTietPhieuNhapTableModel;
import dao.NhaCungCapDao;
import dao.PhieuNhapDao;
import dao.SanPhamDao;
import entity.NguoiDung;
import entity.NhaCungCap;
import entity.SanPham;
import java.awt.Color;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

public class NhapHangPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private PhieuNhapDao phieuNhapDao;
	private SanPhamDao sanPhamDao;
	private NhaCungCapDao nhaCungCapDao;
	private ChiTietPhieuNhapTableModel tableModel;
	private JTable table;
	private JComboBox<NhaCungCap> comboNhaCungCap;
	private JComboBox<SanPham> comboSanPham;
	private JTextField txtSoLo;
	private JFormattedTextField txtHanSuDung;
	private JTextField txtGiaNhap;
	private JTextField txtSoLuong;
	private JTextField txtThanhTien;
	private JTextArea txtGhiChu;
	private JLabel lblTongTien;
	private JButton btnTaoPhieuNhap;
	private JButton btnThemSanPham;
	private JButton btnHuy;
	private NguoiDung currentUser;

	public NhapHangPanel(NguoiDung currentUser) {
		this.currentUser = currentUser;
		phieuNhapDao = new PhieuNhapDao();
		sanPhamDao = new SanPhamDao();
		nhaCungCapDao = new NhaCungCapDao();
		initialize();
		resetForm();
	}

	private void initialize() {
		setLayout(new BorderLayout());
		setBackground(ColorScheme.BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		var titlePanel = new JPanel(new BorderLayout(0, 10));
		titlePanel.setOpaque(false);
		titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
		var lblTitle = new JLabel("Nhập hàng");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
		lblTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		titlePanel.add(lblTitle, BorderLayout.WEST);
		
		var infoBanner = UIHelper.createInfoBanner("<html>ℹ️ <b>Nghiệp vụ Nhập hàng:</b> Lập phiếu nhập kho từ nhà cung cấp, hệ thống sẽ tự động tổng hợp mức tồn kho theo từng <b>Lô hàng</b>.</html>");
		titlePanel.add(infoBanner, BorderLayout.SOUTH);
		
		add(titlePanel, BorderLayout.NORTH);

		var mainPanel = new JPanel(new BorderLayout(15, 0));
		mainPanel.setOpaque(false);

		mainPanel.add(createLeftPanel(), BorderLayout.WEST);
		mainPanel.add(createRightPanel(), BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);
	}

	private JPanel createLeftPanel() {
		var panel = new JPanel();
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
				BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
				"Thông tin phiếu nhập",
				TitledBorder.LEADING,
				TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 14),
				ColorScheme.TEXT_PRIMARY));
		panel.setLayout(null);
		panel.setPreferredSize(new java.awt.Dimension(350, 0));

		int y = 30;
		int fieldWidth = 200;
		int labelWidth = 120;
		int fieldHeight = 30;
		int spacing = 40;

		var lblNhaCungCap = new JLabel("Nhà cung cấp:");
		lblNhaCungCap.setBounds(20, y, labelWidth, 25);
		panel.add(lblNhaCungCap);

		comboNhaCungCap = new JComboBox<>();
		var defaultNcc = new NhaCungCap();
		defaultNcc.setMaNCC(0);
		defaultNcc.setTenNCC("-- Chọn nhà cung cấp --");
		comboNhaCungCap.addItem(defaultNcc);
		var nccList = nhaCungCapDao.getAll();
		for (var ncc : nccList) {
			comboNhaCungCap.addItem(ncc);
		}
		comboNhaCungCap.setRenderer(new DefaultListCellRenderer() {
			@Override
			public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof NhaCungCap ncc) {
					if (ncc.getMaNCC() == 0) setText(ncc.getTenNCC());
					else setText(ncc.getMaNCC() + " - " + ncc.getTenNCC());
				}
				return this;
			}
		});
		comboNhaCungCap.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(comboNhaCungCap);

		y += spacing;

		var lblSanPham = new JLabel("Sản phẩm:*");
		lblSanPham.setBounds(20, y, labelWidth, 25);
		panel.add(lblSanPham);

		comboSanPham = new JComboBox<>();
		comboSanPham.setBounds(140, y, fieldWidth, fieldHeight);
		comboSanPham.setRenderer(new DefaultListCellRenderer() {
			@Override
			public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof SanPham sp) {
					setText(sp.getMaSanPham() + " - " + sp.getTenSanPham());
				}
				return this;
			}
		});
		loadSanPham();
		panel.add(comboSanPham);

		y += spacing;

		var lblSoLo = new JLabel("Mã số lô hàng:*");
		lblSoLo.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoLo);

		txtSoLo = new JTextField();
		txtSoLo.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtSoLo.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoLo);

		y += spacing;

		var lblHanSuDung = new JLabel("Hạn sử dụng:*");
		lblHanSuDung.setBounds(20, y, labelWidth, 25);
		panel.add(lblHanSuDung);

		try {
			MaskFormatter dateMask = new MaskFormatter("##/##/####");
			dateMask.setPlaceholderCharacter('_');
			txtHanSuDung = new JFormattedTextField(dateMask);
		} catch (java.text.ParseException e) {
			txtHanSuDung = new JFormattedTextField();
		}
		
		txtHanSuDung.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtHanSuDung.setBounds(140, y, fieldWidth, fieldHeight);
		txtHanSuDung.setToolTipText("Định dạng: dd/MM/yyyy (ví dụ: 31/12/2024)");
		// FlatLaf placeholder support
		txtHanSuDung.putClientProperty("JTextField.placeholderText", "dd/mm/yyyy");
		panel.add(txtHanSuDung);

		y += spacing;

		var lblGiaNhap = new JLabel("Giá nhập:*");
		lblGiaNhap.setBounds(20, y, labelWidth, 25);
		panel.add(lblGiaNhap);

		txtGiaNhap = new JTextField();
		txtGiaNhap.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtGiaNhap.setBounds(140, y, fieldWidth, fieldHeight);
		((AbstractDocument) txtGiaNhap.getDocument()).setDocumentFilter(new DocumentFilter() {
			private final DecimalFormat df = new DecimalFormat("#,###");
			@Override
			public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
				if (string == null) return;
				replace(fb, offset, 0, string, attr);
			}
			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
				String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
				String before = currentText.substring(0, offset);
				String after = currentText.substring(offset + length);
				String newText = before + text + after;
				newText = newText.replaceAll("[^0-9]", "");
				if (!newText.isEmpty()) {
					try {
						long value = Long.parseLong(newText);
						String formatted = df.format(value);
						fb.replace(0, fb.getDocument().getLength(), formatted, attrs);
					} catch (NumberFormatException e) {
						// Ignored
					}
				} else {
					fb.replace(0, fb.getDocument().getLength(), "", attrs);
				}
			}
			@Override
			public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
				replace(fb, offset, length, "", null);
			}
		});
		panel.add(txtGiaNhap);

		y += spacing;

		var lblSoLuong = new JLabel("Số lượng:*");
		lblSoLuong.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoLuong);

		txtSoLuong = new JTextField();
		txtSoLuong.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtSoLuong.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoLuong);

		y += spacing;

		var lblThanhTien = new JLabel("Tổng giá trị mục:");
		lblThanhTien.setBounds(20, y, labelWidth, 25);
		panel.add(lblThanhTien);

		txtThanhTien = new JTextField();
		txtThanhTien.setEditable(false);
		txtThanhTien.setBackground(ColorScheme.INPUT_DISABLED);
		txtThanhTien.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtThanhTien.setBounds(140, y, fieldWidth, fieldHeight);
		txtThanhTien.setForeground(ColorScheme.DANGER);
		txtThanhTien.setFont(new Font("Segoe UI", Font.BOLD, 12));
		panel.add(txtThanhTien);

		DocumentListener calcListener = new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { calcThanhTien(); }
			public void removeUpdate(DocumentEvent e) { calcThanhTien(); }
			public void changedUpdate(DocumentEvent e) { calcThanhTien(); }
		};
		txtGiaNhap.getDocument().addDocumentListener(calcListener);
		txtSoLuong.getDocument().addDocumentListener(calcListener);

		y += spacing;

		var lblGhiChu = new JLabel("Ghi chú:");
		lblGhiChu.setBounds(20, y, labelWidth, 25);
		panel.add(lblGhiChu);

		txtGhiChu = new JTextArea();
		txtGhiChu.setLineWrap(true);
		txtGhiChu.setWrapStyleWord(true);
		var scrollGhiChu = new JScrollPane(txtGhiChu);
		scrollGhiChu.setBounds(140, y, fieldWidth, 60);
		panel.add(scrollGhiChu);

		y += 70;

		btnThemSanPham = UIHelper.createSuccessButton("Thêm vào danh sách chờ");
		btnThemSanPham.setBounds(20, y, 320, 40);
		btnThemSanPham.addActionListener(e -> handleThemSanPham());
		panel.add(btnThemSanPham);

		y += 50;

		btnTaoPhieuNhap = UIHelper.createPrimaryButton("Xác nhận & Nhập kho");
		btnTaoPhieuNhap.setBounds(20, y, 320, 40);
		btnTaoPhieuNhap.addActionListener(e -> handleTaoPhieuNhap());
		panel.add(btnTaoPhieuNhap);

		y += 50;

		btnHuy = UIHelper.createDangerButton("Xóa sản phẩm đã chọn");
		btnHuy.setBounds(20, y, 320, 40);
		btnHuy.addActionListener(e -> handleXoaItemTable());
		panel.add(btnHuy);

		return panel;
	}

	private void calcThanhTien() {
		try {
			String priceStr = txtGiaNhap.getText().replaceAll("[^0-9]", "");
			BigDecimal price = new BigDecimal(priceStr.isEmpty() ? "0" : priceStr);
			
			String qtyStr = txtSoLuong.getText().replaceAll("[^0-9]", "");
			int qty = qtyStr.isEmpty() ? 0 : Integer.parseInt(qtyStr);
			
			if (price.compareTo(BigDecimal.ZERO) >= 0 && qty > 0) {
				DecimalFormat df = new DecimalFormat("#,###");
				String formattedThanhTien = df.format(price.multiply(new BigDecimal(qty)));
				txtThanhTien.setText(formattedThanhTien + " VND");
			} else {
				txtThanhTien.setText("");
			}
		} catch (Exception ex) {
			txtThanhTien.setText("");
		}
	}

	private JPanel createRightPanel() {
		var panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
				BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
				"Danh sách hàng chờ kiểm nhập",
				TitledBorder.LEADING,
				TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 14),
				ColorScheme.TEXT_PRIMARY));

		tableModel = new ChiTietPhieuNhapTableModel();
		table = new JTable(tableModel);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		table.setRowHeight(28);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.setShowGrid(true);
		table.setGridColor(new Color(235, 238, 242));
		table.setIntercellSpacing(new java.awt.Dimension(1, 1));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		// Custom Cell Renderer cho Cột "Giá nhập" (Index 4) và "Tổng giá trị mục" (Index 6)
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
			private final DecimalFormat df = new DecimalFormat("#,###");
			@Override
			public void setValue(Object value) {
				if (value instanceof BigDecimal) {
					setText(df.format(value) + " VND");
				} else {
					super.setValue(value);
				}
				setHorizontalAlignment(JLabel.RIGHT);
			}
		};
		table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
		table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
		
		var scrollPane = new JScrollPane(table);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER));
		scrollPane.getViewport().setBackground(ColorScheme.PANEL_BG);
		panel.add(scrollPane, BorderLayout.CENTER);

		var totalPanel = new JPanel(new BorderLayout());
		totalPanel.setOpaque(false);
		totalPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		var lblTotal = new JLabel("Tổng giá trị đơn nhập:");
		lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
		lblTotal.setForeground(ColorScheme.TEXT_PRIMARY);
		totalPanel.add(lblTotal, BorderLayout.WEST);

		lblTongTien = new JLabel("0 VND");
		lblTongTien.setFont(new Font("Segoe UI", Font.BOLD, 20));
		lblTongTien.setForeground(ColorScheme.DANGER);
		totalPanel.add(lblTongTien, BorderLayout.EAST);

		panel.add(totalPanel, BorderLayout.SOUTH);

		return panel;
	}

	private void loadSanPham() {
		comboSanPham.removeAllItems();
		var list = sanPhamDao.getAll();
		for (var sp : list) {
			comboSanPham.addItem(sp);
		}
	}

	private void handleThemSanPham() {
		if (comboSanPham.getSelectedItem() == null) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		SanPham sp = (SanPham) comboSanPham.getSelectedItem();
		String soLo = txtSoLo.getText().trim();
		if (soLo.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập số lô!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String strHSD = txtHanSuDung.getText().trim();
		if (strHSD.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập hạn sử dụng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		LocalDate hanSuDung;
		try {
			hanSuDung = LocalDate.parse(strHSD, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		} catch (DateTimeParseException e) {
			JOptionPane.showMessageDialog(this, "Hạn sử dụng không đúng định dạng! Vui lòng nhập dd/MM/yyyy", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (hanSuDung.isBefore(LocalDate.now())) {
			JOptionPane.showMessageDialog(this, "Không thể nhập lô đã quá hạn sử dụng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BigDecimal giaNhap;
		try {
			String priceStr = txtGiaNhap.getText().replaceAll("[^0-9]", "");
			giaNhap = new BigDecimal(priceStr);
			if (giaNhap.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Giá nhập không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int soLuong;
		try {
			String qtyStr = txtSoLuong.getText().replaceAll("[^0-9]", "");
			soLuong = Integer.parseInt(qtyStr);
			if (soLuong <= 0) throw new NumberFormatException();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Số lượng phải là số nguyên dương!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BigDecimal thanhTien = giaNhap.multiply(new BigDecimal(soLuong));
		
		Object[] rowData = {
			sp.getMaSanPham(),
			sp.getTenSanPham(),
			soLo,
			hanSuDung,
			giaNhap,
			soLuong,
			thanhTien
		};
		tableModel.addRow(rowData);
		
		updateTongTien();
		clearProductForm();
	}

	private void handleXoaItemTable() {
		int row = table.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng muốn xóa khỏi bảng", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
			return;
		}
		tableModel.removeRow(row);
		updateTongTien();
	}

	private void updateTongTien() {
		BigDecimal total = BigDecimal.ZERO;
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			BigDecimal thanhTien = (BigDecimal) tableModel.getValueAt(i, 6);
			total = total.add(thanhTien);
		}
		DecimalFormat df = new DecimalFormat("#,###");
		lblTongTien.setText(df.format(total) + " VND");
	}

	private void handleTaoPhieuNhap() {
		if (tableModel.getRowCount() == 0) {
			JOptionPane.showMessageDialog(this, "Phiếu nhập chưa có sản phẩm nào! Hãy thêm sản phẩm trước", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Integer maNCC = null;
		NhaCungCap ncc = (NhaCungCap) comboNhaCungCap.getSelectedItem();
		if (ncc != null && ncc.getMaNCC() > 0) maNCC = ncc.getMaNCC();

		String ghiChu = txtGhiChu.getText().trim();

		System.out.println("[NhapHangPanel] === CHUẨN BỊ GỬI YÊU CẦU TRANSACTION ===");
		List<entity.ChiTietPhieuNhap> chiTietList = new ArrayList<>();

		for (int i = 0; i < tableModel.getRowCount(); i++) {
			int maSanPham = ((Number) tableModel.getValueAt(i, 0)).intValue();
			String soLo = (String) tableModel.getValueAt(i, 2);
			LocalDate hanSuDung = (LocalDate) tableModel.getValueAt(i, 3);
			BigDecimal giaNhap = (BigDecimal) tableModel.getValueAt(i, 4);
			int soLuong = ((Number) tableModel.getValueAt(i, 5)).intValue();

			entity.ChiTietPhieuNhap ct = new entity.ChiTietPhieuNhap();
			ct.setMaSanPham(maSanPham);
			ct.setSoLo(soLo);
			ct.setHanSuDung(hanSuDung);
			ct.setGiaNhap(giaNhap);
			ct.setSoLuong(soLuong);
			chiTietList.add(ct);
		}

		Integer maPN = phieuNhapDao.savePhieuNhapTransaction(currentUser.getMaNguoiDung(), maNCC, ghiChu, chiTietList);
		
		if (maPN != null && maPN > 0) {
			System.out.println("[NhapHangPanel] Giao dịch lưu Phiếu Nhập Hàng thành công!");
			JOptionPane.showMessageDialog(this,
					"Tạo Phiếu Nhập Hàng #" + maPN + " thành công!\n"
					+ tableModel.getRowCount() + " sản phẩm đã được tự động nhập Lô hàng.",
					"Thành công", JOptionPane.INFORMATION_MESSAGE);
			
			// Auto refresh data across panels (Requirement: AUTO_REFRESH_DATA_AFTER_IMPORT)
			var top = javax.swing.SwingUtilities.getWindowAncestor(this);
			if (top instanceof app.MainFrame) {
				((app.MainFrame) top).refreshAllData();
			}
			
			resetForm();
		} else {
			JOptionPane.showMessageDialog(this,
					"Tạo Phiếu Nhập Hàng gặp lỗi!\n(Đã tự động Rollback - Dữ liệu không bị rác)\nXem chi tiết lỗi trong console/terminal.",
					"Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void clearProductForm() {
		if (comboSanPham.getItemCount() > 0) {
			comboSanPham.setSelectedIndex(0);
		}
		txtSoLo.setText("");
		txtHanSuDung.setText("");
		txtGiaNhap.setText("");
		txtSoLuong.setText("");
		txtThanhTien.setText("");
	}

	private void resetForm() {
		if (comboNhaCungCap.getItemCount() > 0) {
			comboNhaCungCap.setSelectedIndex(0);
		}
		clearProductForm();
		txtGhiChu.setText("");
		tableModel.setRowCount(0);
		updateTongTien();
	}
}
