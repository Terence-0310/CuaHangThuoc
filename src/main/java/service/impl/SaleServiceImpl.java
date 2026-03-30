package service.impl;

import common.Session;
import domain.dto.CartItem;
import domain.entity.Batch;
import domain.entity.Customer;
import domain.entity.Invoice;
import domain.entity.InvoiceDetail;
import domain.repository.IBatchRepository;
import domain.repository.ICustomerRepository;
import domain.repository.IInvoiceDetailRepository;
import domain.repository.IInvoiceRepository;
import infrastructure.database.DatabaseHelper;
import service.ISaleService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service Impl: Bán hàng POS + FEFO Logic (★ Core Business)
 * 
 * 🔴 FIX 1: Snapshot GiaVon từ LoHang vào ChiTietHoaDon
 * 🔴 FIX 2: PhuongThucTT (TienMat/ChuyenKhoan/QR) 
 * ⚠️  FIX 3: FEFO tách dòng tự động (đã có từ đầu)
 * ⚠️  FIX 4: UPDLOCK chống Race Condition
 */
public class SaleServiceImpl implements ISaleService {

    private final IInvoiceRepository invoiceRepo;
    private final IInvoiceDetailRepository detailRepo;
    private final IBatchRepository batchRepo;
    private final ICustomerRepository customerRepo;

    public SaleServiceImpl(
            IInvoiceRepository invoiceRepo,
            IInvoiceDetailRepository detailRepo,
            IBatchRepository batchRepo,
            ICustomerRepository customerRepo) {
        this.invoiceRepo = invoiceRepo;
        this.detailRepo = detailRepo;
        this.batchRepo = batchRepo;
        this.customerRepo = customerRepo;
    }

    @Override
    public int checkout(List<CartItem> cart, String soDT, String tenKH, String gioiTinh, String phuongThucTT) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);     // ★ BEGIN TRANSACTION

            // 1. Find or Create Customer
            Integer maKH = null;
            if (soDT != null && !soDT.trim().isEmpty()) {
                Customer kh = customerRepo.findByPhone(conn, soDT.trim());
                if (kh == null) {
                    Customer newKH = new Customer();
                    newKH.setSoDT(soDT.trim());
                    newKH.setTenKH(tenKH != null ? tenKH.trim() : null);
                    newKH.setGioiTinh(gioiTinh != null ? gioiTinh.trim() : null);
                    maKH = customerRepo.insert(conn, newKH);
                } else {
                    maKH = kh.getMaKH();
                }
            }

            // 2. Create Invoice (★ FIX 2: phuongThucTT)
            Invoice invoice = new Invoice(maKH, Session.getCurrentUser().getMaND(), phuongThucTT);
            int maHD = invoiceRepo.insert(conn, invoice);

            // 3. Process each CartItem with FEFO (★ FIX 3: tách dòng + FIX 1: giaVon)
            for (CartItem item : cart) {
                if (!processCartItemFEFO(conn, maHD, item)) {
                    conn.rollback();
                    throw new IllegalArgumentException(
                            "Không đủ tồn kho cho: " + item.getTenSP());
                }
            }

            // 4. Update Invoice total
            invoiceRepo.updateTotal(conn, maHD);
            conn.commit();              // ★ COMMIT TRANSACTION
            return maHD;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new RuntimeException("Lỗi khi thanh toán: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * FEFO: Trừ kho từ lô hết hạn sớm nhất trước
     * ★ FIX 1: Lấy giaVon (giá nhập) từ lô hàng, snapshot vào ChiTietHoaDon
     * ★ FIX 3: Tự động tách thành nhiều dòng nếu 1 lô không đủ
     * ★ FIX 4: UPDLOCK chống 2 nhân viên bán cùng lúc (nằm trong getFEFO query)
     */
    private boolean processCartItemFEFO(Connection conn, int maHD, CartItem item) throws SQLException {
        int remaining = item.getSoLuong();
        
        // ★ FIX 4: getFEFO dùng WITH (UPDLOCK) — khóa dòng tránh race condition
        List<Batch> lots = batchRepo.getFEFO(conn, item.getMaSP());

        for (Batch lot : lots) {
            if (remaining <= 0) break;

            int deduct = Math.min(remaining, lot.getSoLuong());

            // Trừ kho
            batchRepo.updateQuantity(conn, lot.getMaLo(), lot.getSoLuong() - deduct);

            // ★ FIX 1: Snapshot giaVon từ lô hàng 
            InvoiceDetail detail = new InvoiceDetail(
                    maHD, lot.getMaLo(), item.getMaSP(), deduct,
                    item.getGiaBan(),       // DonGia (giá bán)
                    lot.getGiaNhap()        // ★ GiaVon (giá vốn snapshot từ lô)
            );
            detailRepo.insert(conn, detail);

            remaining -= deduct;
        }

        return remaining <= 0;
    }

    @Override
    public List<Object[]> searchProductsForSale(String keyword) {
        List<Object[]> result = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaBan, " +
            "ISNULL(tk.TongTon, 0) AS TonKho " +
            "FROM SanPham sp " +
            "LEFT JOIN (SELECT MaSP, SUM(SoLuong) AS TongTon FROM LoHang " +
            "           WHERE SoLuong > 0 AND HanSuDung > GETDATE() GROUP BY MaSP) tk " +
            "ON sp.MaSP = tk.MaSP " +
            "WHERE sp.TrangThai = 1 AND ISNULL(tk.TongTon, 0) > 0 ");
        if (keyword != null) {
            sql.append("AND sp.TenSP LIKE ? ");
        }
        sql.append("ORDER BY sp.TenSP");

        java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
        try (Connection conn = DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (keyword != null) {
                ps.setNString(1, "%" + keyword + "%");
            }
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.math.BigDecimal gia = rs.getBigDecimal("GiaBan");
                    result.add(new Object[]{
                        rs.getInt("MaSP"),
                        rs.getNString("TenSP"),
                        rs.getNString("DonViTinh"),
                        gia != null ? fmt.format(gia) + " ₫" : "---",
                        rs.getInt("TonKho")
                    });
                }
            }
        } catch (SQLException ignored) {}
        return result;
    }

    @Override
    public Customer findCustomerByPhone(String phone) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            return customerRepo.findByPhone(conn, phone);
        } catch (SQLException e) {
            return null;
        }
    }
}
