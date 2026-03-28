package domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MonthlyPayroll {
    private int payrollID;
    private int month;
    private int year;
    private int empID;
    private String empName; // from JOIN
    private BigDecimal totalSalary;
    private boolean isPaid;
    private LocalDateTime paidDate;

    public MonthlyPayroll() {}

    public int getPayrollID() { return payrollID; }
    public void setPayrollID(int payrollID) { this.payrollID = payrollID; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getEmpID() { return empID; }
    public void setEmpID(int empID) { this.empID = empID; }
    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }
    public BigDecimal getTotalSalary() { return totalSalary; }
    public void setTotalSalary(BigDecimal totalSalary) { this.totalSalary = totalSalary; }
    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean isPaid) { this.isPaid = isPaid; }
    public LocalDateTime getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDateTime paidDate) { this.paidDate = paidDate; }
}
