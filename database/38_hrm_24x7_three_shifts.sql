USE QuanLyCuaHangThuoc;
GO

SET NOCOUNT ON;

PRINT N'=== 24/24 SHIFT POLICY: 3 CA CO DINH + PHU CAP CA DEM ===';

-- Chuan hoa 3 ca 24/24:
-- 1) 06:00 - 14:00
-- 2) 14:00 - 22:00
-- 3) 22:00 - 06:00 (hom sau) - phu cap +30% duoc xu ly tai AttendanceDAO.clockIn()

IF EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Ca Sáng')
BEGIN
    UPDATE HR_Shifts
    SET DefaultStartTime = '06:00', DefaultEndTime = '14:00'
    WHERE ShiftName = N'Ca Sáng';
END
ELSE
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime)
    VALUES (N'Ca Sáng', '06:00', '14:00');
END;

IF EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Ca Chiều')
BEGIN
    UPDATE HR_Shifts
    SET DefaultStartTime = '14:00', DefaultEndTime = '22:00'
    WHERE ShiftName = N'Ca Chiều';
END
ELSE
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime)
    VALUES (N'Ca Chiều', '14:00', '22:00');
END;

IF EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Ca Đêm')
BEGIN
    UPDATE HR_Shifts
    SET DefaultStartTime = '22:00', DefaultEndTime = '06:00'
    WHERE ShiftName = N'Ca Đêm';
END
ELSE
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime)
    VALUES (N'Ca Đêm', '22:00', '06:00');
END;

-- Neu ton tai ten test cu thi dong bo thanh ca dem chuan
UPDATE HR_Shifts
SET ShiftName = N'Ca Đêm', DefaultStartTime = '22:00', DefaultEndTime = '06:00'
WHERE ShiftName = N'Ca Tối Test';

-- Dọn trùng Ca Đêm (nếu có nhiều dòng cùng tên)
DECLARE @NightCanonical INT = (
    SELECT MIN(ShiftID) FROM HR_Shifts WHERE ShiftName = N'Ca Đêm'
);

IF @NightCanonical IS NOT NULL
BEGIN
    UPDATE s
    SET s.ShiftID = @NightCanonical,
        s.ShiftDefaultStart = '22:00',
        s.ShiftDefaultEnd = '06:00'
    FROM HR_Schedules s
    JOIN HR_Shifts sh ON sh.ShiftID = s.ShiftID
    WHERE sh.ShiftName = N'Ca Đêm'
      AND s.ShiftID <> @NightCanonical;

    DELETE FROM HR_Shifts
    WHERE ShiftName = N'Ca Đêm'
      AND ShiftID <> @NightCanonical;
END;

PRINT N'=== DONE: DA CHUAN HOA 3 CA 24/24 ===';
GO
