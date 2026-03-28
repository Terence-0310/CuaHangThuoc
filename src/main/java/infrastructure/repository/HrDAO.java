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
        String sql = "INSERT INTO HR_Employees (FullName, PinCode, Phone, HourlyRate, OvertimeRate, " +
                     "HireDate, ResignDate, Status) " +
                     "OUTPUT INSERTED.EmpID " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
        String sql = "UPDATE HR_Employees SET FullName = ?, PinCode = ?, Phone = ?, HourlyRate = ?, " +
                     "OvertimeRate = ?, HireDate = ?, ResignDate = ?, Status = ? " +
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
            ps.setInt(9, emp.getEmpID());
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
    //  MODULE: DYNAMIC LEAVE CALCULATOR
    // ==============================================================

    private void fillDynamicLeave(Employee emp) {
        int defaultWeekly = getDefaultWeeklyLeave();
        int defaultAnnual = getDefaultAnnualLeave();
        int yearsWorked = 0;
        if (emp.getHireDate() != null) {
            LocalDate endDate = emp.getResignDate() != null ? emp.getResignDate() : LocalDate.now();
            yearsWorked = java.time.Period.between(emp.getHireDate(), endDate).getYears();
        }
        emp.setYearsWorked(yearsWorked);
        int annualTotal = defaultAnnual + yearsWorked;
        emp.setAnnualLeaveTotal(annualTotal);
        int usedAnnual = getUsedAnnualLeave(emp.getEmpID(), LocalDate.now().getYear());
        emp.setAnnualLeaveRemaining(annualTotal - usedAnnual);
        int usedWeekly = getUsedWeeklyLeave(emp.getEmpID(), LocalDate.now());
        emp.setWeeklyLeaveRemaining(defaultWeekly - usedWeekly);
    }

    public int getUsedWeeklyLeave(int empId, LocalDate anyDay) {
        LocalDate monday = anyDay.with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        String sql = "SELECT COUNT(*) FROM HR_Schedules s JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
                     "WHERE s.EmpID = ? AND sh.ShiftName = N'Nghỉ Phép Tuần' AND s.WorkDate BETWEEN ? AND ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId); ps.setDate(2, Date.valueOf(monday)); ps.setDate(3, Date.valueOf(sunday));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return 0;
    }

    public int getUsedAnnualLeave(int empId, int year) {
        String sql = "SELECT COUNT(*) FROM HR_Schedules s JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID " +
                     "WHERE s.EmpID = ? AND sh.ShiftName = N'Nghỉ Phép Năm' AND YEAR(s.WorkDate) = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId); ps.setInt(2, year);
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
         String sql = "IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = ? AND WorkDate = ? AND ShiftID = ?) " +
                     "INSERT INTO HR_Schedules (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd) " +
                     "VALUES (?, ?, ?, ?, ?)";
        int inserted = 0;
        LocalDate today = LocalDate.now();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            LocalDate current = fromDate;
            while (!current.isAfter(toDate)) {
                // ★ Bỏ qua ngày đã qua; nếu hôm nay thì bỏ qua ca đã kết thúc
                if (current.isBefore(today) ||
                    (current.isEqual(today) && LocalTime.now().isAfter(actualEnd))) {
                    current = current.plusDays(1);
                    continue;
                }

                // IF NOT EXISTS params (3)
                ps.setInt(1, empID);
                ps.setDate(2, Date.valueOf(current));
                ps.setInt(3, shiftID);
                // INSERT params (5)
                ps.setInt(4, empID);
                ps.setInt(5, shiftID);
                ps.setDate(6, Date.valueOf(current));
                ps.setTime(7, Time.valueOf(actualStart));
                ps.setTime(8, Time.valueOf(actualEnd));
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
                sc.setActualStart(rs.getTime("ActualStart").toLocalTime());
                sc.setActualEnd(rs.getTime("ActualEnd").toLocalTime());
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
        String sql = "UPDATE HR_Schedules SET EmpID = ?, ShiftID = ?, WorkDate = ?, " +
                     "ActualStart = ?, ActualEnd = ? WHERE ScheduleID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ps.setInt(2, shiftID);
            ps.setDate(3, Date.valueOf(workDate));
            ps.setTime(4, Time.valueOf(actualStart));
            ps.setTime(5, Time.valueOf(actualEnd));
            ps.setInt(6, scheduleID);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UQ_Schedule_EmpDate")) {
                throw new RuntimeException("TRÙNG LỊCH: Nhân viên này đã có lịch ngày " + workDate + "!");
            }
            throw new RuntimeException(e);
        }
    }
}
