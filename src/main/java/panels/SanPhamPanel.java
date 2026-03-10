package panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.JTableHeader;

import common.ColorScheme;
import common.UIHelper;
import components.SanPhamTableModel;
import dao.SanPhamDao;
import entity.NguoiDung;
import entity.SanPham;

/**
 * SanPhamPanel - Panel quản lý sản phẩm (v3: phân trang + sắp xếp + tìm kiếm)
 * 
 * @author Generated
 * @version 3.0
 */
public class SanPhamPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private SanPhamDao dao;
	private SanPhamTableModel tableModel;
	private JTable table;
	private JTextField txtMaSP;
	private JTextField txtTenSP;
	private JTextField txtDonViTinh;
	private JTextField txtGiaBan;
	private JComboBox<String> comboLoaiSP;
	private JTextField txtMoTa;
	private JTextField txtMucTonToiThieu;
	private JButton btnThem;
	private JButton btnSua;
	private JButton btnXoa;
	private JButton btnLamMoi;
	private JButton btnTimKiem;
	private JTextField txtTimKiem;

	// ===== PAGINATION STATE =====
	private int currentPage = 1;
	private int pageSize = 25;
	private int totalPages = 1;
	private int totalRows = 0;

	// ===== SORT STATE =====
	private String sortColumn = "MaSanPham";
	private String sortOrder = "ASC"; // ASC or DESC

	// ===== SEARCH STATE =====
	private String currentKeyword = null; // null = no filter

	// ===== PAGINATION UI COMPONENTS =====
	private JButton btnPrev;
	private JButton btnNext;
	private JButton btnFirst;
	private JButton btnLast;
	private JLabel lblPageInfo;
	private JLabel lblTotalRows;

	// Mapping: column index -> SQL column name
	private static final String[] COLUMN_DB_NAMES = {
			"MaSanPham", "TenSanPham", "DonViTinh",
			"GiaBanDeXuat", "LoaiSanPham", "MoTa", "MucTonToiThieu"
	};

	// Original column names (without sort indicators)
	private static final String[] COLUMN_DISPLAY_NAMES = {
			"Mã SP", "Tên sản phẩm", "Đơn vị tính",
			"Giá bán", "Loại", "Mô tả", "Mức tồn tối thiểu"
	};

	/**
	 * Create the panel.
	 */
	public SanPhamPanel(NguoiDung currentUser) {
		dao = new SanPhamDao();
		initialize();
		loadPageData();
	}

	/**
	 * Initialize the contents of the panel.
	 */
	private void initialize() {
		setLayout(new BorderLayout());
		setBackground(ColorScheme.BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

		// Title
		var titlePanel = new JPanel(new BorderLayout(0, 10));
		titlePanel.setOpaque(false);
		titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
		var lblTitle = new JLabel("Quản lý sản phẩm");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
		lblTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		titlePanel.add(lblTitle, BorderLayout.WEST);
		
		var infoBanner = UIHelper.createInfoBanner("<html>ℹ️ <b>Danh mục Sản phẩm:</b> Nơi quản lý thông tin chung của mặt hàng kinh doanh (chưa lưu tồn kho số lượng - số lượng nằm ở Quản lý lô hàng).</html>");
		titlePanel.add(infoBanner, BorderLayout.SOUTH);
		
		add(titlePanel, BorderLayout.NORTH);

		// Main content
		var mainPanel = new JPanel(new BorderLayout(20, 0));
		mainPanel.setOpaque(false);

		// Left panel - Form
		var formPanel = createFormPanel();
		mainPanel.add(formPanel, BorderLayout.WEST);

		// Right panel - Table + Pagination
		var tablePanel = createTablePanel();
		mainPanel.add(tablePanel, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * Tạo form panel
	 */
	private JPanel createFormPanel() {
		var panel = new JPanel();
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
				BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
				"Thông tin sản phẩm",
				TitledBorder.LEADING,
				TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 16),
				ColorScheme.TEXT_PRIMARY));
		panel.setLayout(null);
		panel.setPreferredSize(new java.awt.Dimension(380, 0));

		int y = 40;
		int labelWidth = 130;
		int fieldWidth = 210;
		int fieldHeight = 35;
		int spacing = 45;

		// Mã SP (read-only)
		var lblMaSP = new JLabel("Mã SP:");
		lblMaSP.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		lblMaSP.setForeground(ColorScheme.TEXT_PRIMARY);
		lblMaSP.setBounds(20, y, labelWidth, 25);
		panel.add(lblMaSP);

		txtMaSP = new JTextField();
		txtMaSP.setEditable(false);
		txtMaSP.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		txtMaSP.setBackground(ColorScheme.INPUT_DISABLED);
		txtMaSP.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
				new EmptyBorder(8, 12, 8, 12)));
		txtMaSP.setBounds(20, y + 25, fieldWidth, fieldHeight);
		panel.add(txtMaSP);

		y += spacing + 25;

		// Tên sản phẩm
		var lblTenSP = new JLabel("Tên sản phẩm:*");
		lblTenSP.setBounds(20, y, labelWidth, 25);
		panel.add(lblTenSP);

		txtTenSP = new JTextField();
		txtTenSP.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtTenSP.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtTenSP);

		y += spacing;

		// Đơn vị tính
		var lblDonViTinh = new JLabel("Đơn vị tính:");
		lblDonViTinh.setBounds(20, y, labelWidth, 25);
		panel.add(lblDonViTinh);

		txtDonViTinh = new JTextField("Hộp");
		txtDonViTinh.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtDonViTinh.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtDonViTinh);

		y += spacing;

		// Giá bán
		var lblGiaBan = new JLabel("Giá bán đề xuất:*");
		lblGiaBan.setBounds(20, y, labelWidth, 25);
		panel.add(lblGiaBan);

		txtGiaBan = new JTextField();
		txtGiaBan.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtGiaBan.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtGiaBan);

		y += spacing;

		// Loại sản phẩm
		var lblLoaiSP = new JLabel("Loại sản phẩm:*");
		lblLoaiSP.setBounds(20, y, labelWidth, 25);
		panel.add(lblLoaiSP);

		comboLoaiSP = new JComboBox<>(new String[] {
				"Thuoc", "DuocMiPham", "ThucPhamChucNang", "ChamSocCaNhan", "ThietBiYTe"
		});
		comboLoaiSP.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(comboLoaiSP);

		y += spacing;

		// Mô tả
		var lblMoTa = new JLabel("Mô tả:");
		lblMoTa.setBounds(20, y, labelWidth, 25);
		panel.add(lblMoTa);

		txtMoTa = new JTextField();
		txtMoTa.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMoTa.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMoTa);

		y += spacing;

		// Mức tồn tối thiểu
		var lblMucTon = new JLabel("Mức tồn tối thiểu:");
		lblMucTon.setBounds(20, y, labelWidth, 25);
		panel.add(lblMucTon);

		txtMucTonToiThieu = new JTextField("10");
		txtMucTonToiThieu.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMucTonToiThieu.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMucTonToiThieu);

		y += spacing + 20;

		// Buttons
		btnThem = UIHelper.createSuccessButton("Thêm mới");
		btnThem.setBounds(20, y, 100, 38);
		btnThem.addActionListener(e -> handleThem());
		panel.add(btnThem);

		btnSua = UIHelper.createPrimaryButton("Cập nhật");
		btnSua.setBounds(130, y, 100, 38);
		btnSua.addActionListener(e -> handleSua());
		panel.add(btnSua);

		btnXoa = UIHelper.createDangerButton("Xóa");
		btnXoa.setBounds(240, y, 100, 38);
		btnXoa.addActionListener(e -> handleXoa());
		panel.add(btnXoa);

		y += 48;

		btnLamMoi = UIHelper.createNeutralButton("Làm mới");
		btnLamMoi.setBounds(20, y, 320, 38);
		btnLamMoi.addActionListener(e -> handleLamMoi());
		panel.add(btnLamMoi);

		return panel;
	}

	/**
	 * Tạo table panel kèm pagination bar
	 */
	private JPanel createTablePanel() {
		var panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
				BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
				"Danh sách sản phẩm",
				TitledBorder.LEADING,
				TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 14),
				ColorScheme.TEXT_PRIMARY));

		// Search panel
		var searchPanel = new JPanel(new BorderLayout(10, 0));
		searchPanel.setOpaque(false);
		searchPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		var lblSearch = new JLabel("Tìm kiếm:");
		lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		searchPanel.add(lblSearch, BorderLayout.WEST);

		txtTimKiem = new JTextField();
		txtTimKiem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		// Enter key triggers search
		txtTimKiem.addActionListener(e -> handleTimKiem());
		searchPanel.add(txtTimKiem, BorderLayout.CENTER);

		btnTimKiem = UIHelper.createPrimaryButton("Tìm");
		btnTimKiem.setPreferredSize(new java.awt.Dimension(90, 35));
		btnTimKiem.addActionListener(e -> handleTimKiem());
		searchPanel.add(btnTimKiem, BorderLayout.EAST);

		panel.add(searchPanel, BorderLayout.NORTH);

		// Table
		tableModel = new SanPhamTableModel();
		table = new JTable(tableModel);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		table.setRowHeight(28);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.setShowGrid(true);
		table.setGridColor(new Color(235, 238, 242));
		table.setIntercellSpacing(new Dimension(1, 1));
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				handleTableSelection();
			}
		});

		// ===== AUTO RESIZE + COLUMN WIDTHS =====
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		var columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(60); // Mã SP
		columnModel.getColumn(0).setMaxWidth(80);
		columnModel.getColumn(1).setPreferredWidth(220); // Tên sản phẩm
		columnModel.getColumn(2).setPreferredWidth(70); // Đơn vị tính
		columnModel.getColumn(2).setMaxWidth(100);
		columnModel.getColumn(3).setPreferredWidth(100); // Giá bán
		columnModel.getColumn(3).setMaxWidth(130);
		columnModel.getColumn(4).setPreferredWidth(130); // Loại
		columnModel.getColumn(4).setMaxWidth(160);
		columnModel.getColumn(5).setPreferredWidth(200); // Mô tả
		// Tăng độ rộng ưu tiên và đặt độ rộng tối thiểu để không bị mất chữ
		columnModel.getColumn(6).setPreferredWidth(120); // Tăng lên 120 cho thoáng
		columnModel.getColumn(6).setMinWidth(100); // Đảm bảo không bao giờ nhỏ hơn 100
		columnModel.getColumn(6).setMaxWidth(200); // Nới lỏng giới hạn trên

		// ===== COLUMN HEADER CLICK → SORT =====
		setupColumnHeaderSorting();

		var scrollPane = new JScrollPane(table);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER));
		scrollPane.getViewport().setBackground(ColorScheme.PANEL_BG);
		panel.add(scrollPane, BorderLayout.CENTER);

		// ===== PAGINATION BAR =====
		var paginationPanel = createPaginationPanel();
		panel.add(paginationPanel, BorderLayout.SOUTH);

		return panel;
	}

	/**
	 * Cài đặt sự kiện click vào tiêu đề cột để sắp xếp
	 */
	private void setupColumnHeaderSorting() {
		JTableHeader header = table.getTableHeader();
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setFont(new Font("Segoe UI", Font.BOLD, 12));

		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int columnIndex = header.columnAtPoint(e.getPoint());
				if (columnIndex < 0 || columnIndex >= COLUMN_DB_NAMES.length)
					return;

				String clickedColumn = COLUMN_DB_NAMES[columnIndex];

				// Toggle sort order nếu click lại cùng cột
				if (clickedColumn.equals(sortColumn)) {
					sortOrder = "ASC".equals(sortOrder) ? "DESC" : "ASC";
				} else {
					sortColumn = clickedColumn;
					sortOrder = "ASC";
				}

				// Reset về trang 1 khi đổi sort
				currentPage = 1;

				// Cập nhật header hiển thị mũi tên
				updateColumnHeaders();

				// Reload data
				loadPageData();
			}
		});

		// Set initial header
		updateColumnHeaders();
	}

	/**
	 * Cập nhật tiêu đề cột với mũi tên sắp xếp ▲ ▼
	 */
	private void updateColumnHeaders() {
		for (int i = 0; i < COLUMN_DISPLAY_NAMES.length; i++) {
			String name = COLUMN_DISPLAY_NAMES[i];
			if (COLUMN_DB_NAMES[i].equals(sortColumn)) {
				String arrow = "ASC".equals(sortOrder) ? " ▲" : " ▼";
				tableModel.setColumnName(i, name + arrow);
			} else {
				tableModel.setColumnName(i, name);
			}
		}
		// Refresh header display
		table.getTableHeader().repaint();
	}

	/**
	 * Tạo pagination bar đẹp, đồng bộ FlatLaf
	 */
	private JPanel createPaginationPanel() {
		var panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.BORDER),
				new EmptyBorder(10, 15, 10, 15)));

		// Left: Total row info
		lblTotalRows = new JLabel("Tổng: 0 sản phẩm");
		lblTotalRows.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		lblTotalRows.setForeground(ColorScheme.TEXT_SECONDARY);
		panel.add(lblTotalRows, BorderLayout.WEST);

		// Center: Navigation buttons
		var navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
		navPanel.setOpaque(false);

		// First page button
		btnFirst = createPaginationButton("«");
		btnFirst.setToolTipText("Trang đầu");
		btnFirst.addActionListener(e -> {
			if (currentPage > 1) {
				currentPage = 1;
				loadPageData();
			}
		});
		navPanel.add(btnFirst);

		// Previous button
		btnPrev = createPaginationButton("‹ Trước");
		btnPrev.setToolTipText("Trang trước");
		btnPrev.addActionListener(e -> {
			if (currentPage > 1) {
				currentPage--;
				loadPageData();
			}
		});
		navPanel.add(btnPrev);

		// Page info label
		navPanel.add(Box.createHorizontalStrut(8));
		lblPageInfo = new JLabel("Trang 1 / 1");
		lblPageInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
		lblPageInfo.setForeground(ColorScheme.PRIMARY);
		navPanel.add(lblPageInfo);
		navPanel.add(Box.createHorizontalStrut(8));

		// Next button
		btnNext = createPaginationButton("Sau ›");
		btnNext.setToolTipText("Trang sau");
		btnNext.addActionListener(e -> {
			if (currentPage < totalPages) {
				currentPage++;
				loadPageData();
			}
		});
		navPanel.add(btnNext);

		// Last page button
		btnLast = createPaginationButton("»");
		btnLast.setToolTipText("Trang cuối");
		btnLast.addActionListener(e -> {
			if (currentPage < totalPages) {
				currentPage = totalPages;
				loadPageData();
			}
		});
		navPanel.add(btnLast);

		panel.add(navPanel, BorderLayout.CENTER);

		return panel;
	}

	/**
	 * Tạo button phân trang với style FlatLaf-friendly
	 */
	private JButton createPaginationButton(String text) {
		var btn = new JButton(text);
		btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
		btn.setForeground(ColorScheme.PRIMARY);
		btn.setBackground(ColorScheme.PANEL_BG);
		btn.setFocusPainted(false);
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btn.setPreferredSize(new Dimension(text.length() > 2 ? 90 : 42, 32));
		btn.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
				new EmptyBorder(4, 10, 4, 10)));

		// Hover effects
		Color normalBg = ColorScheme.PANEL_BG;
		Color hoverBg = new Color(232, 240, 254);

		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (btn.isEnabled()) {
					btn.setBackground(hoverBg);
					btn.setBorder(BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(ColorScheme.PRIMARY, 1),
							new EmptyBorder(4, 10, 4, 10)));
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				btn.setBackground(normalBg);
				btn.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
						new EmptyBorder(4, 10, 4, 10)));
			}
		});

		return btn;
	}

	/**
	 * Cập nhật trạng thái enable/disable của các nút phân trang
	 */
	private void updatePaginationState() {
		boolean hasPrev = currentPage > 1;
		boolean hasNext = currentPage < totalPages;

		btnFirst.setEnabled(hasPrev);
		btnPrev.setEnabled(hasPrev);
		btnNext.setEnabled(hasNext);
		btnLast.setEnabled(hasNext);

		// Dim disabled buttons
		btnFirst.setForeground(hasPrev ? ColorScheme.PRIMARY : ColorScheme.BORDER);
		btnPrev.setForeground(hasPrev ? ColorScheme.PRIMARY : ColorScheme.BORDER);
		btnNext.setForeground(hasNext ? ColorScheme.PRIMARY : ColorScheme.BORDER);
		btnLast.setForeground(hasNext ? ColorScheme.PRIMARY : ColorScheme.BORDER);

		lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);

		// Phân biệt label khi search vs normal
		if (currentKeyword != null && !currentKeyword.isEmpty()) {
			lblTotalRows.setText("Tìm thấy: " + totalRows + " sản phẩm");
		} else {
			lblTotalRows.setText("Tổng: " + totalRows + " sản phẩm");
		}
	}

	/**
	 * Load dữ liệu phân trang vào table (core method)
	 * Gọi SP với keyword + sort params
	 */
	private void loadPageData() {
		tableModel.setRowCount(0);

		var result = dao.getByPage(currentPage, pageSize, currentKeyword, sortColumn, sortOrder);
		totalRows = result.getTotalRows();
		totalPages = result.getTotalPages();

		// Điều chỉnh currentPage nếu vượt quá totalPages
		if (currentPage > totalPages && totalPages > 0) {
			currentPage = totalPages;
			result = dao.getByPage(currentPage, pageSize, currentKeyword, sortColumn, sortOrder);
		}

		for (var sp : result.getData()) {
			tableModel.addRow(new Object[] {
					sp.getMaSanPham(),
					sp.getTenSanPham(),
					sp.getDonViTinh(),
					sp.getGiaBanDeXuat(),
					sp.getLoaiSanPham(),
					sp.getMoTa() != null ? sp.getMoTa() : "",
					sp.getMucTonToiThieu()
			});
		}

		updatePaginationState();
	}

	/**
	 * Load dữ liệu (reset về trang 1, xóa keyword)
	 */
	public void loadData() {
		currentPage = 1;
		currentKeyword = null;
		loadPageData();
	}

	/**
	 * Xử lý khi chọn row trong table
	 */
	private void handleTableSelection() {
		int row = table.getSelectedRow();
		if (row >= 0) {
			int maSP = (Integer) tableModel.getValueAt(row, 0);
			var sp = dao.findById(maSP);
			if (sp != null) {
				fillForm(sp);
			}
		}
	}

	/**
	 * Điền form với dữ liệu sản phẩm
	 */
	private void fillForm(SanPham sp) {
		txtMaSP.setText(String.valueOf(sp.getMaSanPham()));
		txtTenSP.setText(sp.getTenSanPham());
		txtDonViTinh.setText(sp.getDonViTinh());
		txtGiaBan.setText(sp.getGiaBanDeXuat().toString());
		comboLoaiSP.setSelectedItem(sp.getLoaiSanPham());
		txtMoTa.setText(sp.getMoTa() != null ? sp.getMoTa() : "");
		txtMucTonToiThieu.setText(String.valueOf(sp.getMucTonToiThieu()));
	}

	/**
	 * Xử lý thêm mới
	 */
	private void handleThem() {
		if (!validateForm()) {
			return;
		}

		var sp = new SanPham();
		sp.setTenSanPham(txtTenSP.getText().trim());
		sp.setDonViTinh(txtDonViTinh.getText().trim());
		try {
			sp.setGiaBanDeXuat(new BigDecimal(txtGiaBan.getText().trim()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Giá bán không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}
		sp.setLoaiSanPham((String) comboLoaiSP.getSelectedItem());
		sp.setMoTa(txtMoTa.getText().trim());
		try {
			sp.setMucTonToiThieu(Integer.parseInt(txtMucTonToiThieu.getText().trim()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Mức tồn tối thiểu không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (dao.insert(sp)) {
			JOptionPane.showMessageDialog(this, "Thêm sản phẩm thành công!", "Thông báo",
					JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData(); // Reset về trang 1
		} else {
			JOptionPane.showMessageDialog(this, "Thêm sản phẩm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Xử lý cập nhật
	 */
	private void handleSua() {
		if (txtMaSP.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm cần sửa!", "Thông báo",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (!validateForm()) {
			return;
		}

		var sp = new SanPham();
		sp.setMaSanPham(Integer.parseInt(txtMaSP.getText()));
		sp.setTenSanPham(txtTenSP.getText().trim());
		sp.setDonViTinh(txtDonViTinh.getText().trim());
		try {
			sp.setGiaBanDeXuat(new BigDecimal(txtGiaBan.getText().trim()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Giá bán không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}
		sp.setLoaiSanPham((String) comboLoaiSP.getSelectedItem());
		sp.setMoTa(txtMoTa.getText().trim());
		try {
			sp.setMucTonToiThieu(Integer.parseInt(txtMucTonToiThieu.getText().trim()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Mức tồn tối thiểu không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (dao.update(sp)) {
			JOptionPane.showMessageDialog(this, "Cập nhật sản phẩm thành công!", "Thông báo",
					JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadPageData(); // Giữ nguyên trang hiện tại khi update
		} else {
			JOptionPane.showMessageDialog(this, "Cập nhật sản phẩm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Xử lý xóa
	 */
	private void handleXoa() {
		if (txtMaSP.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm cần xóa!", "Thông báo",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		int option = JOptionPane.showConfirmDialog(this,
				"Bạn có chắc chắn muốn xóa sản phẩm này?",
				"Xác nhận xóa",
				JOptionPane.YES_NO_OPTION);

		if (option == JOptionPane.YES_OPTION) {
			int maSP = Integer.parseInt(txtMaSP.getText());
			if (dao.delete(maSP)) {
				JOptionPane.showMessageDialog(this, "Xóa sản phẩm thành công!", "Thông báo",
						JOptionPane.INFORMATION_MESSAGE);
				handleLamMoi();
				loadPageData(); // Reload trang hiện tại
			} else {
				JOptionPane.showMessageDialog(this, "Xóa sản phẩm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Xử lý làm mới
	 */
	private void handleLamMoi() {
		txtMaSP.setText("");
		txtTenSP.setText("");
		txtDonViTinh.setText("Hộp");
		txtGiaBan.setText("");
		comboLoaiSP.setSelectedIndex(0);
		txtMoTa.setText("");
		txtMucTonToiThieu.setText("10");
		table.clearSelection();
	}

	/**
	 * Xử lý tìm kiếm - tích hợp phân trang
	 * Keyword được truyền xuống SP, phân trang tính trên kết quả tìm
	 */
	private void handleTimKiem() {
		String keyword = txtTimKiem.getText().trim();
		if (keyword.isEmpty()) {
			// Xóa keyword → về chế độ xem tất cả
			currentKeyword = null;
		} else {
			currentKeyword = keyword;
		}

		// Reset về trang 1 khi tìm kiếm
		currentPage = 1;
		loadPageData();
	}

	/**
	 * Validate form
	 */
	private boolean validateForm() {
		if (txtTenSP.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập tên sản phẩm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtTenSP.requestFocus();
			return false;
		}

		if (txtGiaBan.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập giá bán!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtGiaBan.requestFocus();
			return false;
		}

		try {
			BigDecimal gia = new BigDecimal(txtGiaBan.getText().trim());
			if (gia.compareTo(BigDecimal.ZERO) < 0) {
				JOptionPane.showMessageDialog(this, "Giá bán phải >= 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
				txtGiaBan.requestFocus();
				return false;
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Giá bán không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtGiaBan.requestFocus();
			return false;
		}

		return true;
	}
}
