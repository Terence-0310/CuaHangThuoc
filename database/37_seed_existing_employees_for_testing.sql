USE QuanLyCuaHangThuoc;
GO

SET NOCOUNT ON;

PRINT N'=== START: SEED TRUC TIEP CHO NHAN VIEN HIEN CO ===';

IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Ca Sáng')
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES (N'Ca Sáng', '06:00', '14:00');

IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Ca Chiều')
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES (N'Ca Chiều', '14:00', '22:00');

IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Ca Đêm')
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES (N'Ca Đêm', '22:00', '06:00');

DECLARE @ShiftSang INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Sáng' ORDER BY ShiftID);
DECLARE @ShiftChieu INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Chiều' ORDER BY ShiftID);
DECLARE @ShiftDem INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Đêm' ORDER BY ShiftID);
DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @Tomorrow DATE = DATEADD(DAY, 1, @Today);

-- Chọn tối đa 10 nhân viên đang làm để seed lịch test
DECLARE @Emp TABLE (
    rn INT IDENTITY(1,1) PRIMARY KEY,
    EmpID INT
);

INSERT INTO @Emp (EmpID)
SELECT TOP 10 EmpID
FROM HR_Employees
WHERE Status = N'Đang làm'
ORDER BY EmpID;

-- Nếu không có ai trạng thái Đang làm thì lấy 10 người đầu và set về Đang làm để test
IF NOT EXISTS (SELECT 1 FROM @Emp)
BEGIN
    INSERT INTO @Emp (EmpID)
    SELECT TOP 10 EmpID FROM HR_Employees ORDER BY EmpID;

    UPDATE e
    SET e.Status = N'Đang làm'
    FROM HR_Employees e
    JOIN @Emp t ON t.EmpID = e.EmpID;
END;

DECLARE @i INT = 1;
DECLARE @n INT = (SELECT COUNT(*) FROM @Emp);
DECLARE @EmpID INT;
DECLARE @ShiftToday INT;
DECLARE @ShiftTomorrow INT;

WHILE @i <= @n
BEGIN
    SELECT @EmpID = EmpID FROM @Emp WHERE rn = @i;

    -- Xoay vòng 3 ca để test full 24/24
    IF (@i % 3 = 1)
    BEGIN
        SET @ShiftToday = @ShiftSang;
        SET @ShiftTomorrow = @ShiftChieu;
    END
    ELSE IF (@i % 3 = 2)
    BEGIN
        SET @ShiftToday = @ShiftChieu;
        SET @ShiftTomorrow = @ShiftDem;
    END
    ELSE
    BEGIN
        SET @ShiftToday = @ShiftDem;
        SET @ShiftTomorrow = @ShiftSang;
    END

    -- Hôm nay
    IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = @EmpID AND WorkDate = @Today)
    BEGIN
        INSERT INTO HR_Schedules (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, ShiftDefaultStart, ShiftDefaultEnd, CreatedAt)
        SELECT @EmpID, s.ShiftID, @Today, s.DefaultStartTime, s.DefaultEndTime, s.DefaultStartTime, s.DefaultEndTime, GETDATE()
        FROM HR_Shifts s
        WHERE s.ShiftID = @ShiftToday;
    END

    -- Ngày mai
    IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = @EmpID AND WorkDate = @Tomorrow)
    BEGIN
        INSERT INTO HR_Schedules (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, ShiftDefaultStart, ShiftDefaultEnd, CreatedAt)
        SELECT @EmpID, s.ShiftID, @Tomorrow, s.DefaultStartTime, s.DefaultEndTime, s.DefaultStartTime, s.DefaultEndTime, GETDATE()
        FROM HR_Shifts s
        WHERE s.ShiftID = @ShiftTomorrow;
    END

    SET @i += 1;
END

SELECT e.EmpID, e.FullName, e.PinCode, e.Status
FROM HR_Employees e
JOIN @Emp t ON t.EmpID = e.EmpID
ORDER BY e.EmpID;

SELECT s.ScheduleID, s.EmpID, e.FullName, s.WorkDate, sh.ShiftName, s.ActualStart, s.ActualEnd
FROM HR_Schedules s
JOIN HR_Employees e ON e.EmpID = s.EmpID
JOIN HR_Shifts sh ON sh.ShiftID = s.ShiftID
WHERE s.WorkDate IN (@Today, @Tomorrow)
  AND EXISTS (SELECT 1 FROM @Emp t WHERE t.EmpID = s.EmpID)
ORDER BY s.WorkDate, s.EmpID;

PRINT N'=== DONE: DA THEM LICH TEST CHO NHAN VIEN HIEN CO ===';
GO
