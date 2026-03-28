package service;

import common.Session;
import domain.entity.Employee;
import domain.entity.Schedule;
import domain.entity.Shift;
import infrastructure.repository.HrDAO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * HRM Service — Phase 1.
 * Phân quyền: chỉ Admin mới được thao tác CRUD nhân sự.
 */
public class HrService {
    private final HrDAO hrDAO = new HrDAO();

    // === AUTHORIZATION ===
    public boolean isAdmin() {
        if (Session.getCurrentUser() != null) {
            return "Admin".equalsIgnoreCase(Session.getCurrentUser().getVaiTro());
        }
        return false;
    }

    private void checkAdminPermission() {
        if (!isAdmin()) {
            throw new SecurityException("Access Denied: Chỉ Admin mới có quyền thực hiện tính năng này!");
        }
    }

    // === EMPLOYEE CRUD ===
    public List<Employee> getAllEmployees() {
        checkAdminPermission();
        return hrDAO.getAllEmployees();
    }

    public Employee getEmployeeById(int empID) {
        checkAdminPermission();
        return hrDAO.getEmployeeById(empID);
    }

    public int insertEmployee(Employee emp) {
        checkAdminPermission();
        return hrDAO.insertEmployee(emp);
    }

    public void updateEmployee(Employee emp) {
        checkAdminPermission();
        hrDAO.updateEmployee(emp);
    }

    /** XÓA MỀM — KHÔNG DELETE */
    public void softDeleteEmployee(int empID) {
        checkAdminPermission();
        hrDAO.softDeleteEmployee(empID);
    }

    public boolean checkDuplicatePin(String pinCode, int excludeEmpID) {
        return hrDAO.checkDuplicatePin(pinCode, excludeEmpID);
    }

    // === SHIFT MANAGEMENT ===
    public List<Shift> getAllShifts() {
        return hrDAO.getAllShifts();
    }

    public void updateShift(int shiftID, LocalTime start, LocalTime end) {
        checkAdminPermission();
        hrDAO.updateShift(shiftID, start, end);
    }

    public void insertShift(String name, LocalTime start, LocalTime end) {
        checkAdminPermission();
        hrDAO.insertShift(name, start, end);
    }

    public void deleteShift(int shiftID) {
        checkAdminPermission();
        hrDAO.deleteShift(shiftID);
    }

    // === SCHEDULE MANAGEMENT ===
    public int copyScheduleLastWeek(LocalDate targetFrom) {
        checkAdminPermission();
        return hrDAO.copyScheduleLastWeek(targetFrom);
    }
    public int insertBulkSchedules(int empID, int shiftID, LocalDate from, LocalDate to,
                                    LocalTime actualStart, LocalTime actualEnd) {
        checkAdminPermission();
        return hrDAO.insertBulkSchedules(empID, shiftID, from, to, actualStart, actualEnd);
    }

    public List<Schedule> getAllSchedules() {
        checkAdminPermission();
        return hrDAO.getAllSchedules();
    }

    public void deleteMultipleSchedules(java.util.List<Integer> ids) {
        checkAdminPermission();
        hrDAO.deleteMultipleSchedules(ids);
    }

    public void updateSchedule(int scheduleID, int empID, int shiftID,
                                LocalDate workDate, LocalTime actualStart, LocalTime actualEnd) {
        checkAdminPermission();
        hrDAO.updateSchedule(scheduleID, empID, shiftID, workDate, actualStart, actualEnd);
    }

    // === LEAVE QUOTA (Dynamic Calculation) ===

    /** Tính phép động — trả Map đầy đủ (tuần hiện tại) */
    public java.util.Map<String, Integer> getLeaveBalance(int empId) {
        return hrDAO.getLeaveBalance(empId);
    }

    /** Tính phép động cho 1 tuần cụ thể */
    public java.util.Map<String, Integer> getLeaveBalanceForWeek(int empId, java.time.LocalDate anyDay) {
        return hrDAO.getLeaveBalanceForWeek(empId, anyDay);
    }

    public int getUsedWeeklyLeave(int empId, java.time.LocalDate anyDay) {
        return hrDAO.getUsedWeeklyLeave(empId, anyDay);
    }

    public int getUsedAnnualLeave(int empId, int year) {
        return hrDAO.getUsedAnnualLeave(empId, year);
    }

    // === CONFIG ===
    public int getDefaultWeeklyLeave() { return hrDAO.getDefaultWeeklyLeave(); }
    public int getDefaultAnnualLeave() { return hrDAO.getDefaultAnnualLeave(); }
    public void updateConfig(int weeklyLeave, int annualLeave) {
        checkAdminPermission();
        hrDAO.updateConfig(weeklyLeave, annualLeave);
    }
}
