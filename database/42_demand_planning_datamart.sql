USE QuanLyCuaHangThuoc;
GO

-- ============================================================
-- Demand planning data model + data mart + forecasting + optimization
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ChiNhanh')
BEGIN
    CREATE TABLE ChiNhanh (
        MaCN INT IDENTITY(1,1) PRIMARY KEY,
        MaVung NVARCHAR(20) NOT NULL,
        TenChiNhanh NVARCHAR(200) NOT NULL,
        TinhThanh NVARCHAR(100) NULL,
        QuanHuyen NVARCHAR(100) NULL,
        TrangThai BIT NOT NULL DEFAULT 1,
        NgayTao DATETIME NOT NULL DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM ChiNhanh)
BEGIN
    INSERT INTO ChiNhanh (MaVung, TenChiNhanh, TinhThanh, QuanHuyen)
    VALUES (N'R-HCM', N'Chi nhánh trung tâm', N'TP.HCM', N'Quận 1');
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'MaCN')
BEGIN
    ALTER TABLE HoaDon ADD MaCN INT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('PhieuNhap') AND name = 'MaCN')
BEGIN
    ALTER TABLE PhieuNhap ADD MaCN INT NULL;
END
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'MaCN')
BEGIN
    UPDATE HoaDon SET MaCN = ISNULL(MaCN, (SELECT TOP 1 MaCN FROM ChiNhanh ORDER BY MaCN));
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_HoaDon_ChiNhanh')
    BEGIN
        ALTER TABLE HoaDon ADD CONSTRAINT FK_HoaDon_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN);
    END
END
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('PhieuNhap') AND name = 'MaCN')
BEGIN
    UPDATE PhieuNhap SET MaCN = ISNULL(MaCN, (SELECT TOP 1 MaCN FROM ChiNhanh ORDER BY MaCN));
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_PhieuNhap_ChiNhanh')
    BEGIN
        ALTER TABLE PhieuNhap ADD CONSTRAINT FK_PhieuNhap_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN);
    END
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'DimRegion')
BEGIN
    CREATE TABLE DimRegion (
        RegionKey INT IDENTITY(1,1) PRIMARY KEY,
        MaVung NVARCHAR(20) NOT NULL UNIQUE,
        TenVung NVARCHAR(100) NOT NULL
    );
END
GO

MERGE DimRegion AS target
USING (
    SELECT DISTINCT MaVung, CONCAT(N'Vùng ', MaVung) AS TenVung
    FROM ChiNhanh
) AS src
ON target.MaVung = src.MaVung
WHEN MATCHED THEN UPDATE SET target.TenVung = src.TenVung
WHEN NOT MATCHED THEN INSERT (MaVung, TenVung) VALUES (src.MaVung, src.TenVung);
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'DimTime')
BEGIN
    CREATE TABLE DimTime (
        DateKey INT PRIMARY KEY, -- yyyymmdd
        [Date] DATE NOT NULL UNIQUE,
        [Year] INT NOT NULL,
        [Month] INT NOT NULL,
        [Week] INT NOT NULL,
        [Quarter] INT NOT NULL,
        DayOfWeek INT NOT NULL,
        IsWeekend BIT NOT NULL
    );
END
GO

/* Compatibility columns for chain_pharmacy ETL scripts */
IF COL_LENGTH('DimTime', 'FullDate') IS NULL
BEGIN
    ALTER TABLE DimTime ADD FullDate DATE NULL;
END
GO

IF COL_LENGTH('DimTime', 'DayInMonth') IS NULL
BEGIN
    ALTER TABLE DimTime ADD DayInMonth INT NULL;
END
GO

IF COL_LENGTH('DimTime', 'MonthNum') IS NULL
BEGIN
    ALTER TABLE DimTime ADD MonthNum INT NULL;
END
GO

IF COL_LENGTH('DimTime', 'QuarterNum') IS NULL
BEGIN
    ALTER TABLE DimTime ADD QuarterNum INT NULL;
END
GO

IF COL_LENGTH('DimTime', 'YearNum') IS NULL
BEGIN
    ALTER TABLE DimTime ADD YearNum INT NULL;
END
GO

