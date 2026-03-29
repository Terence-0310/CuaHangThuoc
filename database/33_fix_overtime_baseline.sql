USE QuanLyCuaHangThuoc;
GO

-- ★ Thêm cột ShiftDefaultStart để lưu giờ BẮT ĐẦU gốc của ca (từ HR_Shifts)
-- Dùng cặp (ShiftDefaultStart, ShiftDefaultEnd) để tính baseline OT chính xác
-- Trước đây chỉ có ShiftDefaultEnd → dùng SnapshotStart (admin xếp) làm baseline → SAI
IF COL_LENGTH('HR_Attendances', 'ShiftDefaultStart') IS NULL
    ALTER TABLE HR_Attendances ADD ShiftDefaultStart TIME NULL;
GO

-- ★ Backfill: Cập nhật các record cũ chưa có ShiftDefaultStart
UPDATE a SET a.ShiftDefaultStart = sh.DefaultStartTime
FROM HR_Attendances a
JOIN HR_Schedules s ON a.ScheduleID = s.ScheduleID
JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID
WHERE a.ShiftDefaultStart IS NULL;
GO

PRINT N'OK: ShiftDefaultStart column added and backfilled';
GO
