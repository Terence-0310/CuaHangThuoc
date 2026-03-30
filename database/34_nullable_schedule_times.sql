USE QuanLyCuaHangThuoc;
GO

-- Allow NULL for ActualStart/ActualEnd to support leave shifts (no working hours)
ALTER TABLE HR_Schedules ALTER COLUMN ActualStart TIME NULL;
ALTER TABLE HR_Schedules ALTER COLUMN ActualEnd TIME NULL;
GO

PRINT N'OK: HR_Schedules.ActualStart/ActualEnd now allow NULL (for leave shifts)';
GO
