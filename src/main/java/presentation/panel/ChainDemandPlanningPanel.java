package presentation.panel;

import common.AppColors;
import common.ServiceFactory;
import domain.dto.InternalTransferSuggestionDTO;
import domain.dto.ReplenishmentSuggestionDTO;
import domain.entity.Branch;
import domain.entity.Region;
import service.IBranchService;
import service.IDemandPlanningService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Màn hình quản trị: chi nhánh chuỗi + gợi ý nhập hàng / điều chuyển từ datamart dự báo nhu cầu.
 */
public class ChainDemandPlanningPanel extends JPanel {

    private final IBranchService branchService = ServiceFactory.getBranchService();
    private final IDemandPlanningService demandPlanningService = ServiceFactory.getDemandPlanningService();

    private DefaultTableModel modelRegions;
    private DefaultTableModel modelBranches;
    private DefaultTableModel modelReplenish;
    private DefaultTableModel modelTransfers;
    private JLabel lblStatus;

    public ChainDemandPlanningPanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.NEUTRAL);
        initUi();
        reloadStaticTables();
        reloadSuggestions();
    }

    private void initUi() {
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("Chuỗi nhà thuốc & Dự báo nhu cầu");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(AppColors.TEXT_PRIMARY);
        top.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnReload = new JButton("Tải lại danh sách");
        styleButton(btnReload);
        btnReload.addActionListener(e -> {
            reloadStaticTables();
            reloadSuggestions();
        });

        JButton btnPipeline = new JButton("Chạy pipeline (datamart → dự báo → tối ưu)");
        styleButton(btnPipeline);
        btnPipeline.addActionListener(e -> runPipelineAsync());

        actions.add(btnReload);
        actions.add(btnPipeline);
        top.add(actions, BorderLayout.EAST);

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(AppColors.TEXT_SECONDARY);
        lblStatus.setBorder(new EmptyBorder(0, 24, 8, 24));

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(top, BorderLayout.CENTER);
        north.add(lblStatus, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        modelRegions = new DefaultTableModel(
                new Object[]{"Mã KV", "Mã code", "Tên khu vực", "Tỉnh/TP", "Hoạt động"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        modelBranches = new DefaultTableModel(
                new Object[]{"Mã CN", "Mã code", "Tên chi nhánh", "Khu vực", "Địa chỉ", "Hoạt động"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        modelReplenish = new DefaultTableModel(
                new Object[]{"Mã SP", "Mã CN", "Cửa sổ (ngày)", "Tồn hiện tại", "Nhu cầu", "Đề xuất nhập"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        modelTransfers = new DefaultTableModel(
                new Object[]{"Mã SP", "CN nguồn", "CN đích", "Số lượng", "Lý do"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabs.addTab("Khu vực", wrapScroll(new JTable(modelRegions)));
        tabs.addTab("Chi nhánh", wrapScroll(new JTable(modelBranches)));
        tabs.addTab("Gợi ý nhập hàng", wrapScroll(new JTable(modelReplenish)));
        tabs.addTab("Gợi ý điều chuyển", wrapScroll(new JTable(modelTransfers)));

        add(tabs, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                reloadStaticTables();
                reloadSuggestions();
            }
        });
    }

    private static void styleButton(JButton b) {
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setFocusPainted(false);
    }

    private static JScrollPane wrapScroll(JTable t) {
        t.setFillsViewportHeight(true);
        t.setRowHeight(22);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new EmptyBorder(8, 16, 16, 16));
        return sp;
    }

    private void reloadStaticTables() {
        try {
            modelRegions.setRowCount(0);
            for (Region r : branchService.getRegions()) {
                modelRegions.addRow(new Object[]{
                        r.getMaKhuVuc(),
                        r.getMaKhuVucCode(),
                        r.getTenKhuVuc(),
                        r.getTinhThanh(),
                        r.isTrangThai() ? "Có" : "Không"
                });
            }
            modelBranches.setRowCount(0);
            for (Branch b : branchService.getBranches()) {
                modelBranches.addRow(new Object[]{
                        b.getMaCN(),
                        b.getMaCNCode(),
                        b.getTenChiNhanh(),
                        b.getTenKhuVuc(),
                        b.getDiaChi(),
                        b.isTrangThai() ? "Có" : "Không"
                });
            }
            lblStatus.setText("Đã tải danh sách khu vực / chi nhánh.");
        } catch (Exception ex) {
            String detail = rootCauseMessage(ex);
            lblStatus.setText("Lỗi tải chi nhánh: " + detail);
            JOptionPane.showMessageDialog(this,
                    "Không tải được dữ liệu chi nhánh (bảng KhuVuc / ChiNhanh).\n\n"
                            + "Chạy migration: mở PowerShell trong thư mục database, gõ:\n"
                            + "  .\\apply_chain_pharmacy_migrations.ps1\n"
                            + "hoặc chạy đủ: .\\run_all_migrations.ps1\n\n"
                            + "Chi tiết: " + detail,
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reloadSuggestions() {
        try {
            modelReplenish.setRowCount(0);
            List<ReplenishmentSuggestionDTO> rep = demandPlanningService.getLatestReplenishmentSuggestions();
            for (ReplenishmentSuggestionDTO dto : rep) {
                modelReplenish.addRow(new Object[]{
                        dto.getMaSP(),
                        dto.getMaCN(),
                        dto.getForecastWindowDays(),
                        dto.getCurrentStock(),
                        dto.getRequiredQty(),
                        dto.getSuggestedQty()
                });
            }
            modelTransfers.setRowCount(0);
            List<InternalTransferSuggestionDTO> tr = demandPlanningService.getLatestTransferSuggestions();
            for (InternalTransferSuggestionDTO dto : tr) {
                modelTransfers.addRow(new Object[]{
                        dto.getMaSP(),
                        dto.getFromMaCN(),
                        dto.getToMaCN(),
                        dto.getSuggestedQty(),
                        dto.getReason() != null ? dto.getReason() : ""
                });
            }
            lblStatus.setText(String.format("Gợi ý: %d dòng nhập hàng, %d dòng điều chuyển.",
                    rep.size(), tr.size()));
        } catch (Exception ex) {
            String detail = rootCauseMessage(ex);
            lblStatus.setText("Lỗi tải gợi ý: " + detail);
            JOptionPane.showMessageDialog(this,
                    "Không đọc được bảng gợi ý (ReplenishmentSuggestion / InternalTransferSuggestion).\n"
                            + "Chạy pipeline sau khi datamart đã có dữ liệu, hoặc chạy migration 41–44.\n\n"
                            + detail,
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void runPipelineAsync() {
        btnSetEnabledRecursive(this, false);
        lblStatus.setText("Đang chạy pipeline trên máy chủ SQL…");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                LocalDate to = LocalDate.now();
                LocalDate from = to.minusDays(120);
                demandPlanningService.refreshDataMart(from, to);
                demandPlanningService.generateForecast(14, 30);
                demandPlanningService.runOptimization(14);
                return null;
            }

            @Override
            protected void done() {
                btnSetEnabledRecursive(ChainDemandPlanningPanel.this, true);
                try {
                    get();
                    lblStatus.setText("Pipeline hoàn tất.");
                    reloadSuggestions();
                    JOptionPane.showMessageDialog(ChainDemandPlanningPanel.this,
                            "Đã làm mới datamart, sinh dự báo 14 ngày và chạy tối ưu.",
                            "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    lblStatus.setText("Pipeline lỗi: " + c.getMessage());
                    JOptionPane.showMessageDialog(ChainDemandPlanningPanel.this,
                            "Pipeline thất bại:\n" + c.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private static void btnSetEnabledRecursive(Container root, boolean en) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton) {
                c.setEnabled(en);
            }
            if (c instanceof Container) {
                btnSetEnabledRecursive((Container) c, en);
            }
        }
    }

    private static String rootCauseMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        return msg != null ? msg : t.getClass().getSimpleName();
    }
}
