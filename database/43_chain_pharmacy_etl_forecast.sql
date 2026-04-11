USE QuanLyCuaHangThuoc;
GO

/* ============================================================
   ETL PROCEDURES FOR DEMAND ANALYTICS
   ============================================================ */

CREATE OR ALTER PROCEDURE sp_PopulateDimTime
    @FromDate DATE,
    @ToDate DATE
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH DateCte AS (
        SELECT @FromDate AS d
        UNION ALL
        SELECT DATEADD(DAY, 1, d)
        FROM DateCte
        WHERE d < @ToDate
    )
    MERGE DimTime AS target
    USING (
        SELECT
            CAST(CONVERT(VARCHAR(8), d, 112) AS INT) AS DateKey,
            d AS FullDate,
            DATEPART(DAY, d) AS DayInMonth,
            DATEPART(MONTH, d) AS MonthNum,
            DATEPART(QUARTER, d) AS QuarterNum,
            DATEPART(YEAR, d) AS YearNum,
            DATEPART(ISO_WEEK, d) AS WeekOfYear,
            DATEPART(WEEKDAY, d) AS DayOfWeekNum
        FROM DateCte
    ) AS src
    ON target.DateKey = src.DateKey
    WHEN NOT MATCHED THEN
        INSERT (DateKey, FullDate, DayInMonth, MonthNum, QuarterNum, YearNum, WeekOfYear, DayOfWeekNum)
        VALUES (src.DateKey, src.FullDate, src.DayInMonth, src.MonthNum, src.QuarterNum, src.YearNum, src.WeekOfYear, src.DayOfWeekNum)
    WHEN MATCHED THEN
        UPDATE SET
            FullDate = src.FullDate,
            DayInMonth = src.DayInMonth,
            MonthNum = src.MonthNum,
            QuarterNum = src.QuarterNum,
            YearNum = src.YearNum,
            WeekOfYear = src.WeekOfYear,
            DayOfWeekNum = src.DayOfWeekNum
    OPTION (MAXRECURSION 32767);
END
GO

CREATE OR ALTER PROCEDURE sp_RefreshFactSalesDaily
    @FromDate DATE,
    @ToDate DATE
AS
BEGIN
    SET NOCOUNT ON;

    DELETE f
    FROM FactSalesDaily f
    JOIN DimTime dt ON f.DateKey = dt.DateKey
    WHERE COALESCE(TRY_CONVERT(date, dt.FullDate), TRY_CONVERT(date, dt.[Date])) BETWEEN @FromDate AND @ToDate;

    INSERT INTO FactSalesDaily (DateKey, MaSP, MaCN, SoLuongBan, DoanhThu, GiaVonUocTinh, SoHoaDon)
    SELECT
        CAST(CONVERT(VARCHAR(8), CAST(hd.NgayBan AS DATE), 112) AS INT) AS DateKey,
        cthd.MaSP,
        COALESCE(hd.MaCN, 1) AS MaCN,
        SUM(cthd.SoLuong) AS SoLuongBan,
        SUM(cthd.ThanhTien) AS DoanhThu,
        SUM(ISNULL(lh.GiaNhap, 0) * cthd.SoLuong) AS GiaVonUocTinh,
        COUNT(DISTINCT hd.MaHD) AS SoHoaDon
    FROM HoaDon hd
    JOIN ChiTietHoaDon cthd ON hd.MaHD = cthd.MaHD
    LEFT JOIN LoHang lh ON cthd.MaLo = lh.MaLo
    WHERE CAST(hd.NgayBan AS DATE) BETWEEN @FromDate AND @ToDate
      AND (hd.TrangThai IS NULL OR hd.TrangThai <> N'Da huy')
    GROUP BY CAST(hd.NgayBan AS DATE), cthd.MaSP, COALESCE(hd.MaCN, 1);
END
GO

CREATE OR ALTER PROCEDURE sp_RefreshFactInventoryDaily
    @AtDate DATE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @DateKey INT = CAST(CONVERT(VARCHAR(8), @AtDate, 112) AS INT);

    DELETE FROM FactInventoryDaily WHERE DateKey = @DateKey;

    INSERT INTO FactInventoryDaily (DateKey, MaSP, MaCN, TonCuoiNgay, GiaTriTonUocTinh)
    SELECT
        @DateKey,
        tk.MaSP,
        tk.MaCN,
        tk.TonHienTai,
        CAST(tk.TonHienTai * ISNULL(avgCost.GiaVonTB, 0) AS DECIMAL(18,0))
    FROM TonKhoChiNhanh tk
    JOIN SanPham sp ON tk.MaSP = sp.MaSP
    OUTER APPLY (
        SELECT AVG(CAST(lh.GiaNhap AS FLOAT)) AS GiaVonTB
        FROM LoHang lh
        WHERE lh.MaSP = sp.MaSP
    ) avgCost;
END
GO

CREATE OR ALTER VIEW vw_DemandFeaturesDaily AS
WITH base AS (
    SELECT
        f.DateKey,
        COALESCE(TRY_CONVERT(date, dt.FullDate), TRY_CONVERT(date, dt.[Date])) AS FullDate,
        f.MaSP,
        f.MaCN,
        f.SoLuongBan,
        COALESCE(dt.DayOfWeekNum, dt.DayOfWeek) AS DayOfWeekNum,
        COALESCE(dt.MonthNum, dt.[Month]) AS MonthNum
    FROM FactSalesDaily f
    JOIN DimTime dt ON f.DateKey = dt.DateKey
)
SELECT
    b.DateKey,
    b.FullDate,
    b.MaSP,
    b.MaCN,
    b.SoLuongBan AS Demand,
    ISNULL(LAG(b.SoLuongBan, 1) OVER (PARTITION BY b.MaSP, b.MaCN ORDER BY b.FullDate), 0) AS Lag1,
    ISNULL(LAG(b.SoLuongBan, 7) OVER (PARTITION BY b.MaSP, b.MaCN ORDER BY b.FullDate), 0) AS Lag7,
    CAST(AVG(CAST(b.SoLuongBan AS FLOAT)) OVER (
        PARTITION BY b.MaSP, b.MaCN
        ORDER BY b.FullDate
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ) AS DECIMAL(18,4)) AS RollingMean7,
    b.DayOfWeekNum,
    b.MonthNum
FROM base b;
GO

PRINT N'43_chain_pharmacy_etl_forecast.sql executed successfully';
GO
