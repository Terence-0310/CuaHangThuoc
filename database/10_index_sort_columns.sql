-- ============================================================
-- FILE: 10_index_sort_columns.sql
-- MO TA: Tạo INDEX cho các cột được phép sort trên ProductPanel
--        Tránh Table Scan khi ORDER BY + OFFSET/FETCH
-- ============================================================
USE QuanLyCuaHangThuoc;
GO

-- Cột TenSP — sort theo tên thuốc (NVARCHAR → dùng NONCLUSTERED)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SanPham_TenSP' AND object_id = OBJECT_ID('SanPham'))
    CREATE NONCLUSTERED INDEX IX_SanPham_TenSP ON SanPham(TenSP);
GO

-- Cột GiaBan — sort theo giá bán lẻ
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SanPham_GiaBan' AND object_id = OBJECT_ID('SanPham'))
    CREATE NONCLUSTERED INDEX IX_SanPham_GiaBan ON SanPham(GiaBan);
GO

-- Cột GiaBanSi — sort theo giá sỉ
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SanPham_GiaBanSi' AND object_id = OBJECT_ID('SanPham'))
    CREATE NONCLUSTERED INDEX IX_SanPham_GiaBanSi ON SanPham(GiaBanSi);
GO

-- Cột TrangThai — filter + sort theo trạng thái
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SanPham_TrangThai' AND object_id = OBJECT_ID('SanPham'))
    CREATE NONCLUSTERED INDEX IX_SanPham_TrangThai ON SanPham(TrangThai);
GO

-- Composite index: TrangThai + TenSP (tối ưu filter + sort cùng lúc)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SanPham_TrangThai_TenSP' AND object_id = OBJECT_ID('SanPham'))
    CREATE NONCLUSTERED INDEX IX_SanPham_TrangThai_TenSP ON SanPham(TrangThai, TenSP);
GO

PRINT N'✅ Đã tạo INDEX cho các cột sort: TenSP, GiaBan, GiaBanSi, TrangThai';
GO
