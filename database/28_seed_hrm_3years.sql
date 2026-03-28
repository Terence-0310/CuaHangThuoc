-- ==================================================================
-- 28_seed_hrm_3years.sql
-- SEED 3 NĂM DỮ LIỆU HRM (2023-01-10 → 2026-03-28)
-- 5 Nhân viên, ~5000 Schedules, ~4000 Attendances
-- ==================================================================
USE QuanLyCuaHangThuoc;
GO
SET NOCOUNT ON;
BEGIN TRAN;

-- ==================================================================
-- STEP 1: XÓA SẠCH + RESET IDENTITY
-- ==================================================================
PRINT N'[1/4] XÓA SẠCH DỮ LIỆU CŨ...';

DELETE FROM HR_Attendances;
DELETE FROM HR_Schedules;
DELETE FROM HR_Employees;
DELETE FROM HR_Shifts;
DELETE FROM HR_Config;

DBCC CHECKIDENT ('HR_Attendances', RESEED, 0);
DBCC CHECKIDENT ('HR_Schedules',   RESEED, 0);
DBCC CHECKIDENT ('HR_Employees',   RESEED, 0);
DBCC CHECKIDENT ('HR_Shifts',      RESEED, 0);
DBCC CHECKIDENT ('HR_Config',      RESEED, 0);

PRINT N'   ✓ Đã xóa + Reset ID về 0.';

-- ==================================================================
-- STEP 2: SEED MASTER DATA
-- ==================================================================
PRINT N'[2/4] TẠO DỮ LIỆU GỐC...';

-- 2A: Config
INSERT INTO HR_Config (DefaultWeeklyLeave, DefaultAnnualLeave) VALUES (1, 12);

-- 2B: Shifts (ID 1→5)
INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES
    (N'Ca Sáng',           '06:00', '14:00'),   -- ID 1
    (N'Ca Chiều',          '14:00', '22:00'),   -- ID 2
    (N'Nghỉ Phép Tuần',   '00:00', '00:00'),   -- ID 3
    (N'Nghỉ Phép Năm',    '00:00', '00:00'),   -- ID 4
    (N'Nghỉ Không Lương',  '00:00', '00:00');   -- ID 5

-- 2C: Employees (ID 1→5)
INSERT INTO HR_Employees (FullName, PinCode, Phone, HourlyRate, OvertimeRate, HireDate, ResignDate, Status) VALUES
    (N'Lê Văn Minh',      '1001', '0901234001', 55000.00, 67500.00, '2023-01-10', NULL,         N'Đang làm'),  -- Senior 3 năm
    (N'Nguyễn Thị Lan',   '1002', '0901234002', 50000.00, 62500.00, '2023-01-10', NULL,         N'Đang làm'),  -- Senior 3 năm
    (N'Trần Văn Hùng',    '1003', '0901234003', 45000.00, 55000.00, '2023-06-01', '2024-12-31', N'Đã nghỉ'),   -- Đã nghỉ
    (N'Phạm Thị Mai',     '1004', '0901234004', 40000.00, 50000.00, '2025-05-01', NULL,         N'Đang làm'),  -- Mới vào
    (N'Hoàng Văn Đức',    '1005', '0901234005', 42000.00, 52000.00, '2025-05-01', NULL,         N'Đang làm');  -- Mới vào

PRINT N'   ✓ Config (1), Shifts (5), Employees (5).';

-- ==================================================================
-- STEP 3: TIME-TRAVEL LOOP (2023-01-10 → 2026-03-28)
-- ==================================================================
PRINT N'[3/4] BẮT ĐẦU SEED LỊCH LÀM + CHẤM CÔNG (3 năm)...';

-- Cache employee data
CREATE TABLE #Emp (
    Idx INT IDENTITY(1,1) PRIMARY KEY,
    EmpID INT, HireDate DATE, ResignDate DATE, HRate DECIMAL(18,2)
);
INSERT INTO #Emp (EmpID, HireDate, ResignDate, HRate)
SELECT EmpID, HireDate, ResignDate, HourlyRate FROM HR_Employees ORDER BY EmpID;

