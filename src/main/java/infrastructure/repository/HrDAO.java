package infrastructure.repository;

import domain.entity.Employee;
import domain.entity.Schedule;
import domain.entity.Shift;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/**
 * HRM DAO — Phase 1: Employees + Shifts CRUD.
 * Nguyên tắc:
 * - PinCode: VARCHAR → dùng setString (giữ số 0 đầu)
 * - Soft Delete: UPDATE Status = N'Đã nghỉ' (KHÔNG DELETE)
 * - Duplicate PIN: catch SQLIntegrityConstraintViolationException
 */
public class HrDAO {

    // ==============================================================
    //  MODULE 1: EMPLOYEE CRUD
    // ==============================================================

    /** Mapper: ResultSet → Employee */
    private Employee mapEmployee(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmpID(rs.getInt("EmpID"));
        e.setFullName(rs.getNString("FullName"));
        e.setPinCode(rs.getString("PinCode"));
        e.setPhone(rs.getString("Phone"));
        e.setHourlyRate(rs.getBigDecimal("HourlyRate"));
        e.setOvertimeRate(rs.getBigDecimal("OvertimeRate"));
        Date hd = rs.getDate("HireDate");
        if (hd != null) e.setHireDate(hd.toLocalDate());
        Date rd = rs.getDate("ResignDate");
        if (rd != null) e.setResignDate(rd.toLocalDate());
        e.setStatus(rs.getNString("Status"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) e.setCreatedAt(ts.toLocalDateTime());
        // ★ FIX: Đọc MaND liên kết tài khoản đăng nhập
        int maND = rs.getInt("MaND");
        e.setMaND(rs.wasNull() ? null : maND);
        return e;
    }

    /** Lấy tất cả nhân viên (bao gồm cả Đã nghỉ) */
    public List<Employee> getAllEmployees() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM HR_Employees ORDER BY EmpID";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Employee emp = mapEmployee(rs);
                fillDynamicLeave(emp);
                list.add(emp);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    /** Lấy nhân viên theo ID */
    public Employee getEmployeeById(int empID) {
        String sql = "SELECT * FROM HR_Employees WHERE EmpID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Employee emp = mapEmployee(rs);
                    fillDynamicLeave(emp);
                    return emp;
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    /**
     * Thêm nhân viên mới.
     * Bắt lỗi trùng PIN → ném RuntimeException với message chứa "TRÙNG MÃ PIN".
     */
    public int insertEmployee(Employee emp) {
        // ★ FIX: Thêm cột MaND để gắn tài khoản đăng nhập ngay lúc tạo NV
        String sql = "INSERT INTO HR_Employees (FullName, PinCode, Phone, HourlyRate, OvertimeRate, " +
                     "HireDate, ResignDate, Status, MaND) " +
                     "OUTPUT INSERTED.EmpID " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, emp.getFullName());
            ps.setString(2, emp.getPinCode());
            ps.setString(3, emp.getPhone());
            ps.setBigDecimal(4, emp.getHourlyRate());
            ps.setBigDecimal(5, emp.getOvertimeRate());
            ps.setDate(6, emp.getHireDate() != null ? Date.valueOf(emp.getHireDate()) : null);
            ps.setDate(7, emp.getResignDate() != null ? Date.valueOf(emp.getResignDate()) : null);
            ps.setNString(8, emp.getStatus());
            if (emp.getMaND() != null) {
                ps.setInt(9, emp.getMaND());
            } else {
                ps.setNull(9, java.sql.Types.INTEGER);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UQ_HR_Employees_PinCode")) {
                throw new RuntimeException("TRÙNG MÃ PIN: Mã PIN '" + emp.getPinCode() + "' đã tồn tại!");
            }
            throw new RuntimeException(e);
        }
        return -1;
    }