IF COL_LENGTH('DimTime', 'WeekOfYear') IS NULL
BEGIN
    ALTER TABLE DimTime ADD WeekOfYear INT NULL;
END
GO

IF COL_LENGTH('DimTime', 'DayOfWeekNum') IS NULL
BEGIN
    ALTER TABLE DimTime ADD DayOfWeekNum INT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'FactSalesDaily')
BEGIN
    CREATE TABLE FactSalesDaily (
        DateKey INT NOT NULL,
        MaSP INT NOT NULL,
        MaCN INT NOT NULL,
        RegionKey INT NOT NULL,
        SoLuongBan INT NOT NULL,
        DoanhThu DECIMAL(18,0) NOT NULL,
        GiaVon DECIMAL(18,0) NOT NULL,
        CONSTRAINT PK_FactSalesDaily PRIMARY KEY (DateKey, MaSP, MaCN),
        CONSTRAINT FK_FactSalesDaily_DimTime FOREIGN KEY (DateKey) REFERENCES DimTime(DateKey),
        CONSTRAINT FK_FactSalesDaily_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT FK_FactSalesDaily_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN),
        CONSTRAINT FK_FactSalesDaily_DimRegion FOREIGN KEY (RegionKey) REFERENCES DimRegion(RegionKey)
    );
END
GO

IF COL_LENGTH('FactSalesDaily', 'GiaVonUocTinh') IS NULL
BEGIN
    ALTER TABLE FactSalesDaily ADD GiaVonUocTinh DECIMAL(18,0) NULL;
END
GO

IF COL_LENGTH('FactSalesDaily', 'SoHoaDon') IS NULL
BEGIN
    ALTER TABLE FactSalesDaily ADD SoHoaDon INT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'FactInventoryDaily')
BEGIN
    CREATE TABLE FactInventoryDaily (
        DateKey INT NOT NULL,
        MaSP INT NOT NULL,
        MaCN INT NOT NULL,
        RegionKey INT NOT NULL,
        TonDauNgay INT NOT NULL,
        TonCuoiNgay INT NOT NULL,
        SafetyStock INT NOT NULL DEFAULT 0,
        CONSTRAINT PK_FactInventoryDaily PRIMARY KEY (DateKey, MaSP, MaCN),
        CONSTRAINT FK_FactInventoryDaily_DimTime FOREIGN KEY (DateKey) REFERENCES DimTime(DateKey),
        CONSTRAINT FK_FactInventoryDaily_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT FK_FactInventoryDaily_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN),
        CONSTRAINT FK_FactInventoryDaily_DimRegion FOREIGN KEY (RegionKey) REFERENCES DimRegion(RegionKey)
    );
END
GO

IF COL_LENGTH('FactInventoryDaily', 'GiaTriTonUocTinh') IS NULL
BEGIN
    ALTER TABLE FactInventoryDaily ADD GiaTriTonUocTinh DECIMAL(18,0) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'DemandForecast')
BEGIN
    CREATE TABLE DemandForecast (
        ForecastId BIGINT IDENTITY(1,1) PRIMARY KEY,
        ForecastDate DATE NOT NULL,
        MaSP INT NOT NULL,
        MaCN INT NOT NULL,
        RegionKey INT NOT NULL,
        ForecastQty DECIMAL(18,2) NOT NULL,
        LowerBoundQty DECIMAL(18,2) NULL,
        UpperBoundQty DECIMAL(18,2) NULL,
        ModelName NVARCHAR(50) NOT NULL DEFAULT N'RollingMean',
        GeneratedAt DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_DemandForecast_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT FK_DemandForecast_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN),
        CONSTRAINT FK_DemandForecast_Region FOREIGN KEY (RegionKey) REFERENCES DimRegion(RegionKey)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'InventoryPolicy')
