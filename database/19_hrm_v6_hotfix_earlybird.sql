-- 19_hrm_v6_hotfix_earlybird.sql
-- Thêm cột ScheduledStart để lưu giờ bắt đầu ca lúc punch-in
-- Dùng cho tính lương: nếu ClockIn < ScheduledStart → lấy ScheduledStart làm mốc
USE QuanLyCuaHangThuoc;
GO

IF COL_LENGTH('HR_AttendanceHistory', 'ScheduledStart') IS NULL
BEGIN
    ALTER TABLE HR_AttendanceHistory ADD ScheduledStart TIME NULL;
END
GO

PRINT N'Hotfix: Thêm cột ScheduledStart vào HR_AttendanceHistory!';
GO
