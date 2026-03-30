USE QuanLyCuaHangThuoc;
GO

-- Add leave shift types (if not already present)
IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Nghỉ Phép Tuần')
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES (N'Nghỉ Phép Tuần', '00:00:00', '00:00:00');

IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Nghỉ Phép Năm')
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES (N'Nghỉ Phép Năm', '00:00:00', '00:00:00');

IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Nghỉ Không Lương')
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES (N'Nghỉ Không Lương', '00:00:00', '00:00:00');
GO

PRINT N'OK: Leave shift types added';
GO
