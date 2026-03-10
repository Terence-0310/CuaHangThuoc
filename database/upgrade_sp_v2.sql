/* Upgrade sp_SanPham_GetPage v2: Search + Sort */
USE CuaHangThuoc_Batch;
GO

CREATE OR ALTER PROC dbo.sp_SanPham_GetPage
    @PageNumber  INT = 1,
    @PageSize    INT = 10,
    @Keyword     NVARCHAR(255) = NULL,
    @SortColumn  NVARCHAR(50)  = N'MaSanPham',
    @SortOrder   NVARCHAR(4)   = N'ASC'
AS
BEGIN
    SET NOCOUNT ON;

    IF @SortOrder NOT IN (N'ASC', N'DESC')
        SET @SortOrder = N'ASC';

    IF @SortColumn NOT IN (N'MaSanPham', N'TenSanPham', N'DonViTinh',
                           N'GiaBanDeXuat', N'LoaiSanPham', N'MoTa', N'MucTonToiThieu')
        SET @SortColumn = N'MaSanPham';

    SET @Keyword = LTRIM(RTRIM(@Keyword));
    IF @Keyword = N'' SET @Keyword = NULL;

    DECLARE @TotalRows INT;
    SELECT @TotalRows = COUNT(*)
    FROM dbo.SanPham
    WHERE DaXoa = 0
      AND (@Keyword IS NULL OR TenSanPham LIKE N'%' + @Keyword + N'%');

    SELECT
        sp.MaSanPham,
        sp.TenSanPham,
        sp.DonViTinh,
        sp.GiaBanDeXuat,
        sp.LoaiSanPham,
        sp.MoTa,
        sp.MucTonToiThieu,
        ISNULL(tk.TongTon, 0) AS TongTon,
        @TotalRows              AS TotalRows,
        CEILING(CAST(@TotalRows AS FLOAT) / @PageSize) AS TotalPages
    FROM dbo.SanPham sp
    LEFT JOIN dbo.v_TonKhoSanPham tk ON tk.MaSanPham = sp.MaSanPham
    WHERE sp.DaXoa = 0
      AND (@Keyword IS NULL OR sp.TenSanPham LIKE N'%' + @Keyword + N'%')
    ORDER BY
        CASE WHEN @SortOrder = N'ASC' THEN
            CASE @SortColumn
                WHEN N'MaSanPham'      THEN RIGHT('0000000000' + CAST(sp.MaSanPham AS NVARCHAR), 10)
                WHEN N'TenSanPham'     THEN sp.TenSanPham
                WHEN N'DonViTinh'      THEN sp.DonViTinh
                WHEN N'GiaBanDeXuat'   THEN RIGHT('0000000000' + CAST(CAST(sp.GiaBanDeXuat AS BIGINT) AS NVARCHAR), 10)
                WHEN N'LoaiSanPham'    THEN sp.LoaiSanPham
                WHEN N'MoTa'           THEN ISNULL(sp.MoTa, N'')
                WHEN N'MucTonToiThieu' THEN RIGHT('0000000000' + CAST(sp.MucTonToiThieu AS NVARCHAR), 10)
            END
        END ASC,
        CASE WHEN @SortOrder = N'DESC' THEN
            CASE @SortColumn
                WHEN N'MaSanPham'      THEN RIGHT('0000000000' + CAST(sp.MaSanPham AS NVARCHAR), 10)
                WHEN N'TenSanPham'     THEN sp.TenSanPham
                WHEN N'DonViTinh'      THEN sp.DonViTinh
                WHEN N'GiaBanDeXuat'   THEN RIGHT('0000000000' + CAST(CAST(sp.GiaBanDeXuat AS BIGINT) AS NVARCHAR), 10)
                WHEN N'LoaiSanPham'    THEN sp.LoaiSanPham
                WHEN N'MoTa'           THEN ISNULL(sp.MoTa, N'')
                WHEN N'MucTonToiThieu' THEN RIGHT('0000000000' + CAST(sp.MucTonToiThieu AS NVARCHAR), 10)
            END
        END DESC
    OFFSET (@PageNumber - 1) * @PageSize ROWS
    FETCH NEXT @PageSize ROWS ONLY;
END
GO

PRINT 'sp_SanPham_GetPage v2 upgraded successfully!';
GO
