USE QuanLyCuaHangThuoc;
GO

-- Allow ClockIn = NULL for absent records (VẮNG MẶT)
ALTER TABLE HR_Attendances ALTER COLUMN ClockIn DATETIME NULL;
GO

PRINT N'✓ HR_Attendances.ClockIn now allows NULL (for absent marking)';
GO
