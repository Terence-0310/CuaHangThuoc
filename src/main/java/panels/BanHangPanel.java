package panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import common.ColorScheme;
import common.UIHelper;
import components.ChiTietHoaDonTableModel;
import dao.ChiTietHoaDonDao;
import dao.HoaDonBanDao;
import dao.KhachHangDao;
import dao.SanPhamDao;
import entity.NguoiDung;

/**
 * BanHangPanel - Panel bán hàng với FEFO
 * 
 * @author Generated
 * @version 1.0
 */
public class BanHangPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private HoaDonBanDao hoaDonDao;
	private ChiTietHoaDonDao chiTietDao;
	private SanPhamDao sanPhamDao;
	private KhachHangDao khachHangDao;
	private ChiTietHoaDonTableModel tableModel;
	private JTable table;
	private JTextField txtMaHoaDon;
	private JComboBox<String> comboKhachHang;
	private JComboBox<String> comboSanPham;
	private JTextField txtSoLuong;
	private JTextField txtGiaBan;
	private JTextArea txtGhiChu;
	private JLabel lblTongTien;
	private JButton btnTaoHoaDon;
	private JButton btnThemSanPham;
	private JButton btnThanhToan;
	private JButton btnHuy;
	private Integer currentMaHoaDon;
	private NguoiDung currentUser;

	/**
	 * Create the panel.
	 */
	public BanHangPanel(NguoiDung currentUser) {
		this.currentUser = currentUser;
		hoaDonDao = new HoaDonBanDao();
		chiTietDao = new ChiTietHoaDonDao();
		sanPhamDao = new SanPhamDao();
		khachHangDao = new KhachHangDao();
		initialize();
		resetForm();
	}

	/**
	 * Initialize the contents of the panel.
	 */
	private void initialize() {
		setLayout(new BorderLayout());
		setBackground(ColorScheme.BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		var titlePanel = new JPanel(new BorderLayout(0, 10));
		titlePanel.setOpaque(false);
		titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
		var lblTitle = new JLabel("Bán hàng");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
		lblTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		titlePanel.add(lblTitle, BorderLayout.WEST);
		
		var infoBanner = UIHelper.createInfoBanner("<html>ℹ️ <b>Nghiệp vụ Bán hàng:</b> Tạo hóa đơn, bán lẻ. Hệ thống tự động xuất kho theo chuẩn <b>FEFO (Hết hạn trước - Xuất kho trước)</b>.</html>");
		titlePanel.add(infoBanner, BorderLayout.SOUTH);
		
		add(titlePanel, BorderLayout.NORTH);

		var mainPanel = new JPanel(new BorderLayout(15, 0));
		mainPanel.setOpaque(false);

		var leftPanel = createLeftPanel();
		mainPanel.add(leftPanel, BorderLayout.WEST);

		var rightPanel = createRightPanel();
		mainPanel.add(rightPanel, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * Tạo panel bên trái - Form thông tin hóa đơn
	 */
	private JPanel createLeftPanel() {
		var panel = new JPanel();
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			"Thông tin hóa đơn",
			TitledBorder.LEADING,
			TitledBorder.TOP,
			new Font("Segoe UI", Font.BOLD, 14),
			ColorScheme.TEXT_PRIMARY
		));
		panel.setLayout(null);
		panel.setPreferredSize(new java.awt.Dimension(350, 0));

		int y = 30;
		int labelWidth = 120;
		int fieldWidth = 200;
		int fieldHeight = 30;
		int spacing = 40;

		var lblMaHoaDon = new JLabel("Mã hóa đơn:");
		lblMaHoaDon.setBounds(20, y, labelWidth, 25);
		panel.add(lblMaHoaDon);

		txtMaHoaDon = new JTextField();
		txtMaHoaDon.setEditable(false);
		txtMaHoaDon.setBackground(ColorScheme.INPUT_DISABLED);
		txtMaHoaDon.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMaHoaDon.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMaHoaDon);

		y += spacing;

		var lblKhachHang = new JLabel("Khách hàng:");
		lblKhachHang.setBounds(20, y, labelWidth, 25);
		panel.add(lblKhachHang);

		comboKhachHang = new JComboBox<>();
		comboKhachHang.addItem("-- Chọn khách hàng --");
		var khList = khachHangDao.getAll();
		for (var kh : khList) {
			comboKhachHang.addItem(kh.getMaKhachHang() + " - " + kh.getHoTen());
		}
		comboKhachHang.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(comboKhachHang);

		y += spacing;

		var lblSanPham = new JLabel("Sản phẩm:*");
		lblSanPham.setBounds(20, y, labelWidth, 25);
		panel.add(lblSanPham);

		comboSanPham = new JComboBox<>();
		comboSanPham.setBounds(140, y, fieldWidth, fieldHeight);
		loadSanPham();
		panel.add(comboSanPham);

		y += spacing;

		var lblSoLuong = new JLabel("Số lượng:*");
		lblSoLuong.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoLuong);

		txtSoLuong = new JTextField();
		txtSoLuong.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtSoLuong.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoLuong);

		y += spacing;

		var lblGiaBan = new JLabel("Giá bán:");
		lblGiaBan.setBounds(20, y, labelWidth, 25);
		panel.add(lblGiaBan);

		txtGiaBan = new JTextField();
		txtGiaBan.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtGiaBan.setBounds(140, y, fieldWidth, fieldHeight);
		txtGiaBan.setToolTipText("Để trống sẽ dùng giá bán đề xuất");
		panel.add(txtGiaBan);

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

		btnThemSanPham = UIHelper.createSuccessButton("Thêm sản phẩm");
		btnThemSanPham.setBounds(20, y, 320, 40);
		btnThemSanPham.addActionListener(e -> handleThemSanPham());
		panel.add(btnThemSanPham);

		y += 50;

		btnTaoHoaDon = UIHelper.createPrimaryButton("Tạo hóa đơn mới");
		btnTaoHoaDon.setBounds(20, y, 320, 40);
		btnTaoHoaDon.addActionListener(e -> handleTaoHoaDon());
		panel.add(btnTaoHoaDon);

		// Moved Payment/Cancel buttons to Right Panel (User Request)

		return panel;
	}

	/**
	 * Tạo panel bên phải - Chi tiết hóa đơn
	 */
	private JPanel createRightPanel() {
		var panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			"Chi tiết hóa đơn",
			TitledBorder.LEADING,
			TitledBorder.TOP,
			new Font("Segoe UI", Font.BOLD, 14),
			ColorScheme.TEXT_PRIMARY
		));

		tableModel = new ChiTietHoaDonTableModel();
		table = new JTable(tableModel);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		table.setRowHeight(25);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.setShowGrid(true); // Kẻ khung lưới
		table.setGridColor(ColorScheme.BORDER);

		var scrollPane = new JScrollPane(table);
		// Kẻ thêm khung cho vùng dữ liệu (User request)
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER));
		panel.add(scrollPane, BorderLayout.CENTER);

		// Bottom container for Total + Actions
		var bottomContainer = new JPanel(new BorderLayout());
		bottomContainer.setOpaque(false);
		bottomContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Panel tổng tiền
		var totalPanel = new JPanel(new BorderLayout());
		totalPanel.setOpaque(false);
		totalPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0)); // Spacing below total

		var lblTotal = new JLabel("Tổng tiền:");
		lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
		lblTotal.setForeground(ColorScheme.TEXT_PRIMARY);
		totalPanel.add(lblTotal, BorderLayout.WEST);

		lblTongTien = new JLabel("0 đ");
		lblTongTien.setFont(new Font("Segoe UI", Font.BOLD, 22));
		lblTongTien.setForeground(ColorScheme.DANGER);
		totalPanel.add(lblTongTien, BorderLayout.EAST);

		bottomContainer.add(totalPanel, BorderLayout.NORTH);

		// Action Buttons (Moved from Left Panel)
		var actionPanel = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
		actionPanel.setOpaque(false);
		
		btnThanhToan = UIHelper.createSuccessButton("Thanh toán (F9)");
		btnThanhToan.setFont(new Font("Segoe UI", Font.BOLD, 16));
		btnThanhToan.setPreferredSize(new java.awt.Dimension(0, 50)); // Bigger button
		btnThanhToan.addActionListener(e -> handleThanhToan());
		
		btnHuy = UIHelper.createDangerButton("Hủy bỏ");
		btnHuy.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnHuy.addActionListener(e -> handleHuy());

		actionPanel.add(btnHuy);
		actionPanel.add(btnThanhToan);

		bottomContainer.add(actionPanel, BorderLayout.CENTER);

		panel.add(bottomContainer, BorderLayout.SOUTH);

		return panel;
	}


	/**
	 * Load danh sách sản phẩm
	 */
	private void loadSanPham() {
		comboSanPham.removeAllItems();
		var list = sanPhamDao.getAll();
		for (var sp : list) {
			comboSanPham.addItem(sp.getMaSanPham() + " - " + sp.getTenSanPham());
		}
	}

	/**
	 * Xử lý tạo hóa đơn mới
	 */
	private void handleTaoHoaDon() {
		Integer maKH = null;
		if (comboKhachHang.getSelectedIndex() > 0) {
			String selected = (String) comboKhachHang.getSelectedItem();
			maKH = Integer.parseInt(selected.split(" - ")[0]);
		}

		String ghiChu = txtGhiChu.getText().trim();

		Integer maHD = hoaDonDao.createHoaDon(currentUser.getMaNguoiDung(), maKH, ghiChu);
		if (maHD != null) {
			currentMaHoaDon = maHD;
			txtMaHoaDon.setText(String.valueOf(maHD));
			JOptionPane.showMessageDialog(this,
				"Tạo hóa đơn thành công! Mã hóa đơn: " + maHD,
				"Thông báo",
				JOptionPane.INFORMATION_MESSAGE);
			tableModel.setRowCount(0);
			updateTongTien();
		} else {
			JOptionPane.showMessageDialog(this,
				"Tạo hóa đơn thất bại!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Xử lý thêm sản phẩm vào hóa đơn
	 */
	private void handleThemSanPham() {
		if (currentMaHoaDon == null) {
			JOptionPane.showMessageDialog(this,
				"Vui lòng tạo hóa đơn trước!",
				"Thông báo",
				JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (comboSanPham.getSelectedIndex() < 0) {
			JOptionPane.showMessageDialog(this,
				"Vui lòng chọn sản phẩm!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		String selected = (String) comboSanPham.getSelectedItem();
		int maSP = Integer.parseInt(selected.split(" - ")[0]);

		if (txtSoLuong.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Vui lòng nhập số lượng!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		int soLuong;
		try {
			soLuong = Integer.parseInt(txtSoLuong.getText().trim());
			if (soLuong <= 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this,
				"Số lượng phải là số nguyên dương!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		BigDecimal giaBan = null;
		if (!txtGiaBan.getText().trim().isEmpty()) {
			try {
				giaBan = new BigDecimal(txtGiaBan.getText().trim());
				if (giaBan.compareTo(BigDecimal.ZERO) < 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this,
					"Giá bán không hợp lệ!",
					"Lỗi",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		// Gọi stored procedure để bán theo FEFO
		if (hoaDonDao.sellProductFEFO(currentMaHoaDon, maSP, soLuong, giaBan)) {
			JOptionPane.showMessageDialog(this,
				"Thêm sản phẩm thành công!",
				"Thông báo",
				JOptionPane.INFORMATION_MESSAGE);
			loadChiTietHoaDon();
			txtSoLuong.setText("");
			txtGiaBan.setText("");
		} else {
			JOptionPane.showMessageDialog(this,
				"Thêm sản phẩm thất bại! Kiểm tra lại tồn kho hoặc số lượng.",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Load chi tiết hóa đơn vào table
	 */
	private void loadChiTietHoaDon() {
		if (currentMaHoaDon == null) {
			return;
		}

		tableModel.setRowCount(0);
		var list = chiTietDao.getDetailForDisplay(currentMaHoaDon);
		for (var row : list) {
			tableModel.addRow(row);
		}
		updateTongTien();
	}

	/**
	 * Cập nhật tổng tiền
	 */
	private void updateTongTien() {
		BigDecimal total = BigDecimal.ZERO;
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			BigDecimal thanhTien = (BigDecimal) tableModel.getValueAt(i, 5);
			total = total.add(thanhTien);
		}
		lblTongTien.setText(total.toString() + " đ");
	}

	/**
	 * Xử lý thanh toán
	 */
	private void handleThanhToan() {
		if (currentMaHoaDon == null) {
			JOptionPane.showMessageDialog(this,
				"Chưa có hóa đơn nào!",
				"Thông báo",
				JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (tableModel.getRowCount() == 0) {
			JOptionPane.showMessageDialog(this,
				"Hóa đơn chưa có sản phẩm nào!",
				"Thông báo",
				JOptionPane.WARNING_MESSAGE);
			return;
		}

		int option = JOptionPane.showConfirmDialog(this,
			"Xác nhận thanh toán hóa đơn?",
			"Xác nhận",
			JOptionPane.YES_NO_OPTION);

		if (option == JOptionPane.YES_OPTION) {
			JOptionPane.showMessageDialog(this,
				"Thanh toán thành công! Mã hóa đơn: " + currentMaHoaDon,
				"Thông báo",
				JOptionPane.INFORMATION_MESSAGE);
			resetForm();
		}
	}

	/**
	 * Xử lý hủy
	 */
	private void handleHuy() {
		if (currentMaHoaDon == null) {
			return;
		}

		int option = JOptionPane.showConfirmDialog(this,
			"Bạn có chắc chắn muốn hủy hóa đơn này?",
			"Xác nhận",
			JOptionPane.YES_NO_OPTION);

		if (option == JOptionPane.YES_OPTION) {
			resetForm();
		}
	}

	/**
	 * Reset form
	 */
	private void resetForm() {
		currentMaHoaDon = null;
		txtMaHoaDon.setText("");
		comboKhachHang.setSelectedIndex(0);
		comboSanPham.setSelectedIndex(-1);
		txtSoLuong.setText("");
		txtGiaBan.setText("");
		txtGhiChu.setText("");
		tableModel.setRowCount(0);
		updateTongTien();
	}
}
