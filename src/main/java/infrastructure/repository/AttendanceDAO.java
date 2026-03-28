package infrastructure.repository;

import domain.entity.AttendanceHistory;
import domain.entity.Employee;
import domain.entity.Schedule;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    /** Validate PIN và lấy Employee */
    public Employee getEmployeeByPin(String pin) {
        String sql = "SELECT * FROM HR_Employees WHERE PinCode = ? AND Status = N'Đang làm'";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Employee e = new Employee();
                e.setEmpID(rs.getInt("EmpID"));
                e.setFullName(rs.getString("FullName"));
                e.setPinCode(rs.getString("PinCode"));
                e.setHourlyRate(rs.getBigDecimal("HourlyRate"));
                e.setStatus(rs.getString("Status"));
                return e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy nhân viên: " + e.getMessage());
        }
        return null;
    }

    /** Lấy lịch SỚM NHẤT trong ngày chưa được chấm công của Employee này */
    public Schedule getNextAvailableSchedule(int empID) {
        String sql = "SELECT TOP 1 s.*, sh.ShiftName " +
                     "FROM HR_Schedules s " +
                     "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
                     "WHERE s.EmpID = ? " +
                     "  AND s.WorkDate = CAST(GETDATE() AS DATE) " +
                     "  AND s.ActualStart IS NOT NULL " +
                     "  AND NOT EXISTS (SELECT 1 FROM HR_Attendances a WHERE a.ScheduleID = s.ScheduleID) " +
                     "ORDER BY s.ActualStart ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Schedule sc = new Schedule();
                sc.setScheduleID(rs.getInt("ScheduleID"));
                sc.setEmpID(rs.getInt("EmpID"));
                sc.setShiftID(rs.getInt("ShiftID"));
                sc.setShiftName(rs.getString("ShiftName"));
                sc.setWorkDate(rs.getDate("WorkDate").toLocalDate());
                sc.setActualStart(rs.getTime("ActualStart").toLocalTime());
                sc.setActualEnd(rs.getTime("ActualEnd").toLocalTime());
                return sc;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra lịch: " + e.getMessage());
        }
        return null;
    }

    /** Lấy toàn bộ các lịch hôm nay CHƯA AI CHẤM CÔNG (để đi làm thay) */
    public List<Schedule> getUnclockedSchedulesToday() {
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT s.*, e.FullName, sh.ShiftName " +
                     "FROM HR_Schedules s " +
                     "JOIN HR_Employees e ON s.EmpID = e.EmpID " +
                     "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
                     "WHERE s.WorkDate = CAST(GETDATE() AS DATE) " +
                     "  AND s.ActualStart IS NOT NULL " +
                     "  AND NOT EXISTS (SELECT 1 FROM HR_Attendances a WHERE a.ScheduleID = s.ScheduleID) " +
                     "ORDER BY s.ActualStart ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Schedule sc = new Schedule();
                sc.setScheduleID(rs.getInt("ScheduleID"));
                sc.setEmpID(rs.getInt("EmpID"));
                sc.setEmpName(rs.getString("FullName"));
                sc.setShiftName(rs.getString("ShiftName"));
                sc.setWorkDate(rs.getDate("WorkDate").toLocalDate());
                sc.setActualStart(rs.getTime("ActualStart").toLocalTime());
                sc.setActualEnd(rs.getTime("ActualEnd").toLocalTime());
                list.add(sc);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách lịch làm thay: " + e.getMessage());
        }
        return list;
    }

    /** Kiểm tra xem nhân viên có đang trong ca (đã Nhận ca, chưa Kết ca) không */
    public boolean isCurrentlyClockedIn(int empID) {
        String sql = "SELECT 1 FROM HR_Attendances WHERE EmpID = ? AND ClockOut IS NULL AND ClockIn IS NOT NULL";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Nhận ca — SNAPSHOT: Chụp HourlyRate, OvertimeRate, giờ ca + giờ mặc định ca tại thời điểm nhận ca */
    public void clockIn(int empID, int scheduleID, String lateReason) {
        String sql = "INSERT INTO HR_Attendances (EmpID, ScheduleID, ClockIn, LateReason, " +
                     "SnapshotRate, SnapshotOTRate, SnapshotStart, SnapshotEnd, ShiftDefaultEnd) " +
                     "SELECT ?, s.ScheduleID, GETDATE(), ?, " +
                     "e.HourlyRate, e.OvertimeRate, s.ActualStart, s.ActualEnd, sh.DefaultEndTime " +
                     "FROM HR_Schedules s " +
                     "JOIN HR_Employees e ON e.EmpID = ? " +
                     "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
                     "WHERE s.ScheduleID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ps.setString(2, lateReason);
            ps.setInt(3, empID);
            ps.setInt(4, scheduleID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi nhận ca: " + e.getMessage());
        }
    }

    /** Dọn dẹp ca cúp điện (Zombie Shift) — Dùng SNAPSHOT thay vì JOIN Schedule */
    public void closeZombieShifts(int empID) {
        String sqlClockOut = 
            "UPDATE a SET " +
            "a.ClockOut = DATEADD(DAY, CASE WHEN a.SnapshotEnd < a.SnapshotStart THEN 1 ELSE 0 END, CAST(CAST(a.ClockIn AS DATE) AS DATETIME)) + CAST(a.SnapshotEnd AS DATETIME), " +
            "a.TotalHours = ROUND(CASE WHEN DATEDIFF(MINUTE, a.ClockIn, " +
            "    DATEADD(DAY, CASE WHEN a.SnapshotEnd < a.SnapshotStart THEN 1 ELSE 0 END, CAST(CAST(a.ClockIn AS DATE) AS DATETIME)) + CAST(a.SnapshotEnd AS DATETIME)" +
            ") < 0 THEN 0 ELSE DATEDIFF(MINUTE, a.ClockIn, " +
            "    DATEADD(DAY, CASE WHEN a.SnapshotEnd < a.SnapshotStart THEN 1 ELSE 0 END, CAST(CAST(a.ClockIn AS DATE) AS DATETIME)) + CAST(a.SnapshotEnd AS DATETIME)" +
            ") / 60.0 END, 2), " +
            "a.LateReason = ISNULL(a.LateReason, '') + ' | Hệ thống tự chốt do quên Kết Ca' " +
            "FROM HR_Attendances a " +
            "WHERE a.EmpID = ? AND a.ClockOut IS NULL AND a.ClockIn IS NOT NULL " +
            "  AND CAST(a.ClockIn AS DATE) < CAST(GETDATE() AS DATE)";

        String sqlMoney = 
            "UPDATE a SET a.DailyEarned = a.TotalHours * a.SnapshotRate " +
            "FROM HR_Attendances a " +
            "WHERE a.EmpID = ? AND a.LateReason LIKE '%Hệ thống tự chốt do quên Kết Ca'";

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sqlClockOut);
                 PreparedStatement ps2 = conn.prepareStatement(sqlMoney)) {
                
                ps1.setInt(1, empID);
                int affected = ps1.executeUpdate();
                if (affected > 0) {
                    ps2.setInt(1, empID);
                    ps2.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi dọn ca cũ: " + e.getMessage());
        }
    }

    /** Lấy trước số giờ thực tế và số giờ quy định — Dùng SNAPSHOT */
    public double[] checkoutPreview(int empID) {
        String sql = 
            "SELECT " +
            "  DATEDIFF(MINUTE, a.ClockIn, GETDATE()) / 60.0 as ActualHours, " +
            "  DATEDIFF(MINUTE, " +
            "      CAST(CAST(a.ClockIn AS DATE) AS DATETIME) + CAST(a.SnapshotStart AS DATETIME), " +
            "      DATEADD(DAY, CASE WHEN a.SnapshotEnd < a.SnapshotStart THEN 1 ELSE 0 END, " +
            "          CAST(CAST(a.ClockIn AS DATE) AS DATETIME)) + CAST(a.SnapshotEnd AS DATETIME)" +
            "  ) / 60.0 as ScheduledHours " +
            "FROM HR_Attendances a " +
            "WHERE a.EmpID = ? AND a.ClockOut IS NULL AND a.ClockIn IS NOT NULL";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new double[]{rs.getDouble("ActualHours"), rs.getDouble("ScheduledHours")};
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi preview kết ca: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy giờ kết ca theo lịch (WorkDate + SnapshotEnd) cho ca đang mở.
     * Trả về LocalDateTime để so sánh chính xác kể cả qua nửa đêm.
     */
    public java.time.LocalDateTime getActiveShiftEndDateTime(int empID) {
        String sql = "SELECT s.WorkDate, a.SnapshotStart, a.SnapshotEnd " +
                     "FROM HR_Attendances a " +
                     "JOIN HR_Schedules s ON a.ScheduleID = s.ScheduleID " +
                     "WHERE a.EmpID = ? AND a.ClockOut IS NULL AND a.ClockIn IS NOT NULL";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                java.sql.Date wd = rs.getDate("WorkDate");
                Time start = rs.getTime("SnapshotStart");
                Time end = rs.getTime("SnapshotEnd");
                if (wd != null && end != null) {
                    java.time.LocalDate workDate = wd.toLocalDate();
                    java.time.LocalTime endTime = end.toLocalTime();
                    // Nếu giờ kết < giờ bắt đầu → ca qua nửa đêm → cộng 1 ngày
                    if (start != null && endTime.isBefore(start.toLocalTime())) {
                        workDate = workDate.plusDays(1);
                    }
                    return java.time.LocalDateTime.of(workDate, endTime);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /** Kết ca & Tính lương — TỰ ĐỘNG TÁCH GIỜ THƯỜNG / TĂNG CA */
    public void clockOut(int empID, String overtimeReason) {
        // Step 1: ClockOut + TotalHours + OvertimeHours
        String sqlUpdateClockOut =
            "UPDATE a SET " +
            "  a.ClockOut = GETDATE(), " +
            "  a.TotalHours = ROUND(DATEDIFF(MINUTE, a.ClockIn, GETDATE()) / 60.0, 2), " +
            // OT = max(0, totalMinutes - defaultShiftMinutes)
            // DefaultShiftMinutes = DATEDIFF(MINUTE, SnapshotStart, ShiftDefaultEnd) — adjusted for cross-midnight
            "  a.OvertimeHours = CASE " +
            "    WHEN a.ShiftDefaultEnd IS NOT NULL THEN " +
            "      ROUND(GREATEST(0, " +
            "        DATEDIFF(MINUTE, a.ClockIn, GETDATE()) / 60.0 - " +
            "        DATEDIFF(MINUTE, a.SnapshotStart, " +
            "          CASE WHEN a.ShiftDefaultEnd < a.SnapshotStart " +
            "               THEN DATEADD(DAY, 1, CAST(a.ShiftDefaultEnd AS DATETIME)) " +
            "               ELSE CAST(a.ShiftDefaultEnd AS DATETIME) END" +
            "        ) / 60.0" +
            "      ), 2) " +
            "    ELSE 0 END, " +
            "  a.LateReason = ISNULL(a.LateReason, '') + ? " +
            "FROM HR_Attendances a " +
            "WHERE a.EmpID = ? AND a.ClockOut IS NULL AND a.ClockIn IS NOT NULL";

        // Step 2: DailyEarned = regularHrs * rate + OT * otRate
        String sqlUpdateMoney =
            "UPDATE a SET a.DailyEarned = " +
            "  (a.TotalHours - a.OvertimeHours) * a.SnapshotRate + " +
            "  a.OvertimeHours * ISNULL(a.SnapshotOTRate, a.SnapshotRate) " +
            "FROM HR_Attendances a " +
            "WHERE a.EmpID = ? AND a.ClockIn IS NOT NULL AND CAST(a.ClockOut AS DATE) = CAST(GETDATE() AS DATE)";

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sqlUpdateClockOut);
                 PreparedStatement ps2 = conn.prepareStatement(sqlUpdateMoney)) {

                String extra = (overtimeReason != null && !overtimeReason.trim().isEmpty()) ? " | Tăng ca: " + overtimeReason.trim() : "";
                ps1.setString(1, extra);
                ps1.setInt(2, empID);
                int rows = ps1.executeUpdate();
                if (rows == 0) {
                    throw new RuntimeException("Bạn chưa nhận ca hoặc đã kết ca rồi!");
                }

                ps2.setInt(1, empID);
                ps2.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kết ca: " + e.getMessage());
        }
    }

    // ==========================================
    // MODULE: TỰ ĐỘNG ĐÁNH DẤU VẮNG MẶT
    // ==========================================

    /**
     * ★ Tự động quét các lịch đã qua mà không ai nhận ca.
     * Tạo bản ghi "VẮNG MẶT" trong HR_Attendances.
     * Gọi khi mở tab Lịch Sử hoặc khi load Dashboard.
     *
     * Điều kiện:
     *  - WorkDate + ActualEnd < NOW (ca đã kết thúc)
     *  - Không tồn tại bản ghi HR_Attendances cho ScheduleID đó
     */
    public int markAbsentSchedules() {
        String sql =
            "INSERT INTO HR_Attendances (EmpID, ScheduleID, ClockIn, ClockOut, TotalHours, DailyEarned, " +
            "    LateReason, SnapshotRate, SnapshotStart, SnapshotEnd) " +
            "SELECT s.EmpID, s.ScheduleID, NULL, NULL, 0, 0, " +
            "    N'VẮNG MẶT - Không nhận ca', e.HourlyRate, s.ActualStart, s.ActualEnd " +
            "FROM HR_Schedules s " +
            "JOIN HR_Employees e ON s.EmpID = e.EmpID " +
            "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
            "WHERE NOT EXISTS (SELECT 1 FROM HR_Attendances a WHERE a.ScheduleID = s.ScheduleID) " +
            "  AND s.ActualStart IS NOT NULL " +
            "  AND DATEADD(MINUTE, " +
            "      DATEDIFF(MINUTE, 0, s.ActualEnd) + CASE WHEN s.ActualEnd < s.ActualStart THEN 1440 ELSE 0 END, " +
            "      CAST(s.WorkDate AS DATETIME)) < GETDATE()";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[markAbsentSchedules] Lỗi: " + e.getMessage());
            return 0;
        }
    }

    // ==========================================
    // MODULE: LỊCH SỬ CHẤM CÔNG (HR_ADMIN)
    // ==========================================

    public List<AttendanceHistory> getAttendanceHistory(java.time.LocalDate fromDate, java.time.LocalDate toDate, int empId) {
        List<AttendanceHistory> list = new java.util.ArrayList<>();
        String sql = "SELECT a.AttendanceID, e.FullName, s.WorkDate, sh.ShiftName, a.ClockIn, a.ClockOut, a.TotalHours, a.DailyEarned, a.LateReason, ISNULL(a.SnapshotRate, e.HourlyRate) AS HourlyRate " +
                     "FROM HR_Attendances a " +
                     "JOIN HR_Schedules s ON a.ScheduleID = s.ScheduleID " +
                     "JOIN HR_Employees e ON a.EmpID = e.EmpID " +
                     "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
                     "WHERE CAST(s.WorkDate AS DATE) BETWEEN ? AND ? ";
        if (empId > 0) {
            sql += " AND a.EmpID = ? ";
        }
        sql += " ORDER BY s.WorkDate DESC, a.ClockIn DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(fromDate));
            ps.setDate(2, java.sql.Date.valueOf(toDate));
            if (empId > 0) {
                ps.setInt(3, empId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AttendanceHistory h = new AttendanceHistory();
                h.setRecordID(rs.getInt("AttendanceID"));
                h.setEmpName(rs.getString("FullName"));
                java.sql.Date wd = rs.getDate("WorkDate");
                if (wd != null) h.setWorkDate(wd.toLocalDate());
                h.setShiftName(rs.getString("ShiftName"));
                
                java.sql.Timestamp ci = rs.getTimestamp("ClockIn");
                if (ci != null) h.setClockIn(ci.toLocalDateTime());
                
                java.sql.Timestamp co = rs.getTimestamp("ClockOut");
                if (co != null) h.setClockOut(co.toLocalDateTime());
                
                h.setTotalHours(rs.getBigDecimal("TotalHours"));
                h.setDailyEarned(rs.getBigDecimal("DailyEarned"));
                h.setLateReason(rs.getString("LateReason"));
                h.setHourlyRate(rs.getBigDecimal("HourlyRate"));
                list.add(h);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi query lịch sử chấm công: " + e.getMessage());
        }
        return list;
    }

    public void updateManualTime(int recordId, java.time.LocalDateTime newIn, java.time.LocalDateTime newOut, java.math.BigDecimal totalHr, String adminNote) {
        String sql = "UPDATE HR_Attendances SET ClockIn = ?, ClockOut = ?, TotalHours = ?, " +
                     "DailyEarned = ? * ISNULL(SnapshotRate, 0), " +
                     "LateReason = CASE WHEN LateReason IS NULL THEN '' ELSE LateReason + ' | ' END + ? " +
                     "WHERE AttendanceID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(newIn));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(newOut));
            ps.setBigDecimal(3, totalHr);
            ps.setBigDecimal(4, totalHr);  // SQL tự nhân × SnapshotRate
            ps.setString(5, "[Admin sửa: " + adminNote + "]");
            ps.setInt(6, recordId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật giờ thủ công: " + e.getMessage());
        }
    }

    // ==========================================
    // MODULE: ATTENDANCE WITH REVENUE (SUPER DASHBOARD)
    // ==========================================

    /**
     * Lấy lịch sử chấm công + doanh thu hóa đơn trong ca.
     * JOIN HoaDon (qua MaND) với điều kiện NgayBan BETWEEN ClockIn AND ClockOut.
     */
    public List<AttendanceHistory> getAttendanceWithRevenue(java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        List<AttendanceHistory> list = new java.util.ArrayList<>();
        String sql =
            "SELECT a.AttendanceID, e.FullName, e.MaND, s.WorkDate, sh.ShiftName, " +
            "a.ClockIn, a.ClockOut, a.TotalHours, a.DailyEarned, a.LateReason, " +
            "ISNULL(a.SnapshotRate, e.HourlyRate) AS HourlyRate, " +
            "ISNULL(inv.InvoiceCount, 0) AS InvoiceCount, " +
            "ISNULL(inv.TotalRevenue, 0) AS TotalRevenue " +
            "FROM HR_Attendances a " +
            "JOIN HR_Schedules s ON a.ScheduleID = s.ScheduleID " +
            "JOIN HR_Employees e ON a.EmpID = e.EmpID " +
            "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
            "OUTER APPLY ( " +
            "    SELECT COUNT(*) AS InvoiceCount, SUM(hd.TongTien) AS TotalRevenue " +
            "    FROM HoaDon hd " +
            "    WHERE (e.MaND IS NOT NULL AND hd.MaND = e.MaND OR e.MaND IS NULL) " +
            "      AND ISNULL(hd.TrangThai, N'Thanh cong') = N'Thanh cong' " +
            "      AND a.ClockOut IS NOT NULL " +
            "      AND hd.NgayBan BETWEEN a.ClockIn AND a.ClockOut " +
            ") inv " +
            "WHERE CAST(s.WorkDate AS DATE) BETWEEN ? AND ? " +
            "ORDER BY s.WorkDate DESC, a.ClockIn DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(fromDate));
            ps.setDate(2, java.sql.Date.valueOf(toDate));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AttendanceHistory h = new AttendanceHistory();
                h.setRecordID(rs.getInt("AttendanceID"));
                h.setEmpName(rs.getNString("FullName"));
                h.setMaNd(rs.getInt("MaND"));
                h.setWorkDate(rs.getDate("WorkDate").toLocalDate());
                h.setShiftName(rs.getNString("ShiftName"));
                Timestamp ci = rs.getTimestamp("ClockIn");
                if (ci != null) h.setClockIn(ci.toLocalDateTime());
                Timestamp co = rs.getTimestamp("ClockOut");
                if (co != null) h.setClockOut(co.toLocalDateTime());
                h.setTotalHours(rs.getBigDecimal("TotalHours"));
                h.setDailyEarned(rs.getBigDecimal("DailyEarned"));
                h.setLateReason(rs.getString("LateReason"));
                h.setHourlyRate(rs.getBigDecimal("HourlyRate"));
                h.setInvoiceCount(rs.getInt("InvoiceCount"));
                h.setTotalRevenue(rs.getBigDecimal("TotalRevenue"));
                list.add(h);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi query lịch sử + doanh thu: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy danh sách hóa đơn chi tiết trong ca cho ShiftDetailsDialog.
     */
    public List<java.util.Map<String, Object>> getInvoicesInShift(int attendanceId) {
        List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        String sql =
            "SELECT hd.MaHD, hd.NgayBan, ISNULL(kh.TenKH, N'Khách lẻ') AS KhachHang, " +
            "hd.TongTien, ISNULL(hd.TrangThai, N'Thanh cong') AS TrangThai " +
            "FROM HoaDon hd " +
            "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
            "CROSS JOIN ( " +
            "    SELECT a.ClockIn, a.ClockOut, e.MaND " +
            "    FROM HR_Attendances a " +
            "    JOIN HR_Employees e ON a.EmpID = e.EmpID " +
            "    WHERE a.AttendanceID = ? " +
            ") att " +
            "WHERE (att.MaND IS NOT NULL AND hd.MaND = att.MaND OR att.MaND IS NULL) " +
            "  AND hd.NgayBan BETWEEN att.ClockIn AND att.ClockOut " +
            "ORDER BY hd.NgayBan ASC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendanceId);
            ResultSet rs = ps.executeQuery();
            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("MaHD", rs.getInt("MaHD"));
                Timestamp ngayBan = rs.getTimestamp("NgayBan");
                row.put("NgayBan", ngayBan != null ? ngayBan.toLocalDateTime().format(timeFmt) : "");
                row.put("KhachHang", rs.getNString("KhachHang"));
                row.put("TongTien", String.format("%,.0f", rs.getBigDecimal("TongTien")));
                row.put("TrangThai", rs.getNString("TrangThai"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi query hóa đơn trong ca: " + e.getMessage());
        }
        return list;
    }

    // ==========================================
    // AUTO NO-SHOW DETECTION
    // ==========================================

    /**
     * Tự động ghi nhận "Nghỉ không phép" cho các ca đã quá mà NV không chấm công.
     * Chạy 1 lần khi app khởi động.
     * Điều kiện: WorkDate < Hôm nay VÀ không có row trong HR_Attendances VÀ không phải ca Nghỉ Phép/Không Lương.
     * @return Số record được tạo.
     */
    public int autoMarkNoShow() {
        String sql =
            "INSERT INTO HR_Attendances (EmpID, ScheduleID, ClockIn, ClockOut, TotalHours, " +
            "DailyEarned, LateReason, SnapshotRate, SnapshotStart, SnapshotEnd) " +
            "SELECT s.EmpID, s.ScheduleID, " +
            "CAST(s.WorkDate AS DATETIME) + CAST(s.ActualStart AS DATETIME), " +
            "CAST(s.WorkDate AS DATETIME) + CAST(s.ActualStart AS DATETIME), " +
            "0, " +       // TotalHours = 0
            "0, " +       // DailyEarned = 0
            "N'[Hệ thống] Nghỉ không phép - NV không chấm công', " +
            "e.HourlyRate, s.ActualStart, s.ActualEnd " +
            "FROM HR_Schedules s " +
            "JOIN HR_Employees e ON s.EmpID = e.EmpID " +
            "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
            "WHERE s.WorkDate < CAST(GETDATE() AS DATE) " +
            "AND sh.ShiftName NOT IN (N'Nghỉ Phép', N'Nghỉ Không Lương') " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM HR_Attendances a WHERE a.ScheduleID = s.ScheduleID" +
            ")";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("[AutoNoShow] Đã ghi " + rows + " lần nghỉ không phép.");
            }
            return rows;
        } catch (SQLException e) {
            System.err.println("[AutoNoShow] Lỗi: " + e.getMessage());
            return 0;
        }
    }
}
