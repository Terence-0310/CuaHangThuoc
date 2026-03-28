package domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceHistory {
    private int recordID;
    private int empID;
    private String empName; // from JOIN
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private BigDecimal totalHours;
    private boolean isLate;
    private String lateReason;
    private boolean isExcused;
    private BigDecimal regularHours;
    private BigDecimal overtimeHours;
    private BigDecimal dailyEarned;
    // Bổ sung các cột dành cho Lịch sử
    private String shiftName;
    private LocalDate workDate;
    private BigDecimal hourlyRate;

    public AttendanceHistory() {}

    public int getRecordID() { return recordID; }
    public void setRecordID(int recordID) { this.recordID = recordID; }
    public int getEmpID() { return empID; }
    public void setEmpID(int empID) { this.empID = empID; }
    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }
    public LocalDateTime getClockIn() { return clockIn; }
    public void setClockIn(LocalDateTime clockIn) { this.clockIn = clockIn; }
    public LocalDateTime getClockOut() { return clockOut; }
    public void setClockOut(LocalDateTime clockOut) { this.clockOut = clockOut; }
    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
    public boolean isLate() { return isLate; }
    public void setLate(boolean late) { isLate = late; }
    public String getLateReason() { return lateReason; }
    public void setLateReason(String lateReason) { this.lateReason = lateReason; }
    public boolean isExcused() { return isExcused; }
    public void setExcused(boolean excused) { this.isExcused = excused; }
    public BigDecimal getRegularHours() { return regularHours; }
    public void setRegularHours(BigDecimal regularHours) { this.regularHours = regularHours; }
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
    public BigDecimal getDailyEarned() { return dailyEarned; }
    public void setDailyEarned(BigDecimal dailyEarned) { this.dailyEarned = dailyEarned; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    // Revenue tracking
    private int invoiceCount;
    private BigDecimal totalRevenue;
    private int maNd; // NguoiDung.MaND for invoice lookup

    public int getInvoiceCount() { return invoiceCount; }
    public void setInvoiceCount(int invoiceCount) { this.invoiceCount = invoiceCount; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public int getMaNd() { return maNd; }
    public void setMaNd(int maNd) { this.maNd = maNd; }
}