DECLARE @EmpCount INT = (SELECT COUNT(*) FROM #Emp);

-- Biến vòng lặp
DECLARE @CurDate DATE = '2023-01-10';
DECLARE @EndDate DATE = '2026-03-28';
DECLARE @Today   DATE = CAST(GETDATE() AS DATE);

-- Biến nhân viên
DECLARE @i INT, @EmpID INT, @HireDate DATE, @ResignDate DATE, @Rate DECIMAL(18,2);

-- Biến ca làm
DECLARE @ShiftID INT, @ActStart TIME, @ActEnd TIME, @SchedID INT;
DECLARE @rand INT;

-- Biến phép
DECLARE @WeeklyUsed INT, @AnnualUsed INT;
DECLARE @Monday DATE, @Sunday DATE;

-- Biến chấm công
DECLARE @LateMin INT, @OverMin INT, @LateReason NVARCHAR(200);
DECLARE @ClockIn DATETIME, @ClockOut DATETIME;
DECLARE @TotalHours DECIMAL(18,2), @DailyEarned DECIMAL(18,2);

-- Counter
DECLARE @TotalSched INT = 0, @TotalAttend INT = 0, @TotalNoShow INT = 0;

-- Late reasons pool
CREATE TABLE #Reasons (ID INT, Reason NVARCHAR(100));
INSERT INTO #Reasons VALUES
    (0, N'Kẹt xe trên đường đi làm'),
    (1, N'Hư xe giữa đường, phải sửa'),
    (2, N'Việc gia đình đột xuất'),
    (3, N'Thời tiết xấu, mưa lớn ngập đường'),
    (4, N'Dậy trễ do mất ngủ'),
    (5, N'Xe buýt đến trễ'),
    (6, N'Đưa con đi khám bệnh'),
    (7, N'Tắc đường do tai nạn giao thông'),
    (8, N'Quên điện thoại phải quay về lấy'),
    (9, N'Chờ thợ sửa ống nước ở nhà');

-- ============ MAIN LOOP ============
WHILE @CurDate <= @EndDate
BEGIN
    SET @i = 1;
    WHILE @i <= @EmpCount
    BEGIN
        -- Lấy thông tin NV
        SELECT @EmpID = EmpID, @HireDate = HireDate, @ResignDate = ResignDate, @Rate = HRate
        FROM #Emp WHERE Idx = @i;

        -- Kiểm tra vòng đời: NV phải đang trong thời gian làm việc
        IF @CurDate >= @HireDate AND (@ResignDate IS NULL OR @CurDate <= @ResignDate)
        BEGIN
            -- ====== QUYẾT ĐỊNH CA ======
            SET @rand = ABS(CHECKSUM(NEWID())) % 100;

            -- Tính Monday-Sunday của tuần chứa @CurDate
            SET @Monday = DATEADD(DAY, -((DATEPART(WEEKDAY, @CurDate) + 5) % 7), @CurDate);
            SET @Sunday = DATEADD(DAY, 6, @Monday);

            -- Đếm phép tuần đã dùng trong tuần này
            SELECT @WeeklyUsed = COUNT(*) FROM HR_Schedules s
            INNER JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID
            WHERE s.EmpID = @EmpID AND sh.ShiftName = N'Nghỉ Phép Tuần'
            AND s.WorkDate BETWEEN @Monday AND @Sunday;

            -- Đếm phép năm đã dùng trong năm này
            SELECT @AnnualUsed = COUNT(*) FROM HR_Schedules s
            INNER JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID
            WHERE s.EmpID = @EmpID AND sh.ShiftName = N'Nghỉ Phép Năm'
            AND YEAR(s.WorkDate) = YEAR(@CurDate);

            -- Tính thâm niên → tổng phép năm
            DECLARE @YearsWorked INT = DATEDIFF(YEAR, @HireDate, @CurDate);
            DECLARE @AnnualQuota INT = 12 + @YearsWorked;

            -- Logic phân ca
            IF @WeeklyUsed = 0 AND @rand < 14  -- ~1/7 ngày = nghỉ phép tuần
                SET @ShiftID = 3;
            ELSE IF @AnnualUsed < @AnnualQuota AND @rand >= 95  -- ~5% = nghỉ phép năm (rải rác)
                SET @ShiftID = 4;
            ELSE IF @rand < 50
                SET @ShiftID = 1;  -- Ca Sáng
            ELSE
                SET @ShiftID = 2;  -- Ca Chiều

            -- Lấy giờ ca
            SELECT @ActStart = DefaultStartTime, @ActEnd = DefaultEndTime
            FROM HR_Shifts WHERE ShiftID = @ShiftID;

            -- ====== INSERT SCHEDULE ======
            INSERT INTO HR_Schedules (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd)
            VALUES (@EmpID, @ShiftID, @CurDate, @ActStart, @ActEnd);

            SET @SchedID = SCOPE_IDENTITY();
            SET @TotalSched = @TotalSched + 1;

            -- ====== INSERT ATTENDANCE (chỉ ca làm + ngày quá khứ) ======
            IF @ShiftID IN (1, 2) AND @CurDate < @Today
            BEGIN
                SET @rand = ABS(CHECKSUM(NEWID())) % 100;

                IF @rand < 97  -- 97% đi làm bình thường
                BEGIN
                    -- Random trễ/sớm
                    SET @LateReason = NULL;
                    SET @LateMin = 0;
                    SET @OverMin = ABS(CHECKSUM(NEWID())) % 10;  -- 0-10 phút OT

                    IF ABS(CHECKSUM(NEWID())) % 100 < 12  -- 12% trễ
                    BEGIN
                        SET @LateMin = 5 + ABS(CHECKSUM(NEWID())) % 25;  -- Trễ 5-30 phút
                        SELECT @LateReason = Reason FROM #Reasons WHERE ID = ABS(CHECKSUM(NEWID())) % 10;
                        SET @LateReason = N'Trễ ' + CAST(@LateMin AS NVARCHAR) + N' phút - ' + @LateReason;
                    END
                    ELSE
                    BEGIN
                        SET @LateMin = -(ABS(CHECKSUM(NEWID())) % 8);  -- Sớm 0-8 phút
                    END

                    SET @ClockIn  = CAST(@CurDate AS DATETIME) + CAST(DATEADD(MINUTE, @LateMin, @ActStart) AS DATETIME);
                    SET @ClockOut = CAST(@CurDate AS DATETIME) + CAST(DATEADD(MINUTE, @OverMin, @ActEnd)   AS DATETIME);

                    SET @TotalHours = ROUND(DATEDIFF(MINUTE, @ClockIn, @ClockOut) / 60.0, 2);
                    SET @DailyEarned = ROUND(@TotalHours * @Rate, 0);

                    INSERT INTO HR_Attendances
                        (EmpID, ScheduleID, ClockIn, ClockOut, TotalHours, DailyEarned, LateReason, SnapshotRate, SnapshotStart, SnapshotEnd)
                    VALUES
                        (@EmpID, @SchedID, @ClockIn, @ClockOut, @TotalHours, @DailyEarned, @LateReason, @Rate, @ActStart, @ActEnd);

                    SET @TotalAttend = @TotalAttend + 1;
                END
                ELSE  -- 3% NO-SHOW
                BEGIN
                    SET @ClockIn = CAST(@CurDate AS DATETIME) + CAST(@ActStart AS DATETIME);

                    INSERT INTO HR_Attendances
                        (EmpID, ScheduleID, ClockIn, ClockOut, TotalHours, DailyEarned, LateReason, SnapshotRate, SnapshotStart, SnapshotEnd)
                    VALUES
                        (@EmpID, @SchedID, @ClockIn, @ClockIn, 0, 0,
                         N'[Hệ thống] Nghỉ không phép - NV không chấm công',
                         @Rate, @ActStart, @ActEnd);

                    SET @TotalNoShow = @TotalNoShow + 1;
                END
            END
        END

        SET @i = @i + 1;
    END

    -- Progress mỗi 6 tháng
    IF DAY(@CurDate) = 1 AND MONTH(@CurDate) IN (1, 7)
        PRINT N'   ... Đang seed: ' + CONVERT(VARCHAR(10), @CurDate, 120);

    SET @CurDate = DATEADD(DAY, 1, @CurDate);
END

-- ============ CLEANUP TEMP ============
DROP TABLE #Emp;
DROP TABLE #Reasons;

-- ==================================================================
-- STEP 4: BÁO CÁO
-- ==================================================================
PRINT N'[4/4] KẾT QUẢ SEED:';
PRINT N'   Schedules:  ' + CAST(@TotalSched AS VARCHAR);
PRINT N'   Attendance:  ' + CAST(@TotalAttend AS VARCHAR);
PRINT N'   No-Show:     ' + CAST(@TotalNoShow AS VARCHAR);

SELECT N'HR_Config'      AS [Bảng], COUNT(*) AS [Rows] FROM HR_Config      UNION ALL
SELECT N'HR_Shifts',                 COUNT(*)           FROM HR_Shifts      UNION ALL
SELECT N'HR_Employees',              COUNT(*)           FROM HR_Employees   UNION ALL
SELECT N'HR_Schedules',              COUNT(*)           FROM HR_Schedules   UNION ALL
SELECT N'HR_Attendances',            COUNT(*)           FROM HR_Attendances;

-- Phân tích nhanh
SELECT 
    e.FullName,
    COUNT(s.ScheduleID) AS [Tổng Ca],
    SUM(CASE WHEN sh.ShiftName = N'Ca Sáng' THEN 1 ELSE 0 END) AS [Ca Sáng],
    SUM(CASE WHEN sh.ShiftName = N'Ca Chiều' THEN 1 ELSE 0 END) AS [Ca Chiều],
    SUM(CASE WHEN sh.ShiftName = N'Nghỉ Phép Tuần' THEN 1 ELSE 0 END) AS [Phép Tuần],
    SUM(CASE WHEN sh.ShiftName = N'Nghỉ Phép Năm' THEN 1 ELSE 0 END) AS [Phép Năm],
    (SELECT COUNT(*) FROM HR_Attendances a WHERE a.EmpID = e.EmpID AND a.TotalHours > 0) AS [Ngày Làm],
    (SELECT COUNT(*) FROM HR_Attendances a WHERE a.EmpID = e.EmpID AND a.TotalHours = 0) AS [No-Show],
    (SELECT CAST(SUM(a.DailyEarned) AS DECIMAL(18,0)) FROM HR_Attendances a WHERE a.EmpID = e.EmpID) AS [Tổng Lương]
FROM HR_Employees e
LEFT JOIN HR_Schedules s ON e.EmpID = s.EmpID
LEFT JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID
GROUP BY e.EmpID, e.FullName
ORDER BY e.EmpID;

COMMIT TRAN;

PRINT N'';
PRINT N'=== SEED 3 NĂM HRM HOÀN TẤT! ===';
GO
