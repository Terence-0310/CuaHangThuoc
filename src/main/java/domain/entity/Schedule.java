package domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Entity: Lịch xếp ca — Maps to HR_Schedules.
 * ActualStart/ActualEnd = Giờ thực tế (đã override hoặc giữ mặc định từ Shift).
 */
public class Schedule {
    private int scheduleID;
    private int empID;
    private String empName;         // JOIN từ HR_Employees.FullName
    private int shiftID;
    private String shiftName;       // JOIN từ HR_Shifts.ShiftName
    private LocalDate workDate;
    private LocalTime actualStart;  // Giờ vào thực tế
    private LocalTime actualEnd;    // Giờ ra thực tế
    private LocalDateTime createdAt;

    public Schedule() {}

    // === Getters & Setters ===
    public int getScheduleID() { return scheduleID; }
    public void setScheduleID(int id) { this.scheduleID = id; }

    public int getEmpID() { return empID; }
    public void setEmpID(int empID) { this.empID = empID; }

    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }

    public int getShiftID() { return shiftID; }
    public void setShiftID(int shiftID) { this.shiftID = shiftID; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public LocalTime getActualStart() { return actualStart; }
    public void setActualStart(LocalTime actualStart) { this.actualStart = actualStart; }

    public LocalTime getActualEnd() { return actualEnd; }
    public void setActualEnd(LocalTime actualEnd) { this.actualEnd = actualEnd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
