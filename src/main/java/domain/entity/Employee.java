package domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity Nhân viên — Maps to HR_Employees.
 * PinCode: VARCHAR (giữ số 0 đầu).
 * Status: "Đang làm" | "Đã nghỉ" (Soft Delete).
 */
public class Employee {
    private int empID;
    private String fullName;
    private String pinCode;
    private String phone;
    private BigDecimal hourlyRate;
    private BigDecimal overtimeRate;
    private LocalDate hireDate;
    private LocalDate resignDate;
    private String status;
    private LocalDateTime createdAt;
    private Integer maND; // ★ FK → NguoiDung.MaND (liên kết tài khoản đăng nhập)

    // Dynamic fields (tính từ DAO, không lưu DB)
    private int weeklyLeaveRemaining;
    private int annualLeaveRemaining;
    private int annualLeaveTotal;
    private int yearsWorked;

    public Employee() {}

    public int getEmpID() { return empID; }
    public void setEmpID(int empID) { this.empID = empID; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public BigDecimal getOvertimeRate() { return overtimeRate; }
    public void setOvertimeRate(BigDecimal overtimeRate) { this.overtimeRate = overtimeRate; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public LocalDate getResignDate() { return resignDate; }
    public void setResignDate(LocalDate resignDate) { this.resignDate = resignDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getWeeklyLeaveRemaining() { return weeklyLeaveRemaining; }
    public void setWeeklyLeaveRemaining(int v) { this.weeklyLeaveRemaining = v; }

    public int getAnnualLeaveRemaining() { return annualLeaveRemaining; }
    public void setAnnualLeaveRemaining(int v) { this.annualLeaveRemaining = v; }

    public int getAnnualLeaveTotal() { return annualLeaveTotal; }
    public void setAnnualLeaveTotal(int v) { this.annualLeaveTotal = v; }

    public int getYearsWorked() { return yearsWorked; }
    public void setYearsWorked(int yearsWorked) { this.yearsWorked = yearsWorked; }

    public Integer getMaND() { return maND; }
    public void setMaND(Integer maND) { this.maND = maND; }

    /** Soft Delete check */
    public boolean isActive() { return "Đang làm".equals(status); }
}
