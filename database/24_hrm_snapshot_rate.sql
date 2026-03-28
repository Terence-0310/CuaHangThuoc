-- =================================================================
-- 24_hrm_snapshot_rate.sql
-- HRM: Snapshot Rate & Schedule tại thời điểm ClockIn
-- Mục đích: Admin sửa lương/lịch giữa chừng không ảnh hưởng ca đang chạy
-- =================================================================
USE QuanLyCuaHangThuoc;
GO

-- 1. Thêm cột SnapshotRate (Lương/giờ tại thời điểm nhận ca)
IF COL_LENGTH('HR_Attendances', 'SnapshotRate') IS NULL
BEGIN
    ALTER TABLE HR_Attendances ADD SnapshotRate DECIMAL(18,0) NULL;
    PRINT N'✅ Thêm cột SnapshotRate vào HR_Attendances';
END
GO

-- 2. Thêm cột SnapshotStart (Giờ bắt đầu ca lúc nhận ca)
IF COL_LENGTH('HR_Attendances', 'SnapshotStart') IS NULL
BEGIN
    ALTER TABLE HR_Attendances ADD SnapshotStart TIME NULL;
    PRINT N'✅ Thêm cột SnapshotStart vào HR_Attendances';
END
GO

-- 3. Thêm cột SnapshotEnd (Giờ kết thúc ca lúc nhận ca)
IF COL_LENGTH('HR_Attendances', 'SnapshotEnd') IS NULL
BEGIN
    ALTER TABLE HR_Attendances ADD SnapshotEnd TIME NULL;
    PRINT N'✅ Thêm cột SnapshotEnd vào HR_Attendances';
END
GO

-- 4. Backfill dữ liệu cũ (gán snapshot từ bảng gốc cho record đã tồn tại)
UPDATE a SET
    a.SnapshotRate  = e.HourlyRate,
    a.SnapshotStart = s.ActualStart,
    a.SnapshotEnd   = s.ActualEnd
FROM HR_Attendances a
JOIN HR_Employees e ON a.EmpID = e.EmpID
JOIN HR_Schedules s ON a.ScheduleID = s.ScheduleID
WHERE a.SnapshotRate IS NULL;
GO

PRINT N'✅ Backfill snapshot cho dữ liệu cũ';
PRINT N'=== Migration 24 (Snapshot Rate) hoàn tất! ===';
GO
