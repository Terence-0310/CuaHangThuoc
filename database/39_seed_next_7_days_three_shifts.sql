USE QuanLyCuaHangThuoc;
GO

SET NOCOUNT ON;

PRINT N'=== START: SEED LICH 7 NGAY TOI (3 CA) ===';

DECLARE @ShiftSang INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Sáng' ORDER BY ShiftID);
DECLARE @ShiftChieu INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Chiều' ORDER BY ShiftID);
DECLARE @ShiftDem INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Đêm' ORDER BY ShiftID);

IF @ShiftSang IS NULL OR @ShiftChieu IS NULL OR @ShiftDem IS NULL
BEGIN
    RAISERROR (N'Chua co day du 3 ca chuan (Ca Sáng/Ca Chiều/Ca Đêm).', 16, 1);
    RETURN;
END;

DECLARE @StartDate DATE = CAST(GETDATE() AS DATE);
DECLARE @EndDate DATE = DATEADD(DAY, 6, @StartDate); -- 7 ngay tinh ca hom nay

-- Nhan vien dang lam
DECLARE @Emp TABLE (
    rn INT IDENTITY(1,1) PRIMARY KEY,
    EmpID INT
);

INSERT INTO @Emp (EmpID)
SELECT EmpID
FROM HR_Employees
WHERE Status = N'Đang làm'
ORDER BY EmpID;

IF NOT EXISTS (SELECT 1 FROM @Emp)
BEGIN
    PRINT N'Khong co nhan vien dang lam de seed.';
    RETURN;
END;

DECLARE @i INT = 1;
DECLARE @n INT = (SELECT COUNT(*) FROM @Emp);
DECLARE @EmpID INT;
DECLARE @d DATE;
DECLARE @offset INT;
DECLARE @ShiftID INT;

DECLARE @Inserted INT = 0;

WHILE @i <= @n
BEGIN
    SELECT @EmpID = EmpID FROM @Emp WHERE rn = @i;
    SET @d = @StartDate;

    WHILE @d <= @EndDate
    BEGIN
        -- Xoay vong 3 ca theo EmpID + ngay de dam bao du 3 ca tren tong the
        SET @offset = (ABS(CHECKSUM(@EmpID, @d)) % 3);
        IF @offset = 0 SET @ShiftID = @ShiftSang;
        IF @offset = 1 SET @ShiftID = @ShiftChieu;
        IF @offset = 2 SET @ShiftID = @ShiftDem;

        IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = @EmpID AND WorkDate = @d)
        BEGIN
            INSERT INTO HR_Schedules (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, ShiftDefaultStart, ShiftDefaultEnd, CreatedAt)
            SELECT @EmpID, s.ShiftID, @d, s.DefaultStartTime, s.DefaultEndTime, s.DefaultStartTime, s.DefaultEndTime, GETDATE()
            FROM HR_Shifts s
            WHERE s.ShiftID = @ShiftID;

            SET @Inserted += 1;
        END

        SET @d = DATEADD(DAY, 1, @d);
    END

    SET @i += 1;
END

PRINT N'So lich da them moi: ' + CAST(@Inserted AS NVARCHAR(20));

SELECT TOP 200
    s.WorkDate,
    sh.ShiftName,
    COUNT(*) AS SoNhanVien
FROM HR_Schedules s
JOIN HR_Shifts sh ON sh.ShiftID = s.ShiftID
WHERE s.WorkDate BETWEEN @StartDate AND @EndDate
GROUP BY s.WorkDate, sh.ShiftName
ORDER BY s.WorkDate, sh.ShiftName;

PRINT N'=== DONE: SEED LICH 7 NGAY TOI (3 CA) ===';
GO
