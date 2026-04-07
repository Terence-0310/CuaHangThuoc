USE QuanLyCuaHangThuoc;
GO

SET NOCOUNT ON;

PRINT N'=== START: SEED PAYROLL DEMO TU ATTENDANCE ===';

DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @FromDate DATE = DATEADD(MONTH, -3, DATEFROMPARTS(YEAR(@Today), MONTH(@Today), 1));
DECLARE @ToDate DATE = EOMONTH(@Today);

IF OBJECT_ID('tempdb..#PayrollAgg') IS NOT NULL DROP TABLE #PayrollAgg;
CREATE TABLE #PayrollAgg (
    EmpID INT NOT NULL,
    Thang INT NOT NULL,
    Nam INT NOT NULL,
    TongGio DECIMAL(18,2) NOT NULL,
    TongTien DECIMAL(18,2) NOT NULL
);

INSERT INTO #PayrollAgg (EmpID, Thang, Nam, TongGio, TongTien)
SELECT
    a.EmpID,
    MONTH(s.WorkDate) AS Thang,
    YEAR(s.WorkDate) AS Nam,
    CAST(SUM(ISNULL(a.TotalHours, 0)) AS DECIMAL(18,2)) AS TongGio,
    CAST(SUM(ISNULL(a.DailyEarned, 0)) AS DECIMAL(18,2)) AS TongTien
FROM HR_Attendances a
JOIN HR_Schedules s ON s.ScheduleID = a.ScheduleID
JOIN HR_Employees e ON e.EmpID = a.EmpID
WHERE s.WorkDate BETWEEN @FromDate AND @ToDate
  AND e.Status = N'Đang làm'
GROUP BY a.EmpID, MONTH(s.WorkDate), YEAR(s.WorkDate)
HAVING SUM(ISNULL(a.TotalHours, 0)) > 0;

-- Upsert payroll để demo bảng lương full: tháng cũ = Đã thanh toán, tháng hiện tại = Chờ thanh toán.
MERGE HR_Payroll AS target
USING (
    SELECT
        p.EmpID, p.Thang, p.Nam, p.TongGio, p.TongTien,
        CASE
            WHEN p.Nam = YEAR(@Today) AND p.Thang = MONTH(@Today) THEN N'Chờ thanh toán'
            ELSE N'Đã thanh toán'
        END AS TrangThai,
        CASE
            WHEN p.Nam = YEAR(@Today) AND p.Thang = MONTH(@Today) THEN NULL
            ELSE EOMONTH(DATEFROMPARTS(p.Nam, p.Thang, 1))
        END AS NgayThanhToan
    FROM #PayrollAgg p
) AS src
ON target.EmpID = src.EmpID AND target.Thang = src.Thang AND target.Nam = src.Nam
WHEN MATCHED THEN
    UPDATE SET
        target.TongGio = src.TongGio,
        target.TongTien = src.TongTien,
        target.TrangThai = src.TrangThai,
        target.NgayThanhToan = src.NgayThanhToan
WHEN NOT MATCHED THEN
    INSERT (EmpID, Thang, Nam, TongGio, TongTien, TrangThai, NgayThanhToan)
    VALUES (src.EmpID, src.Thang, src.Nam, src.TongGio, src.TongTien, src.TrangThai, src.NgayThanhToan);

PRINT N'So dong payroll duoc tao/cap nhat: ' + CAST(@@ROWCOUNT AS NVARCHAR(20));

SELECT
    p.Nam, p.Thang,
    COUNT(*) AS SoNhanVien,
    SUM(CASE WHEN p.TrangThai = N'Đã thanh toán' THEN 1 ELSE 0 END) AS DaThanhToan,
    SUM(CASE WHEN p.TrangThai = N'Chờ thanh toán' THEN 1 ELSE 0 END) AS ChoThanhToan,
    CAST(SUM(p.TongGio) AS DECIMAL(18,2)) AS TongGio,
    CAST(SUM(p.TongTien) AS DECIMAL(18,0)) AS TongLuong
FROM HR_Payroll p
WHERE DATEFROMPARTS(p.Nam, p.Thang, 1) BETWEEN DATEFROMPARTS(YEAR(@FromDate), MONTH(@FromDate), 1)
                                             AND DATEFROMPARTS(YEAR(@ToDate), MONTH(@ToDate), 1)
GROUP BY p.Nam, p.Thang
ORDER BY p.Nam, p.Thang;

PRINT N'=== DONE: SEED PAYROLL DEMO TU ATTENDANCE ===';
GO
