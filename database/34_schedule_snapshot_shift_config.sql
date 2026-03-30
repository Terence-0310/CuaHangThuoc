USE QuanLyCuaHangThuoc;
GO

-- ★ Lưu giờ cấu hình gốc vào HR_Schedules ngay lúc xếp ca
-- Khi admin sửa cấu hình ca sau đó, các lịch đã xếp KHÔNG bị ảnh hưởng
IF COL_LENGTH('HR_Schedules', 'ShiftDefaultStart') IS NULL
    ALTER TABLE HR_Schedules ADD ShiftDefaultStart TIME NULL;
GO

IF COL_LENGTH('HR_Schedules', 'ShiftDefaultEnd') IS NULL
    ALTER TABLE HR_Schedules ADD ShiftDefaultEnd TIME NULL;
GO

-- ★ Backfill: Cập nhật lịch cũ từ HR_Shifts hiện tại
UPDATE s SET
    s.ShiftDefaultStart = sh.DefaultStartTime,
    s.ShiftDefaultEnd = sh.DefaultEndTime
FROM HR_Schedules s
JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID
WHERE s.ShiftDefaultStart IS NULL;
GO

PRINT N'OK: HR_Schedules — ShiftDefaultStart/End columns added and backfilled';
GO
