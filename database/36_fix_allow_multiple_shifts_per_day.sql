USE QuanLyCuaHangThuoc;
GO

IF EXISTS (
    SELECT 1 
    FROM sys.key_constraints 
    WHERE type = 'UQ' AND parent_object_id = OBJECT_ID('HR_Schedules') AND name = 'UQ_Schedule_EmpDate'
)
BEGIN
    ALTER TABLE HR_Schedules DROP CONSTRAINT UQ_Schedule_EmpDate;
END

IF NOT EXISTS (
    SELECT 1 
    FROM sys.key_constraints 
    WHERE type = 'UQ' AND parent_object_id = OBJECT_ID('HR_Schedules') AND name = 'UQ_Schedule_EmpDateShift'
)
BEGIN
    ALTER TABLE HR_Schedules ADD CONSTRAINT UQ_Schedule_EmpDateShift UNIQUE (EmpID, WorkDate, ShiftID);
END
GO
PRINT N'✅ Đã thay đổi ràng buộc (Constraint) trên bảng HR_Schedules! Giờ nhân viên có thể làm 2 ca khác nhau trong cùng 1 ngày (VD: Ca sáng, sau đó nghỉ, rồi làm Ca chiều).';
GO