    /**
     * Cập nhật nhân viên.
     * Bắt lỗi trùng PIN khi update.
     */
    public void updateEmployee(Employee emp) {
        // ★ FIX: Thêm MaND vào UPDATE để admin có thể thay đổi liên kết tài khoản
        String sql = "UPDATE HR_Employees SET FullName = ?, PinCode = ?, Phone = ?, HourlyRate = ?, " +
                     "OvertimeRate = ?, HireDate = ?, ResignDate = ?, Status = ?, MaND = ? " +
                     "WHERE EmpID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, emp.getFullName());
            ps.setString(2, emp.getPinCode());
            ps.setString(3, emp.getPhone());
            ps.setBigDecimal(4, emp.getHourlyRate());
            ps.setBigDecimal(5, emp.getOvertimeRate());
            ps.setDate(6, emp.getHireDate() != null ? Date.valueOf(emp.getHireDate()) : null);
            ps.setDate(7, emp.getResignDate() != null ? Date.valueOf(emp.getResignDate()) : null);
            ps.setNString(8, emp.getStatus());
            if (emp.getMaND() != null) {
                ps.setInt(9, emp.getMaND());
            } else {
                ps.setNull(9, java.sql.Types.INTEGER);
            }
            ps.setInt(10, emp.getEmpID());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UQ_HR_Employees_PinCode")) {
                throw new RuntimeException("TRÙNG MÃ PIN: Mã PIN '" + emp.getPinCode() + "' đã được sử dụng!");
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * XÓA MỀM: UPDATE Status = N'Đã nghỉ'.
     * TUYỆT ĐỐI KHÔNG DELETE FROM.
     */
    public void softDeleteEmployee(int empID) {
        String sql = "UPDATE HR_Employees SET Status = N'Đã nghỉ', ResignDate = CAST(GETDATE() AS DATE) WHERE EmpID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /**
     * ★ Lấy danh sách tài khoản NguoiDung (NV) chưa liên kết hoặc đang liên kết với empID hiện tại.
     * Dùng cho ComboBox chọn tài khoản khi thêm/sửa NV.
     * @return List of Object[]{MaND, displayText} — e.g. {7, "nv01 — Vương Gia Long"}
     */
    public java.util.List<Object[]> getUserAccountsForLinking(int currentEmpID) {
        // Lấy tất cả tài khoản NV (VaiTro != 'Admin') chưa được gắn cho NV nào khác, hoặc đang gắn cho chính empID này
        String sql = "SELECT nd.MaND, nd.TenDangNhap, nd.HoTen FROM NguoiDung nd " +
                     "WHERE nd.TrangThai = 1 " +
                     "AND NOT EXISTS ( " +
                     "  SELECT 1 FROM HR_Employees e WHERE e.MaND = nd.MaND AND e.EmpID != ? " +
                     ") " +
                     "ORDER BY nd.TenDangNhap";
        java.util.List<Object[]> list = new java.util.ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentEmpID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int maND = rs.getInt("MaND");
                    String login = rs.getString("TenDangNhap");
                    String hoTen = rs.getNString("HoTen");
                    list.add(new Object[]{maND, login + " — " + hoTen});
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    /** Kiểm tra PIN đã tồn tại chưa (dùng cho validate trước khi Insert) */
    public boolean checkDuplicatePin(String pinCode, int excludeEmpID) {
        String sql = "SELECT COUNT(*) FROM HR_Employees WHERE PinCode = ? AND EmpID != ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pinCode);
            ps.setInt(2, excludeEmpID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return false;
    }

    // ==============================================================
    //  MODULE 2: SHIFT CRUD
    // ==============================================================

    /** Mapper: ResultSet → Shift */
    private Shift mapShift(ResultSet rs) throws SQLException {
        Shift s = new Shift();
        s.setShiftID(rs.getInt("ShiftID"));
        s.setShiftName(rs.getNString("ShiftName"));
        s.setStartTime(rs.getTime("DefaultStartTime").toLocalTime());
        s.setEndTime(rs.getTime("DefaultEndTime").toLocalTime());
        return s;
    }

    /** Lấy tất cả ca làm */
    public List<Shift> getAllShifts() {
        List<Shift> list = new ArrayList<>();
        String sql = "SELECT * FROM HR_Shifts ORDER BY ShiftID";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapShift(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    /** Cập nhật giờ bắt đầu/kết thúc ca */
    public void updateShift(int shiftID, LocalTime start, LocalTime end) {
        String sql = "UPDATE HR_Shifts SET DefaultStartTime = ?, DefaultEndTime = ? WHERE ShiftID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTime(1, Time.valueOf(start));
            ps.setTime(2, Time.valueOf(end));
            ps.setInt(3, shiftID);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** Thêm ca mới */
    public void insertShift(String shiftName, LocalTime start, LocalTime end) {
        String sql = "INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, shiftName);
            ps.setTime(2, Time.valueOf(start));
            ps.setTime(3, Time.valueOf(end));
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** Xóa ca (chỉ khi chưa có schedule nào dùng) */
    public void deleteShift(int shiftID) {
        String sql = "DELETE FROM HR_Shifts WHERE ShiftID = ? AND NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE ShiftID = ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shiftID);
            ps.setInt(2, shiftID);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Không thể xóa! Ca này đang được sử dụng trong lịch.");
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ==============================================================
    //  MODULE 3: SCHEDULE CRUD
    // ==============================================================

    // ==============================================================
    //  MODULE: HR CONFIG (Cấu hình phép mặc định)
    // ==============================================================

    public int getDefaultWeeklyLeave() {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT DefaultWeeklyLeave FROM HR_Config WHERE ConfigID = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return 1;
    }

    public int getDefaultAnnualLeave() {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT DefaultAnnualLeave FROM HR_Config WHERE ConfigID = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return 12;
    }

    public void updateConfig(int weeklyLeave, int annualLeave) {
        String sql = "UPDATE HR_Config SET DefaultWeeklyLeave = ?, DefaultAnnualLeave = ? WHERE ConfigID = 1";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, weeklyLeave);
            ps.setInt(2, annualLeave);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ==============================================================
    //  MODULE: DYNAMIC LEAVE CALCULATOR (ERP Standard)
    //  Tất cả phép được tính realtime từ DB, không dùng biến đếm tĩnh
    // ==============================================================

    /**
     * Tính toán phép động (Dynamic Leave Calculation).
     * Trả về Map chứa tất cả thông số phép hiện tại của NV.
     *
     * Keys:
     *   - "DefaultWeeklyLeave"     : int - Hạn mức phép tuần từ HR_Config
     *   - "DefaultAnnualLeave"     : int - Hạn mức phép năm gốc từ HR_Config
     *   - "YearsWorked"            : int - Số năm thâm niên
     *   - "AnnualLeaveTotal"       : int - Tổng phép năm = gốc + thâm niên
     *   - "UsedWeeklyLeave"        : int - Số phép tuần ĐÃ DÙNG (tuần hiện tại)
     *   - "UsedAnnualLeave"        : int - Số phép năm ĐÃ DÙNG (năm hiện tại)
     *   - "RemainingWeeklyLeave"   : int - Phép tuần CÒN LẠI
     *   - "RemainingAnnualLeave"   : int - Phép năm CÒN LẠI
     */
    public java.util.Map<String, Integer> getLeaveBalance(int empId) {
        java.util.Map<String, Integer> balance = new java.util.LinkedHashMap<>();

        // 1. LẤY CONFIG GỐC
        int defaultWeekly = getDefaultWeeklyLeave();
        int defaultAnnual = getDefaultAnnualLeave();
        balance.put("DefaultWeeklyLeave", defaultWeekly);
        balance.put("DefaultAnnualLeave", defaultAnnual);

        // 2. TÍNH THÂM NIÊN (YearsWorked)
        int yearsWorked = 0;
        String sqlSeniority =
            "SELECT DATEDIFF(YEAR, HireDate, GETDATE()) AS YearsWorked " +
            "FROM HR_Employees WHERE EmpID = ? AND HireDate IS NOT NULL";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSeniority)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                yearsWorked = Math.max(0, rs.getInt("YearsWorked"));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        balance.put("YearsWorked", yearsWorked);

        // 3. TỔNG PHÉP NĂM = gốc + thâm niên
        int annualTotal = defaultAnnual + yearsWorked;
        balance.put("AnnualLeaveTotal", annualTotal);

        // 4. ĐẾM PHÉP TUẦN ĐÃ DÙNG (tuần hiện tại: Mon → Sun)
        int usedWeekly = getUsedWeeklyLeave(empId, LocalDate.now());
        balance.put("UsedWeeklyLeave", usedWeekly);

        // 5. ĐẾM PHÉP NĂM ĐÃ DÙNG (năm hiện tại)
        int usedAnnual = getUsedAnnualLeave(empId, LocalDate.now().getYear());
        balance.put("UsedAnnualLeave", usedAnnual);

        // 6. TÍNH CÒN LẠI
        balance.put("RemainingWeeklyLeave", defaultWeekly - usedWeekly);
        balance.put("RemainingAnnualLeave", annualTotal - usedAnnual);

        return balance;
    }

    /**
     * Tính phép tuần còn lại cho 1 tuần CỤ THỂ (dùng khi xếp ca nhiều tuần).
     * @param anyDay bất kỳ ngày nào trong tuần cần kiểm tra
     */
    public java.util.Map<String, Integer> getLeaveBalanceForWeek(int empId, LocalDate anyDay) {
        java.util.Map<String, Integer> balance = getLeaveBalance(empId);
        // Override weekly với tuần cụ thể (thay vì tuần hiện tại)
        int usedWeekly = getUsedWeeklyLeave(empId, anyDay);
        balance.put("UsedWeeklyLeave", usedWeekly);
        balance.put("RemainingWeeklyLeave", balance.get("DefaultWeeklyLeave") - usedWeekly);
        return balance;
    }

    private void fillDynamicLeave(Employee emp) {
        java.util.Map<String, Integer> balance = getLeaveBalance(emp.getEmpID());
        int yearsWorked = balance.get("YearsWorked");
        if (emp.getHireDate() != null && emp.getResignDate() != null) {
            yearsWorked = java.time.Period.between(emp.getHireDate(), emp.getResignDate()).getYears();
        }
        emp.setYearsWorked(yearsWorked);
        emp.setAnnualLeaveTotal(balance.get("AnnualLeaveTotal"));
        emp.setAnnualLeaveRemaining(balance.get("RemainingAnnualLeave"));
        emp.setWeeklyLeaveRemaining(balance.get("RemainingWeeklyLeave"));
    }

    public int getUsedWeeklyLeave(int empId, LocalDate anyDay) {
        LocalDate monday = anyDay.with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        String sql = "SELECT COUNT(*) FROM HR_Schedules s " +
                     "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
                     "WHERE s.EmpID = ? AND sh.ShiftName = N'Nghỉ Phép Tuần' " +
                     "AND s.WorkDate BETWEEN ? AND ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setDate(2, Date.valueOf(monday));
            ps.setDate(3, Date.valueOf(sunday));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return 0;
    }

    public int getUsedAnnualLeave(int empId, int year) {
        String sql = "SELECT COUNT(*) FROM HR_Schedules s " +
                     "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
                     "WHERE s.EmpID = ? AND sh.ShiftName = N'Nghỉ Phép Năm' " +
                     "AND YEAR(s.WorkDate) = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setInt(2, year);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return 0;
    }

    public int copyScheduleLastWeek(LocalDate targetFrom) {
        LocalDate lastWeekFrom = targetFrom.minusDays(7);
        LocalDate lastWeekTo = targetFrom.minusDays(1);
        LocalDate targetTo = targetFrom.plusDays(6);

        String sqlCheck = "SELECT COUNT(*) FROM HR_Schedules WHERE WorkDate BETWEEN ? AND ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setDate(1, Date.valueOf(targetFrom));
            psCheck.setDate(2, Date.valueOf(targetTo));
            ResultSet rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new RuntimeException("Tuần này đã có lịch, không thể copy đè!");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra lịch tuần mới: " + e.getMessage());
        }

        String sqlInsert = 
            "INSERT INTO HR_Schedules (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, CreatedAt) " +
            "SELECT EmpID, ShiftID, DATEADD(DAY, 7, WorkDate), ActualStart, ActualEnd, GETDATE() " +
            "FROM HR_Schedules " +
            "WHERE WorkDate BETWEEN ? AND ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
            psInsert.setDate(1, Date.valueOf(lastWeekFrom));
            psInsert.setDate(2, Date.valueOf(lastWeekTo));
            int copied = psInsert.executeUpdate();
            if (copied == 0) {
                throw new RuntimeException("Tuần trước không có lịch nào để copy!");
            }
            return copied;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi copy lịch: " + e.getMessage());
        }
    }

    /**
     * Batch Insert: Xếp ca từ fromDate đến toDate.
     * Dùng vòng lặp LocalDate + addBatch() + executeBatch().
     * Chống trùng: IF NOT EXISTS trước mỗi INSERT.
     * @return số ngày được thêm thành công
     */
    public int insertBulkSchedules(int empID, int shiftID, LocalDate fromDate, LocalDate toDate,
                                    LocalTime actualStart, LocalTime actualEnd) {
         // ★ Chặn trùng: 1 NV chỉ có 1 lịch/ngày (không cho vừa làm vừa nghỉ cùng ngày)
         // ★ Snapshot shift config: Lưu giờ cấu hình gốc vào schedule để admin sửa config sau không ảnh hưởng
         String sql = "IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = ? AND WorkDate = ?) " +
                     "INSERT INTO HR_Schedules (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, ShiftDefaultStart, ShiftDefaultEnd) " +
                     "SELECT ?, ?, ?, ?, ?, sh.DefaultStartTime, sh.DefaultEndTime " +
                     "FROM HR_Shifts sh WHERE sh.ShiftID = ?";
        int inserted = 0;
        LocalDate today = LocalDate.now();
        boolean isLeave = (actualStart == null || actualEnd == null);
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            LocalDate current = fromDate;
            while (!current.isAfter(toDate)) {
                // ★ Bỏ qua ngày đã qua; nếu hôm nay thì bỏ qua ca đã kết thúc (chỉ cho ca làm việc)
                if (current.isBefore(today) ||
                    (!isLeave && current.isEqual(today) && LocalTime.now().isAfter(actualEnd))) {
                    current = current.plusDays(1);
                    continue;
                }

                // IF NOT EXISTS params (2)
                ps.setInt(1, empID);
                ps.setDate(2, Date.valueOf(current));
                // SELECT ?, ?, ?, ?, ? FROM HR_Shifts WHERE ShiftID = ?
                ps.setInt(3, empID);
                ps.setInt(4, shiftID);
                ps.setDate(5, Date.valueOf(current));
                if (isLeave) {
                    ps.setNull(6, java.sql.Types.TIME);
                    ps.setNull(7, java.sql.Types.TIME);
                } else {
                    ps.setTime(6, Time.valueOf(actualStart));
                    ps.setTime(7, Time.valueOf(actualEnd));
                }
                ps.setInt(8, shiftID); // ShiftID for sub-select
                ps.addBatch();

                current = current.plusDays(1);
            }

            int[] results = ps.executeBatch();
            for (int r : results) {
                if (r > 0) inserted++;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xếp ca: " + e.getMessage());
        }
        return inserted;
    }

    /**
     * Lấy lịch làm việc (JOIN Employee + Shift).
     * Chỉ lấy WorkDate >= hôm nay (Ẩn lịch cũ).
     */
    public List<Schedule> getAllSchedules() {
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT s.ScheduleID, s.EmpID, e.FullName, s.ShiftID, sh.ShiftName, " +
                     "s.WorkDate, s.ActualStart, s.ActualEnd, s.CreatedAt " +
                     "FROM HR_Schedules s " +
                     "JOIN HR_Employees e ON s.EmpID = e.EmpID " +
                     "JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
                     "WHERE s.WorkDate >= CAST(GETDATE() AS DATE) " +
                     "AND NOT EXISTS (" +
                     "    SELECT 1 FROM HR_Attendances a " +
                     "    WHERE a.ScheduleID = s.ScheduleID AND a.ClockOut IS NOT NULL" +
                     ") " +
                     "ORDER BY s.WorkDate ASC, e.FullName";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Schedule sc = new Schedule();
                sc.setScheduleID(rs.getInt("ScheduleID"));
                sc.setEmpID(rs.getInt("EmpID"));
                sc.setEmpName(rs.getNString("FullName"));
                sc.setShiftID(rs.getInt("ShiftID"));
                sc.setShiftName(rs.getNString("ShiftName"));
                sc.setWorkDate(rs.getDate("WorkDate").toLocalDate());
                java.sql.Time tStart = rs.getTime("ActualStart");
                java.sql.Time tEnd = rs.getTime("ActualEnd");
                sc.setActualStart(tStart != null ? tStart.toLocalTime() : null);
                sc.setActualEnd(tEnd != null ? tEnd.toLocalTime() : null);
                Timestamp ts = rs.getTimestamp("CreatedAt");
                if (ts != null) sc.setCreatedAt(ts.toLocalDateTime());
                list.add(sc);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    /**
     * Xóa hàng loạt lịch bằng DELETE WHERE ScheduleID IN(?,?,...).
     * KHÔNG dùng vòng lặp xóa từng dòng để tránh Index Shifting Bug.
     */
    public void deleteMultipleSchedules(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;

        // Build IN clause: "?,?,?"
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append("?");
        }

        String sql = "DELETE FROM HR_Schedules WHERE ScheduleID IN (" + inClause + ")";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(i + 1, ids.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException e) { 
            if (e.getMessage() != null && e.getMessage().contains("FK_Att_Schedule")) {
                throw new RuntimeException("Không thể xoá lịch này vì nhân viên đã điểm danh (hoặc làm thay) cho ca làm việc này!\nDữ liệu chấm công đã dính liền với lịch.");
            }
            throw new RuntimeException(e); 
        }
    }

    /**
     * Cập nhật 1 dòng lịch (Dual-Mode Edit).
     * Cho phép đổi Ca, Ngày, Giờ vào/ra.
     */
    public void updateSchedule(int scheduleID, int empID, int shiftID, LocalDate workDate,
                                LocalTime actualStart, LocalTime actualEnd) {
        // ★ Snapshot shift config: re-capture DefaultStart/End khi đổi ca
        String sql = "UPDATE s SET s.EmpID = ?, s.ShiftID = ?, s.WorkDate = ?, " +
                     "s.ActualStart = ?, s.ActualEnd = ?, " +
                     "s.ShiftDefaultStart = sh.DefaultStartTime, s.ShiftDefaultEnd = sh.DefaultEndTime " +
                     "FROM HR_Schedules s JOIN HR_Shifts sh ON sh.ShiftID = ? " +
                     "WHERE s.ScheduleID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ps.setInt(2, shiftID);
            ps.setDate(3, Date.valueOf(workDate));
            if (actualStart != null) ps.setTime(4, Time.valueOf(actualStart));
            else ps.setNull(4, java.sql.Types.TIME);
            if (actualEnd != null) ps.setTime(5, Time.valueOf(actualEnd));
            else ps.setNull(5, java.sql.Types.TIME);
            ps.setInt(6, shiftID);  // JOIN HR_Shifts
            ps.setInt(7, scheduleID);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UQ_Schedule_EmpDate")) {
                throw new RuntimeException("TRÙNG LỊCH: Nhân viên này đã có lịch ngày " + workDate + "!");
            }
            throw new RuntimeException(e);
        }
    }
}
