USE QuanLyCuaHangThuoc;
GO

SET NOCOUNT ON;

PRINT N'=== START: SEED 30 NGAY TOI (3 CA + LICH THUC TE) ===';

DECLARE @ShiftSang INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Sáng' ORDER BY ShiftID);
DECLARE @ShiftChieu INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Chiều' ORDER BY ShiftID);
DECLARE @ShiftDem INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Đêm' ORDER BY ShiftID);

IF @ShiftSang IS NULL OR @ShiftChieu IS NULL OR @ShiftDem IS NULL
BEGIN
    RAISERROR (N'Chua co day du 3 ca chuan (Ca Sáng/Ca Chiều/Ca Đêm).', 16, 1);
    RETURN;
END;

DECLARE @StartDate DATE = CAST(GETDATE() AS DATE);
DECLARE @EndDate DATE = DATEADD(DAY, 29, @StartDate); -- 30 ngay tinh ca hom nay

-- ============================================================
-- 1) ACTIVE EMPLOYEES
-- ============================================================
IF OBJECT_ID('tempdb..#Emp') IS NOT NULL DROP TABLE #Emp;
CREATE TABLE #Emp (
    EmpRn INT NOT NULL PRIMARY KEY,
    EmpID INT NOT NULL
);

INSERT INTO #Emp (EmpRn, EmpID)
SELECT ROW_NUMBER() OVER (ORDER BY EmpID) AS EmpRn, EmpID
FROM HR_Employees
WHERE Status = N'Đang làm';

IF NOT EXISTS (SELECT 1 FROM #Emp)
BEGIN
    PRINT N'Khong co nhan vien dang lam de seed.';
    RETURN;
END;

-- ============================================================
-- 2) DATE RANGE
-- ============================================================
IF OBJECT_ID('tempdb..#Dates') IS NOT NULL DROP TABLE #Dates;
CREATE TABLE #Dates (
    DayIdx INT NOT NULL PRIMARY KEY,
    WorkDate DATE NOT NULL
);

;WITH n AS (
    SELECT 0 AS i
    UNION ALL
    SELECT i + 1 FROM n WHERE i < 29
)
INSERT INTO #Dates (DayIdx, WorkDate)
SELECT i, DATEADD(DAY, i, @StartDate)
FROM n
OPTION (MAXRECURSION 32);

-- ============================================================
-- 3) BUILD PLAN:
-- - 1 nhan vien / 1 ngay chi 1 ca
-- - Co 1 ngay nghi/7 ngay cho moi NV (thuc te van hanh)
-- - Xoay ca cong bang de phu 24/24
-- ============================================================
IF OBJECT_ID('tempdb..#Plan') IS NOT NULL DROP TABLE #Plan;
CREATE TABLE #Plan (
    EmpID INT NOT NULL,
    WorkDate DATE NOT NULL,
    ShiftID INT NULL
);

INSERT INTO #Plan (EmpID, WorkDate, ShiftID)
SELECT
    e.EmpID,
    d.WorkDate,
    CASE
        -- Nghi 1 ngay moi 7 ngay (khong insert schedule ngay nay)
        WHEN ((e.EmpRn + d.DayIdx) % 7) = 0 THEN NULL
        -- Xoay 3 ca theo block 2 ngay/ca de giam nhay ca lien tuc
        WHEN ((e.EmpRn + (d.DayIdx / 2)) % 3) = 0 THEN @ShiftSang
        WHEN ((e.EmpRn + (d.DayIdx / 2)) % 3) = 1 THEN @ShiftChieu
        ELSE @ShiftDem
    END AS ShiftID
FROM #Emp e
CROSS JOIN #Dates d;

-- ============================================================
-- 4) INSERT ONLY MISSING SCHEDULES
-- ============================================================
DECLARE @Inserted INT = 0;

INSERT INTO HR_Schedules
    (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, ShiftDefaultStart, ShiftDefaultEnd, CreatedAt)
SELECT
    p.EmpID,
    p.ShiftID,
    p.WorkDate,
    sh.DefaultStartTime,
    sh.DefaultEndTime,
    sh.DefaultStartTime,
    sh.DefaultEndTime,
    GETDATE()
FROM #Plan p
JOIN HR_Shifts sh ON sh.ShiftID = p.ShiftID
WHERE p.ShiftID IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM HR_Schedules s
      WHERE s.EmpID = p.EmpID
        AND s.WorkDate = p.WorkDate
  );

SET @Inserted = @@ROWCOUNT;

PRINT N'So lich da them moi: ' + CAST(@Inserted AS NVARCHAR(20));

-- ============================================================
-- 5) QA CHECKS
-- ============================================================

-- 5.1 Phu 3 ca theo ngay
SELECT
    s.WorkDate,
    sh.ShiftName,
    COUNT(*) AS SoNhanVien
FROM HR_Schedules s
JOIN HR_Shifts sh ON sh.ShiftID = s.ShiftID
WHERE s.WorkDate BETWEEN @StartDate AND @EndDate
GROUP BY s.WorkDate, sh.ShiftName
ORDER BY s.WorkDate, sh.ShiftName;

-- 5.2 Kiem tra trung lich (phai = 0)
SELECT
    s.EmpID, s.WorkDate, COUNT(*) AS SoLichTrongNgay
FROM HR_Schedules s
WHERE s.WorkDate BETWEEN @StartDate AND @EndDate
GROUP BY s.EmpID, s.WorkDate
HAVING COUNT(*) > 1;

-- 5.3 Uoc tinh payroll theo lich trong 30 ngay:
--     - Ca dem (22:00-06:00) +30%
--     - Moi ca mac dinh 8h
SELECT
    e.EmpID,
    e.FullName,
    COUNT(*) AS SoCaDuKien,
    SUM(CASE WHEN sh.DefaultStartTime = '22:00:00' AND sh.DefaultEndTime = '06:00:00' THEN 1 ELSE 0 END) AS SoCaDem,
    CAST(COUNT(*) * 8.0 AS DECIMAL(10,2)) AS TongGioDuKien,
    CAST(SUM(
        CASE
            WHEN sh.DefaultStartTime = '22:00:00' AND sh.DefaultEndTime = '06:00:00'
                THEN 8.0 * (e.HourlyRate * 1.3)
            ELSE 8.0 * e.HourlyRate
        END
    ) AS DECIMAL(18,0)) AS LuongDuKien_30Ngay
FROM HR_Schedules s
JOIN HR_Employees e ON e.EmpID = s.EmpID
JOIN HR_Shifts sh ON sh.ShiftID = s.ShiftID
WHERE s.WorkDate BETWEEN @StartDate AND @EndDate
GROUP BY e.EmpID, e.FullName, e.HourlyRate
ORDER BY e.EmpID;

PRINT N'=== DONE: SEED 30 NGAY TOI (3 CA + LICH THUC TE) ===';
GO