BEGIN
    CREATE TABLE InventoryPolicy (
        PolicyId INT IDENTITY(1,1) PRIMARY KEY,
        MaSP INT NOT NULL,
        MaCN INT NOT NULL,
        LeadTimeDays INT NOT NULL DEFAULT 3,
        ServiceLevel DECIMAL(5,2) NOT NULL DEFAULT 0.95,
        SafetyStock INT NOT NULL DEFAULT 0,
        ReorderPoint INT NOT NULL DEFAULT 0,
        IsActive BIT NOT NULL DEFAULT 1,
        UpdatedAt DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT UQ_InventoryPolicy UNIQUE (MaSP, MaCN),
        CONSTRAINT FK_InventoryPolicy_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT FK_InventoryPolicy_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ReplenishmentSuggestion')
BEGIN
    CREATE TABLE ReplenishmentSuggestion (
        SuggestionId BIGINT IDENTITY(1,1) PRIMARY KEY,
        SuggestedDate DATE NOT NULL,
        MaSP INT NOT NULL,
        MaCN INT NOT NULL,
        ForecastWindowDays INT NOT NULL,
        CurrentStock INT NOT NULL,
        RequiredQty INT NOT NULL,
        SuggestedQty INT NOT NULL,
        SuggestionType NVARCHAR(20) NOT NULL DEFAULT N'PO',
        Status NVARCHAR(20) NOT NULL DEFAULT N'NEW',
        GeneratedAt DATETIME NOT NULL DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'InternalTransferSuggestion')
BEGIN
    CREATE TABLE InternalTransferSuggestion (
        TransferSuggestionId BIGINT IDENTITY(1,1) PRIMARY KEY,
        SuggestedDate DATE NOT NULL,
        MaSP INT NOT NULL,
        FromMaCN INT NOT NULL,
        ToMaCN INT NOT NULL,
        SuggestedQty INT NOT NULL,
        Reason NVARCHAR(200) NULL,
        Status NVARCHAR(20) NOT NULL DEFAULT N'NEW',
        GeneratedAt DATETIME NOT NULL DEFAULT GETDATE()
    );
END
GO

CREATE OR ALTER PROCEDURE sp_DemandPlanning_RefreshDataMart
    @FromDate DATE = NULL,
    @ToDate DATE = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET @FromDate = ISNULL(@FromDate, DATEADD(DAY, -120, CAST(GETDATE() AS DATE)));
    SET @ToDate = ISNULL(@ToDate, CAST(GETDATE() AS DATE));

    ;WITH d AS (
        SELECT @FromDate AS d
        UNION ALL
        SELECT DATEADD(DAY, 1, d) FROM d WHERE d < @ToDate
    )
    MERGE DimTime AS target
    USING (
        SELECT
            CAST(CONVERT(VARCHAR(8), d, 112) AS INT) AS DateKey,
            d AS [Date],
            YEAR(d) AS [Year],
            MONTH(d) AS [Month],
            DATEPART(ISO_WEEK, d) AS [Week],
            DATEPART(QUARTER, d) AS [Quarter],
            DATEPART(WEEKDAY, d) AS DayOfWeek,
            CASE WHEN DATEPART(WEEKDAY, d) IN (1,7) THEN 1 ELSE 0 END AS IsWeekend
        FROM d
    ) AS src
    ON target.DateKey = src.DateKey
    WHEN MATCHED THEN UPDATE SET
        target.[Date] = src.[Date],
        target.FullDate = src.[Date],
        target.DayInMonth = DAY(src.[Date]),
        target.[Year] = src.[Year],
        target.YearNum = src.[Year],
        target.[Month] = src.[Month],
        target.MonthNum = src.[Month],
        target.[Week] = src.[Week],
        target.WeekOfYear = src.[Week],
        target.[Quarter] = src.[Quarter],
        target.QuarterNum = src.[Quarter],
        target.DayOfWeek = src.DayOfWeek,
        target.DayOfWeekNum = src.DayOfWeek,
        target.IsWeekend = src.IsWeekend
    WHEN NOT MATCHED THEN INSERT (DateKey, [Date], [Year], [Month], [Week], [Quarter], DayOfWeek, IsWeekend)
        VALUES (src.DateKey, src.[Date], src.[Year], src.[Month], src.[Week], src.[Quarter], src.DayOfWeek, src.IsWeekend)
    OPTION (MAXRECURSION 1000);

    UPDATE DimTime
    SET
        FullDate = ISNULL(FullDate, [Date]),
        DayInMonth = ISNULL(DayInMonth, DAY([Date])),
        MonthNum = ISNULL(MonthNum, [Month]),
        QuarterNum = ISNULL(QuarterNum, [Quarter]),
        YearNum = ISNULL(YearNum, [Year]),
        WeekOfYear = ISNULL(WeekOfYear, [Week]),
        DayOfWeekNum = ISNULL(DayOfWeekNum, DayOfWeek)
    WHERE [Date] BETWEEN @FromDate AND @ToDate;

    DELETE FROM FactSalesDaily WHERE DateKey BETWEEN CAST(CONVERT(VARCHAR(8), @FromDate, 112) AS INT)
                                                 AND CAST(CONVERT(VARCHAR(8), @ToDate, 112) AS INT);

    INSERT INTO FactSalesDaily (DateKey, MaSP, MaCN, RegionKey, SoLuongBan, DoanhThu, GiaVon)
    SELECT
        CAST(CONVERT(VARCHAR(8), CAST(hd.NgayBan AS DATE), 112) AS INT) AS DateKey,
        ct.MaSP,
        hd.MaCN,
        dr.RegionKey,
        SUM(ct.SoLuong) AS SoLuongBan,
        SUM(ct.ThanhTien) AS DoanhThu,
        SUM(ISNULL(ct.GiaVon, 0) * ct.SoLuong) AS GiaVon
    FROM HoaDon hd
    JOIN ChiTietHoaDon ct ON ct.MaHD = hd.MaHD
    JOIN ChiNhanh cn ON cn.MaCN = hd.MaCN
    JOIN DimRegion dr ON dr.MaVung = cn.MaVung
    WHERE CAST(hd.NgayBan AS DATE) BETWEEN @FromDate AND @ToDate
      AND ISNULL(hd.TrangThai, N'Thanh cong') = N'Thanh cong'
      AND ISNULL(hd.LoaiHD, N'SALE') = N'SALE'
    GROUP BY CAST(hd.NgayBan AS DATE), ct.MaSP, hd.MaCN, dr.RegionKey;

    DELETE FROM FactInventoryDaily WHERE DateKey BETWEEN CAST(CONVERT(VARCHAR(8), @FromDate, 112) AS INT)
                                                    AND CAST(CONVERT(VARCHAR(8), @ToDate, 112) AS INT);

    INSERT INTO FactInventoryDaily (DateKey, MaSP, MaCN, RegionKey, TonDauNgay, TonCuoiNgay, SafetyStock)
    SELECT
        CAST(CONVERT(VARCHAR(8), d.[Date], 112) AS INT) AS DateKey,
        sp.MaSP,
        cn.MaCN,
        dr.RegionKey,
        ISNULL(base.TonKho, 0) AS TonDauNgay,
        ISNULL(base.TonKho, 0) - ISNULL(sale.SoLuongBan, 0) AS TonCuoiNgay,
        ISNULL(ip.SafetyStock, 0) AS SafetyStock
    FROM DimTime d
    CROSS JOIN SanPham sp
    CROSS JOIN ChiNhanh cn
    JOIN DimRegion dr ON dr.MaVung = cn.MaVung
    OUTER APPLY (
        SELECT ISNULL(SUM(lh.SoLuong), 0) AS TonKho
        FROM LoHang lh
        WHERE lh.MaSP = sp.MaSP
    ) base
    OUTER APPLY (
        SELECT ISNULL(SUM(fs.SoLuongBan), 0) AS SoLuongBan
        FROM FactSalesDaily fs
        WHERE fs.DateKey = CAST(CONVERT(VARCHAR(8), d.[Date], 112) AS INT)
          AND fs.MaSP = sp.MaSP
          AND fs.MaCN = cn.MaCN
    ) sale
    LEFT JOIN InventoryPolicy ip ON ip.MaSP = sp.MaSP AND ip.MaCN = cn.MaCN
    WHERE d.[Date] BETWEEN @FromDate AND @ToDate
      AND sp.TrangThai = 1;
END
GO

CREATE OR ALTER PROCEDURE sp_DemandPlanning_GenerateForecast
    @HorizonDays INT = 14,
    @LookbackDays INT = 30
AS
BEGIN
    SET NOCOUNT ON;
    IF @HorizonDays < 1 SET @HorizonDays = 14;
    IF @LookbackDays < 7 SET @LookbackDays = 30;

    DECLARE @today DATE = CAST(GETDATE() AS DATE);
    DELETE FROM DemandForecast WHERE ForecastDate >= @today;

    ;WITH base AS (
        SELECT
            fs.MaSP,
            fs.MaCN,
            fs.RegionKey,
            CAST(AVG(CAST(fs.SoLuongBan AS DECIMAL(18,2))) AS DECIMAL(18,2)) AS AvgDemand,
            CAST(STDEV(CAST(fs.SoLuongBan AS FLOAT)) AS DECIMAL(18,2)) AS StdDemand
        FROM FactSalesDaily fs
        JOIN DimTime dt ON dt.DateKey = fs.DateKey
        WHERE dt.[Date] BETWEEN DATEADD(DAY, -@LookbackDays, @today) AND DATEADD(DAY, -1, @today)
        GROUP BY fs.MaSP, fs.MaCN, fs.RegionKey
    ),
    seq AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1 FROM seq WHERE n < @HorizonDays
    )
    INSERT INTO DemandForecast (ForecastDate, MaSP, MaCN, RegionKey, ForecastQty, LowerBoundQty, UpperBoundQty, ModelName)
    SELECT
        DATEADD(DAY, s.n, @today) AS ForecastDate,
        b.MaSP,
        b.MaCN,
        b.RegionKey,
        b.AvgDemand AS ForecastQty,
        CASE WHEN b.AvgDemand - ISNULL(b.StdDemand, 0) < 0 THEN 0 ELSE b.AvgDemand - ISNULL(b.StdDemand, 0) END AS LowerBoundQty,
        b.AvgDemand + ISNULL(b.StdDemand, 0) AS UpperBoundQty,
        N'RollingMean'
    FROM base b
    CROSS JOIN seq s
    OPTION (MAXRECURSION 1000);
