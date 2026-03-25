package common;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DocumentPrinter — In phiếu Trả hàng NCC / Biên bản Hủy hàng
 *
 * Sử dụng Java Graphics2D (zero dependencies).
 * Hỗ trợ: in máy in / xem trước (print preview dialog).
 */
public class DocumentPrinter {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /**
     * Hiện dialog hỏi "Bạn có muốn in phiếu?" → nếu có, in.
     */
    public static void askAndPrint(Component parent, DocumentData data) {
        int choice = JOptionPane.showConfirmDialog(parent,
                "Bạn có muốn in phiếu " +
                (data.isReturn ? "xuất trả hàng" : "biên bản tiêu hủy") + "?",
                "In phiếu", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            doPrint(data);
        }
    }

    /**
     * Gửi phiếu tới máy in (hiện dialog chọn printer).
     */
    private static void doPrint(DocumentData data) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(data.isReturn ? "Phieu_Tra_Hang_" + data.maLo : "Bien_Ban_Huy_" + data.maLo);

        PageFormat pf = job.defaultPage();
        Paper paper = pf.getPaper();
        // A5 landscape: 210 x 148mm → points
        paper.setSize(595, 420);
        paper.setImageableArea(36, 36, 523, 348);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);

        job.setPrintable(new ReceiptPrintable(data), pf);

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(null,
                        "In phiếu thành công!", "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(null,
                        "Lỗi in: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================================================================
    //  DATA CLASS
    // ================================================================

    public static class DocumentData {
        public boolean isReturn;
        // Common
        public int maLo;
        public String tenSP;
        public String soLo;
        public int soLuong;
        public long donGia;
        public long tongTien;
        public String lyDo;
        public String nguoiThucHien;
        // Return specific
        public String tenNCC;
        public String hinhThucHoan;
        // Destroy specific
        public String phanLoaiLyDo;
    }

    // ================================================================
    //  PRINTABLE (Graphics2D rendering)
    // ================================================================

    private static class ReceiptPrintable implements Printable {
        private final DocumentData d;

        ReceiptPrintable(DocumentData d) { this.d = d; }

        @Override
        public int print(Graphics graphics, PageFormat pf, int pageIndex) {
            if (pageIndex > 0) return NO_SUCH_PAGE;

            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.translate(pf.getImageableX(), pf.getImageableY());

            int w = (int) pf.getImageableWidth();
            int y = 0;

            // === HEADER ===
            Font fontTitle = new Font("Segoe UI", Font.BOLD, 16);
            Font fontSubtitle = new Font("Segoe UI", Font.PLAIN, 9);
            Font fontLabel = new Font("Segoe UI", Font.PLAIN, 10);
            Font fontValue = new Font("Segoe UI", Font.BOLD, 10);
            Font fontBig = new Font("Segoe UI", Font.BOLD, 14);
            Font fontSmall = new Font("Segoe UI", Font.PLAIN, 8);

            // Company name
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g.setColor(new Color(0x1B, 0x3A, 0x5C));
            g.drawString("APOTHECARY PRO", 0, y += 16);
            g.setFont(fontSubtitle);
            g.setColor(Color.GRAY);
            g.drawString("Hệ thống Quản lý Nhà thuốc", 0, y += 14);

            // Line
            y += 8;
            g.setColor(new Color(0x1B, 0x3A, 0x5C));
            g.fillRect(0, y, w, 2);
            y += 10;

            // Document title
            g.setFont(fontTitle);
            g.setColor(d.isReturn ? new Color(0x17, 0xA2, 0xB8) : new Color(0xDC, 0x35, 0x45));
            String title = d.isReturn ? "PHIẾU XUẤT TRẢ HÀNG NCC" : "BIÊN BẢN TIÊU HỦY THUỐC";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (w - fm.stringWidth(title)) / 2, y += 18);

            // Date
            g.setFont(fontSmall);
            g.setColor(Color.GRAY);
            String dateStr = "Ngày: " + LocalDateTime.now().format(DT_FMT);
            g.drawString(dateStr, (w - g.getFontMetrics().stringWidth(dateStr)) / 2, y += 16);

            y += 12;

            // === INFO TABLE ===
            int labelX = 10;
            int valueX = 150;
            int lineH = 18;

            y = drawInfoRow(g, "Mã Lô:", String.valueOf(d.maLo), labelX, valueX, y, lineH, fontLabel, fontValue);
            y = drawInfoRow(g, "Tên sản phẩm:", d.tenSP, labelX, valueX, y, lineH, fontLabel, fontValue);
            y = drawInfoRow(g, "Số lô:", d.soLo, labelX, valueX, y, lineH, fontLabel, fontValue);
            y = drawInfoRow(g, d.isReturn ? "Số lượng trả:" : "Số lượng hủy:",
                    String.format("%,d", d.soLuong), labelX, valueX, y, lineH, fontLabel, fontValue);
            y = drawInfoRow(g, d.isReturn ? "Đơn giá nhập:" : "Đơn giá vốn:",
                    MONEY_FMT.format(d.donGia) + " VNĐ", labelX, valueX, y, lineH, fontLabel, fontValue);

            if (d.isReturn) {
                y = drawInfoRow(g, "Nhà cung cấp:", d.tenNCC, labelX, valueX, y, lineH, fontLabel, fontValue);
                y = drawInfoRow(g, "Hình thức hoàn:", d.hinhThucHoan, labelX, valueX, y, lineH, fontLabel, fontValue);
            } else {
                y = drawInfoRow(g, "Phân loại:", d.phanLoaiLyDo, labelX, valueX, y, lineH, fontLabel, fontValue);
            }

            // Separator
            y += 6;
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(10, y, w - 10, y);
            y += 10;

            // === TOTAL ===
            g.setFont(fontBig);
            g.setColor(d.isReturn ? new Color(0x28, 0xA7, 0x45) : new Color(0xDC, 0x35, 0x45));
            String totalLabel = d.isReturn ? "TỔNG TIỀN HOÀN:" : "TỔNG THIỆT HẠI:";
            String totalValue = MONEY_FMT.format(d.tongTien) + " VNĐ";
            g.drawString(totalLabel, labelX, y += 18);
            g.drawString(totalValue, valueX, y);

            y += 10;
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(10, y, w - 10, y);
            y += 10;

            // === LÝ DO ===
            g.setFont(fontLabel);
            g.setColor(Color.DARK_GRAY);
            g.drawString("Lý do:", labelX, y += 14);
            g.setFont(fontValue);
            g.setColor(Color.BLACK);
            // Word wrap ly do
            y = drawWrappedText(g, d.lyDo, labelX, y + 14, w - 20, fontValue);

            // === SIGNATURES ===
            y += 30;
            g.setFont(fontLabel);
            g.setColor(Color.DARK_GRAY);
            g.drawString("Người lập phiếu", labelX, y);
            g.drawString(d.isReturn ? "Đại diện NCC" : "Người chứng kiến", w - 130, y);

            y += 14;
            g.setFont(fontValue);
            g.setColor(Color.BLACK);
            g.drawString(d.nguoiThucHien, labelX, y);
            g.drawString("(Ký, ghi rõ họ tên)", w - 130, y);

            y += 10;
            g.setFont(fontSmall);
            g.setColor(Color.GRAY);
            g.drawString("(Ký, ghi rõ họ tên)", labelX, y);

            return PAGE_EXISTS;
        }

        private int drawInfoRow(Graphics2D g, String label, String value,
                                int lx, int vx, int y, int h,
                                Font fLabel, Font fValue) {
            y += h;
            g.setFont(fLabel);
            g.setColor(Color.GRAY);
            g.drawString(label, lx, y);
            g.setFont(fValue);
            g.setColor(Color.BLACK);
            g.drawString(value != null ? value : "---", vx, y);
            return y;
        }

        private int drawWrappedText(Graphics2D g, String text, int x, int y, int maxW, Font font) {
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            if (text == null) text = "";

            String[] words = text.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(test) <= maxW) {
                    if (line.length() > 0) line.append(" ");
                    line.append(word);
                } else {
                    g.drawString(line.toString(), x, y);
                    y += fm.getHeight();
                    line = new StringBuilder(word);
                }
            }
            if (line.length() > 0) {
                g.drawString(line.toString(), x, y);
                y += fm.getHeight();
            }
            return y;
        }
    }
}
