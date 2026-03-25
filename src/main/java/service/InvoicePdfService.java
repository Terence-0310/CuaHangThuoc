package service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import infrastructure.database.DatabaseHelper;

import java.awt.Color;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.time.format.DateTimeFormatter;

/**
 * Generates invoice PDF files for completed sales.
 */
public class InvoicePdfService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0x17, 0xA2, 0xB8));
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font FONT_NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font FONT_SMALL = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font FONT_TOTAL = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(0x27, 0xAE, 0x60));

    /**
     * Generate invoice PDF and save to the given file path.
     * @return the file path of the generated PDF
     */
    public static String generate(int maHD, String outputPath) throws Exception {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(outputPath));
        doc.open();

        // ---- HEADER ----
        Paragraph pTitle = new Paragraph("HOA DON BAN HANG", FONT_TITLE);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        doc.add(pTitle);

        Paragraph pStore = new Paragraph("Apothecary Pro - Pharmacy Management", FONT_SMALL);
        pStore.setAlignment(Element.ALIGN_CENTER);
        doc.add(pStore);
        doc.add(new Paragraph(" "));

        // ---- INVOICE INFO ----
        String sqlHeader =
            "SELECT hd.MaHD, hd.NgayBan, hd.TongTien, hd.PhuongThucTT, " +
            "ISNULL(kh.TenKH, N'Khach vang lai') AS TenKH, " +
            "ISNULL(kh.SoDT, '---') AS SoDT, " +
            "nd.HoTen AS TenNV " +
            "FROM HoaDon hd " +
            "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
            "JOIN NguoiDung nd ON hd.MaND = nd.MaND " +
            "WHERE hd.MaHD = ?";

        String tenKH = "---", soDT = "---", tenNV = "---", pttt = "---", ngay = "---";
        BigDecimal tongTien = BigDecimal.ZERO;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlHeader)) {
            ps.setInt(1, maHD);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("NgayBan");
                    if (ts != null) ngay = ts.toLocalDateTime().format(DT_FMT);
                    tongTien = rs.getBigDecimal("TongTien");
                    pttt = rs.getNString("PhuongThucTT");
                    tenKH = rs.getNString("TenKH");
                    soDT = rs.getString("SoDT");
                    tenNV = rs.getNString("TenNV");
                }
            }
        }

        // Info table
        PdfPTable infoTbl = new PdfPTable(2);
        infoTbl.setWidthPercentage(100);
        infoTbl.setWidths(new float[]{1, 2});

        addInfoRow(infoTbl, "Ma hoa don:", "#" + maHD);
        addInfoRow(infoTbl, "Ngay ban:", ngay);
        addInfoRow(infoTbl, "Khach hang:", tenKH + " (" + soDT + ")");
        addInfoRow(infoTbl, "Nhan vien:", tenNV);
        addInfoRow(infoTbl, "Phuong thuc:", pttt);
        doc.add(infoTbl);
        doc.add(new Paragraph(" "));

        // ---- DETAIL TABLE ----
        PdfPTable tbl = new PdfPTable(5);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{3, 1.5f, 0.8f, 1.2f, 1.5f});

        Color headerBg = new Color(0x2C, 0x3E, 0x50);
        addHeaderCell(tbl, "San pham", headerBg);
        addHeaderCell(tbl, "So Lo", headerBg);
        addHeaderCell(tbl, "SL", headerBg);
        addHeaderCell(tbl, "Don gia", headerBg);
        addHeaderCell(tbl, "Thanh tien", headerBg);

        String sqlDetail =
            "SELECT sp.TenSP, l.SoLo, ct.SoLuong, ct.DonGia, ct.ThanhTien " +
            "FROM ChiTietHoaDon ct " +
            "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
            "JOIN LoHang l ON ct.MaLo = l.MaLo " +
            "WHERE ct.MaHD = ?";

        boolean alt = false;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
            ps.setInt(1, maHD);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Color bg = alt ? new Color(0xF5, 0xF5, 0xF5) : Color.WHITE;
                    addCell(tbl, rs.getNString("TenSP"), FONT_NORMAL, Element.ALIGN_LEFT, bg);
                    addCell(tbl, rs.getNString("SoLo"), FONT_NORMAL, Element.ALIGN_LEFT, bg);
                    addCell(tbl, String.valueOf(rs.getInt("SoLuong")), FONT_NORMAL, Element.ALIGN_CENTER, bg);
                    BigDecimal dg = rs.getBigDecimal("DonGia");
                    BigDecimal tt = rs.getBigDecimal("ThanhTien");
                    addCell(tbl, dg != null ? String.format("%,.0f", dg) : "0", FONT_NORMAL, Element.ALIGN_RIGHT, bg);
                    addCell(tbl, tt != null ? String.format("%,.0f", tt) : "0", FONT_BOLD, Element.ALIGN_RIGHT, bg);
                    alt = !alt;
                }
            }
        }
        doc.add(tbl);
        doc.add(new Paragraph(" "));

        // ---- TOTAL ----
        Paragraph pTotal = new Paragraph(
                "TONG TIEN: " + (tongTien != null ? String.format("%,.0f VND", tongTien) : "0 VND"),
                FONT_TOTAL);
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        doc.add(pTotal);

        doc.add(new Paragraph(" "));
        Paragraph pThank = new Paragraph("Cam on quy khach!", FONT_SMALL);
        pThank.setAlignment(Element.ALIGN_CENTER);
        doc.add(pThank);

        doc.close();
        return outputPath;
    }

    private static void addInfoRow(PdfPTable tbl, String label, String value) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, FONT_BOLD));
        c1.setBorder(0);
        c1.setPadding(4);
        tbl.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, FONT_NORMAL));
        c2.setBorder(0);
        c2.setPadding(4);
        tbl.addCell(c2);
    }

    private static void addHeaderCell(PdfPTable tbl, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        tbl.addCell(cell);
    }

    private static void addCell(PdfPTable tbl, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        tbl.addCell(cell);
    }
}