END
GO

CREATE OR ALTER PROCEDURE sp_DemandPlanning_RunOptimization
    @HorizonDays INT = 14
AS
BEGIN
    SET NOCOUNT ON;
    IF @HorizonDays < 1 SET @HorizonDays = 14;

    DECLARE @today DATE = CAST(GETDATE() AS DATE);
    DELETE FROM ReplenishmentSuggestion WHERE SuggestedDate = @today;
    DELETE FROM InternalTransferSuggestion WHERE SuggestedDate = @today;

    ;WITH agg AS (
        SELECT
            df.MaSP,
            df.MaCN,
            SUM(df.ForecastQty) AS ForecastQtyWindow
        FROM DemandForecast df
        WHERE df.ForecastDate BETWEEN DATEADD(DAY, 1, @today) AND DATEADD(DAY, @HorizonDays, @today)
        GROUP BY df.MaSP, df.MaCN
    ),
    stock AS (
        SELECT MaSP, SUM(SoLuong) AS CurrentStock
        FROM LoHang
        GROUP BY MaSP
    ),
    pol AS (
        SELECT
            MaSP,
            MaCN,
            ISNULL(SafetyStock, 0) AS SafetyStock,
            ISNULL(ReorderPoint, 0) AS ReorderPoint
        FROM InventoryPolicy
        WHERE IsActive = 1
    )
    INSERT INTO ReplenishmentSuggestion (SuggestedDate, MaSP, MaCN, ForecastWindowDays, CurrentStock, RequiredQty, SuggestedQty, SuggestionType, Status)
    SELECT
        @today,
        a.MaSP,
        a.MaCN,
        @HorizonDays,
        ISNULL(s.CurrentStock, 0) AS CurrentStock,
        CAST(CEILING(a.ForecastQtyWindow + ISNULL(p.SafetyStock, 0)) AS INT) AS RequiredQty,
        CASE
            WHEN CAST(CEILING(a.ForecastQtyWindow + ISNULL(p.SafetyStock, 0)) AS INT) - ISNULL(s.CurrentStock, 0) > 0
                THEN CAST(CEILING(a.ForecastQtyWindow + ISNULL(p.SafetyStock, 0)) AS INT) - ISNULL(s.CurrentStock, 0)
            ELSE 0
        END AS SuggestedQty,
        N'PO',
        N'NEW'
    FROM agg a
    LEFT JOIN stock s ON s.MaSP = a.MaSP
    LEFT JOIN pol p ON p.MaSP = a.MaSP AND p.MaCN = a.MaCN
    WHERE CAST(CEILING(a.ForecastQtyWindow + ISNULL(p.SafetyStock, 0)) AS INT) > ISNULL(s.CurrentStock, 0);

    ;WITH deficit AS (
        SELECT
            r.MaSP,
            r.MaCN AS ToMaCN,
            r.SuggestedQty AS NeededQty
        FROM ReplenishmentSuggestion r
        WHERE r.SuggestedDate = @today AND r.SuggestedQty > 0
    ),
    surplus AS (
        SELECT
            p.MaSP,
            p.MaCN AS FromMaCN,
            CASE WHEN SUM(lh.SoLuong) - (p.ReorderPoint + p.SafetyStock) > 0
                 THEN SUM(lh.SoLuong) - (p.ReorderPoint + p.SafetyStock)
                 ELSE 0 END AS SurplusQty
        FROM InventoryPolicy p
        JOIN LoHang lh ON lh.MaSP = p.MaSP
        WHERE p.IsActive = 1
        GROUP BY p.MaSP, p.MaCN, p.ReorderPoint, p.SafetyStock
        HAVING CASE WHEN SUM(lh.SoLuong) - (p.ReorderPoint + p.SafetyStock) > 0
                 THEN SUM(lh.SoLuong) - (p.ReorderPoint + p.SafetyStock)
                 ELSE 0 END > 0
    )
    INSERT INTO InternalTransferSuggestion (SuggestedDate, MaSP, FromMaCN, ToMaCN, SuggestedQty, Reason, Status)
    SELECT
        @today,
        d.MaSP,
        s.FromMaCN,
        d.ToMaCN,
        CASE WHEN s.SurplusQty > d.NeededQty THEN d.NeededQty ELSE s.SurplusQty END AS SuggestedQty,
        N'Điều chuyển nội bộ trước khi tạo PO',
        N'NEW'
    FROM deficit d
    JOIN surplus s ON s.MaSP = d.MaSP
    WHERE s.FromMaCN <> d.ToMaCN;
