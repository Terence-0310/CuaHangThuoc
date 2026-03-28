package domain.entity;

import java.time.LocalDate;

public class Leave {
    private int leaveID;
    private int empID;
    private String empName;
    private LocalDate leaveDate;
    private String reason;
    private boolean isExcused;

    public int getLeaveID() { return leaveID; }
    public void setLeaveID(int leaveID) { this.leaveID = leaveID; }
    public int getEmpID() { return empID; }
    public void setEmpID(int empID) { this.empID = empID; }
    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }
    public LocalDate getLeaveDate() { return leaveDate; }
    public void setLeaveDate(LocalDate leaveDate) { this.leaveDate = leaveDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isExcused() { return isExcused; }
    public void setExcused(boolean excused) { isExcused = excused; }
}
