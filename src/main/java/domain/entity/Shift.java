package domain.entity;

import java.time.LocalTime;

public class Shift {
    private int shiftID;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;

    public int getShiftID() { return shiftID; }
    public void setShiftID(int shiftID) { this.shiftID = shiftID; }
    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