END
GO

CREATE OR ALTER VIEW vw_DemandPlanning_Evaluation AS
SELECT
    fs.MaSP,
    fs.MaCN,
    dt.[Date],
    CAST(fs.SoLuongBan AS DECIMAL(18,2)) AS ActualQty,
    df.ForecastQty,
    ABS(CAST(fs.SoLuongBan AS DECIMAL(18,2)) - df.ForecastQty) AS AE,
    POWER(CAST(fs.SoLuongBan AS DECIMAL(18,2)) - df.ForecastQty, 2) AS SE,
    CASE WHEN fs.SoLuongBan = 0 THEN NULL
         ELSE ABS(CAST(fs.SoLuongBan AS DECIMAL(18,2)) - df.ForecastQty) / NULLIF(CAST(fs.SoLuongBan AS DECIMAL(18,2)), 0) END AS APE
FROM FactSalesDaily fs
JOIN DimTime dt ON dt.DateKey = fs.DateKey
JOIN DemandForecast df
    ON df.MaSP = fs.MaSP
   AND df.MaCN = fs.MaCN
   AND df.ForecastDate = dt.[Date];
GO

CREATE OR ALTER PROCEDURE sp_DemandPlanning_Evaluate
    @BacktestDays INT = 30
AS
BEGIN
    SET NOCOUNT ON;
    IF @BacktestDays < 7 SET @BacktestDays = 30;

    DECLARE @fromDate DATE = DATEADD(DAY, -@BacktestDays, CAST(GETDATE() AS DATE));
    SELECT
        COUNT(1) AS SampleSize,
        CAST(AVG(AE) AS DECIMAL(18,4)) AS MAE,
        CAST(SQRT(AVG(SE)) AS DECIMAL(18,4)) AS RMSE,
        CAST(AVG(APE) * 100.0 AS DECIMAL(18,4)) AS MAPE
    FROM vw_DemandPlanning_Evaluation
    WHERE [Date] >= @fromDate;
END
GO

PRINT N'42_demand_planning_datamart.sql completed';
GO
