package service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import dao.ThongKeDao;
import dao.ThongKeDao.*;

/**
 * DashboardService - Tầng service cho Dashboard Tổng Quan Nhà Thuốc.
 * Đóng vai trò trung gian giữa DashboardPanel (UI) và ThongKeDao (Data).
 * Xử lý logic nghiệp vụ trước khi trả dữ liệu cho UI.
 *
 * @version 1.0
 */
public class DashboardService {

    private final ThongKeDao thongKeDao;

    public DashboardService() {
        this.thongKeDao = new ThongKeDao();
    }

    // ===================== KPI SUMMARY =====================

    /** Lấy thống kê doanh thu & hóa đơn hôm nay */
    public ThongKeNgay getThongKeHomNay() {
        try {
            ThongKeNgay result = thongKeDao.getThongKeNgay();
            return result != null ? result : new ThongKeNgay(0, BigDecimal.ZERO);
        } catch (Exception e) {
            e.printStackTrace();
            return new ThongKeNgay(0, BigDecimal.ZERO);
        }
    }

    /** Lấy doanh thu hôm qua (dùng để so sánh trend) */
    public BigDecimal getDoanhThuHomQua() {
        try {
            BigDecimal result = thongKeDao.getDoanhThuHomQua();
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    /** Tổng sản phẩm đang kinh doanh */
    public int getTongSanPham() {
        try {
            return thongKeDao.getTongSanPham();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** Số sản phẩm dưới mức tồn tối thiểu */
    public int getSoSanPhamSapHet() {
        try {
            return thongKeDao.getSoSanPhamTonThap();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** Số lô sắp hết hạn trong 30 ngày */
    public int getSoLoSapHetHan() {
        try {
            return thongKeDao.getSoLoSapHetHan(30);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** Tổng khách hàng */
    public int getTongKhachHang() {
        try {
            return thongKeDao.getTongKhachHang();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ===================== CẢNH BÁO =====================

    /** Danh sách thuốc tồn kho thấp */
    public List<CanhBaoTonKho> getDanhSachTonKhoThap() {
        try {
            return thongKeDao.getThuocCanNhap();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /** Danh sách lô sắp hết hạn trong 30 ngày */
    public List<CanhBaoHetHan> getDanhSachLoSapHetHan() {
        try {
            return thongKeDao.getThuocSapHetHan(30);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // ===================== HOẠT ĐỘNG GẦN ĐÂY =====================

    /** Danh sách hóa đơn gần nhất */
    public List<HoaDonGanDay> getHoaDonGanDay() {
        try {
            return thongKeDao.getHoaDonGanDay(10);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // ===================== THỐNG KÊ PHỤ =====================

    /** Top 5 sản phẩm bán chạy hôm nay */
    public List<SanPhamBanChay> getTopSanPhamBanChayHomNay() {
        try {
            return thongKeDao.getTopSanPhamBanChayHomNay(5);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /** Tỷ lệ tồn kho theo trạng thái */
    public Map<String, Integer> getTyLeTonKho() {
        try {
            return thongKeDao.getTyLeTonKho();
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }
}
