# 📖 Apothecary Pro — Hướng Dẫn Dự Án

> **Hệ thống Quản lý Cửa hàng Thuốc** | Java Swing + SQL Server  
> Cập nhật: 23/03/2026

---

## 📋 Mục Lục

1. [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
2. [Cài đặt & Chạy lần đầu](#-cài-đặt--chạy-lần-đầu)
3. [Kiến trúc tổng quan](#-kiến-trúc-tổng-quan)
4. [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
5. [MVP Pattern — Tách UI khỏi Logic](#-mvp-pattern--tách-ui-khỏi-logic)
6. [Quy ước code](#-quy-ước-code)
7. [Database](#-database)
8. [Hướng dẫn phát triển module mới](#-hướng-dẫn-phát-triển-module-mới)
9. [Tài khoản demo](#-tài-khoản-demo)
10. [Lệnh thường dùng](#-lệnh-thường-dùng)
11. [Troubleshooting](#-troubleshooting)

---

## 💻 Yêu cầu hệ thống

| Phần mềm | Version tối thiểu | Ghi chú |
|-----------|:-----------------:|---------|
| **JDK** | 17+ | `java -version` để kiểm tra |
| **Maven** | 3.8+ | `mvn -version` |
| **SQL Server** | 2019+ | Express edition OK |
| **IDE** | Bất kỳ | IntelliJ / Eclipse / VS Code |

---

## 🚀 Cài đặt & Chạy lần đầu

### Bước 1: Clone dự án

```bash
git clone https://github.com/BaoVuong150/eProject-StoreBanThuoc.git
cd eProject-StoreBanThuoc
```

### Bước 2: Tạo database

Chạy lần lượt các file SQL trong thư mục `database/` theo đúng thứ tự:

```bash
# Dùng sqlcmd (SQL Server CLI)
sqlcmd -S localhost -i database/01_create_database.sql -C
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/02_seed_data.sql -C
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/03_stored_procedures.sql -C
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/05_migration_financial_fix.sql -C
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/06_edge_case_fixes.sql -C
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/07_unique_lot_constraint.sql -C
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/08_phieunhap_giasi_fix.sql -C
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/09_nha_cung_cap.sql -C
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/09_seed_25_products.sql -C
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/10_index_sort_columns.sql -C
```

> **Hoặc:** Mở file trong SSMS (SQL Server Management Studio) và chạy từng file.

### Bước 3: Cấu hình kết nối database

Mở file `src/main/resources/application.properties` và sửa:

```properties
# ★ Sửa theo máy của bạn
db.url=jdbc:sqlserver://localhost:1433;databaseName=QuanLyCuaHangThuoc;encrypt=false;trustServerCertificate=true;
db.user=sa
db.password=123456    # ← Đổi thành password SQL Server của bạn
```

### Bước 4: Build & Chạy

```bash
# Build
mvn compile

# Chạy
mvn compile exec:java
```

> **Lần đầu:** Maven sẽ tải dependencies (~30s). Các lần sau chạy nhanh hơn.

---

## 🏗️ Kiến trúc tổng quan

Dự án theo **Clean Architecture 3 tầng** + **MVP Pattern** ở tầng UI:

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION (UI)                        │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │  View (Swing) │◄──│  Presenter   │───►│  IXxxView    │  │
│  │  ProductPanel │    │  (Logic+State)│   │  (Interface) │  │
│  └──────────────┘    └──────┬───────┘    └──────────────┘  │
│                             │                               │
├─────────────────────────────┼───────────────────────────────┤
│                    SERVICE LAYER                            │
│           ┌─────────────────▼────────────────┐              │
│           │     IProductService (Interface)    │              │
│           │     ProductServiceImpl             │              │
│           └─────────────────┬────────────────┘              │
│                             │                               │
├─────────────────────────────┼───────────────────────────────┤
│                 INFRASTRUCTURE (DAO)                        │
│           ┌─────────────────▼────────────────┐              │
│           │  IProductRepository (Interface)    │              │
│           │  ProductRepositoryImpl (SQL)        │              │
│           └─────────────────┬────────────────┘              │
│                             │                               │
├─────────────────────────────┼───────────────────────────────┤
│                    DATABASE                                │
│               ┌─────────────▼──────────┐                    │
│               │   SQL Server           │                    │
│               │   QuanLyCuaHangThuoc   │                    │
│               └────────────────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

**Dependency flow:** View → Presenter → Service → Repository → Database

**Quy tắc cốt lõi:**
- Tầng trên phụ thuộc tầng dưới qua **Interface** (không phụ thuộc Impl)
- `ServiceFactory.java` đóng vai trò **DI Container** — inject tất cả dependencies
- Tầng `presentation/` **KHÔNG BAO GIỜ** gọi trực tiếp Repository/DAO

---

## 📁 Cấu trúc thư mục

```
eProject-StoreBanThuoc/
│
├── database/                              # SQL Scripts
│   ├── 01_create_database.sql             # Schema gốc (8 bảng)
│   ├── 02_seed_data.sql                   # Data demo
│   └── ...                                # Migration files
│
├── src/main/
│   ├── java/
│   │   ├── App.java                       # ★ Entry point (main method)
│   │   │
│   │   ├── common/                        # Shared utilities
│   │   │   ├── AppColors.java             #   Design tokens (màu sắc)
│   │   │   ├── CurrencyFormatter.java     #   Format tiền tệ
│   │   │   ├── DateUtils.java             #   Format ngày tháng
│   │   │   ├── ServiceFactory.java        #   ★ DI Container
│   │   │   └── Session.java               #   Quản lý phiên đăng nhập
│   │   │
│   │   ├── domain/                        # Business Domain
│   │   │   ├── entity/                    #   8 Entity classes
│   │   │   │   ├── Product.java
│   │   │   │   ├── Batch.java
│   │   │   │   ├── Invoice.java
│   │   │   │   ├── InvoiceDetail.java
│   │   │   │   ├── Customer.java
│   │   │   │   ├── Supplier.java
│   │   │   │   ├── ImportReceipt.java
│   │   │   │   └── User.java
│   │   │   │
│   │   │   ├── dto/                       #   Data Transfer Objects
│   │   │   │   ├── CartItem.java
│   │   │   │   ├── RevenueDTO.java
│   │   │   │   ├── TopSellingDTO.java
│   │   │   │   └── TopCustomerDTO.java
│   │   │   │
│   │   │   └── repository/               #   ★ Repository Interfaces
│   │   │       ├── IProductRepository.java
│   │   │       ├── IBatchRepository.java
│   │   │       └── ... (8 interfaces)
│   │   │
│   │   ├── infrastructure/                # Technical Infrastructure
│   │   │   ├── database/
│   │   │   │   └── DatabaseHelper.java    #   HikariCP connection pool
│   │   │   │
│   │   │   └── repository/               #   ★ DAO Implementations
│   │   │       ├── ProductRepositoryImpl.java
│   │   │       ├── BatchRepositoryImpl.java
│   │   │       └── ... (8 implementations)
│   │   │
│   │   ├── service/                       # Business Logic
│   │   │   ├── IProductService.java       #   ★ Service Interfaces
│   │   │   ├── IBatchService.java
│   │   │   ├── ISaleService.java
│   │   │   ├── ... (8 interfaces)
│   │   │   │
│   │   │   └── impl/                      #   Service Implementations
│   │   │       ├── ProductServiceImpl.java
│   │   │       ├── SaleServiceImpl.java   #   (có FEFO logic)
│   │   │       └── ... (8 implementations)
│   │   │
│   │   └── presentation/                  # ★ UI Layer (MVP Pattern)
│   │       ├── LoginFrame.java            #   Màn hình đăng nhập
│   │       ├── MainFrame.java             #   Khung chính + sidebar
│   │       ├── ProductPanel.java          #   ★ View (Swing layout)
│   │       │
│   │       ├── view/                      #   View Interfaces
│   │       │   └── IProductView.java      #   ★ Giao kèo View-Presenter
│   │       │
│   │       ├── presenter/                 #   Presenters (Logic)
│   │       │   └── ProductPresenter.java  #   ★ Logic + State (0 Swing)
│   │       │
│   │       ├── panel/                     #   Placeholder panels
│   │       │   ├── DashboardPanel.java    #   TODO
│   │       │   ├── POSPanel.java          #   TODO
│   │       │   ├── ImportPanel.java       #   TODO
│   │       │   ├── InventoryPanel.java    #   TODO
│   │       │   └── CustomerPanel.java     #   TODO
│   │       │
│   │       ├── dialog/                    #   Dialog windows
│   │       └── component/                 #   Reusable UI components
│   │
│   └── resources/
│       └── application.properties         # ★ Database config
│
├── pom.xml                                # Maven dependencies
├── PROJECT_STATUS.md                      # Tiến độ dự án
└── PROJECT_GUIDE.md                       # ★ File này
```

---

## 🎯 MVP Pattern — Tách UI khỏi Logic

### Vấn đề cũ

Trước đây, `ProductPanel.java` 975 dòng chứa **cả UI + Logic + State** lẫn lộn.
→ Đổi UI = viết lại logic = rủi ro bug.

### Giải pháp: MVP (Model-View-Presenter)

| File | Import Swing? | Chứa gì | Sửa khi đổi UI? |
|------|:---:|---------|:---:|
| `IProductView.java` | ❌ | Interface — hợp đồng | ❌ Không sửa |
| `ProductPresenter.java` | ❌ | Logic + State + gọi Service | ❌ Không sửa |
| `ProductPanel.java` | ✅ | Swing layout + implement IProductView | ✅ **Chỉ sửa file này** |

### Flow hoạt động

```
User click "Thêm Mới"
  → ProductPanel (Button event)
    → presenter.doAdd()                    // delegate sang Presenter
      → view.getTenSP()                   // Presenter đọc data từ View
      → view.getDonViTinh()
      → view.getGiaBanText()
      → productService.insert(product)    // Presenter gọi Service
      → view.showInfo("Thành công!")      // Presenter ra lệnh View hiển thị
      → presenter.loadPage(currentPage)   // Presenter tự refresh
        → view.displayProducts(list)      // View chỉ hiển thị
```

### Khi đổi UI

```java
// 1. Tạo class mới
public class ProductPanelV2 extends JPanel implements IProductView {
    private final ProductPresenter presenter;

    public ProductPanelV2() {
        // 2. Tạo Presenter (logic giữ nguyên)
        presenter = new ProductPresenter(this, ServiceFactory.getProductService());

        // 3. Dựng UI mới tùy ý
        // ... Card layout, Dark mode, JavaFX, bất kỳ ...

        // 4. Wire events
        btnAdd.addActionListener(e -> presenter.doAdd());

        // 5. Load data
        presenter.loadPage(1);
    }

    // 6. Implement tất cả method trong IProductView
    @Override
    public void displayProducts(List<Product> products, Set<Integer> checkedIds) {
        // Hiển thị theo cách mới
    }
    // ... implement các method khác ...
}
```

**Xong!** Toàn bộ logic CRUD, phân trang, sort, bulk action giữ nguyên 100%.

---

## 📝 Quy ước code

### Đặt tên

| Loại | Quy tắc | Ví dụ |
|------|---------|-------|
| Entity | Danh từ tiếng Anh, PascalCase | `Product`, `Batch`, `Invoice` |
| Interface repo | `I` + tên entity + `Repository` | `IProductRepository` |
| Interface service | `I` + tên entity + `Service` | `IProductService` |
| Impl | Tên interface + `Impl` | `ProductRepositoryImpl` |
| View interface | `I` + tên module + `View` | `IProductView` |
| Presenter | Tên module + `Presenter` | `ProductPresenter` |
| Panel (UI) | Tên module + `Panel` | `ProductPanel` |

### Cột database → Entity field

| DB Column (tiếng Việt) | Java Field (camelCase) |
|------------------------|----------------------|
| `MaSP` | `maSP` |
| `TenSP` | `tenSP` |
| `GiaBan` | `giaBan` |
| `HanSuDung` | `hanSuDung` |
| `TrangThai` | `trangThai` |

### Packages

```
common/         → Utility dùng chung (không logic nghiệp vụ)
domain/         → Entity + Interface (KHÔNG import infrastructure)
infrastructure/ → DAO impl + Database access
service/        → Business logic
presentation/   → UI (Swing)
```

---

## 🗄️ Database

### Tên: `QuanLyCuaHangThuoc`

### 8 Bảng chính

| Bảng | Mô tả | Quan hệ |
|------|-------|---------|
| `NguoiDung` | Tài khoản đăng nhập | — |
| `SanPham` | Sản phẩm thuốc | — |
| `NhaCungCap` | Nhà cung cấp | — |
| `LoHang` | Lô hàng (số lô, HSD, tồn kho) | FK → SanPham |
| `PhieuNhap` | Phiếu nhập hàng | FK → NguoiDung, NhaCungCap |
| `KhachHang` | Khách hàng (SĐT unique) | — |
| `HoaDon` | Hóa đơn bán hàng | FK → KhachHang, NguoiDung |
| `ChiTietHoaDon` | Chi tiết hóa đơn | FK → HoaDon, LoHang, SanPham |

### ERD (simplified)

```
NguoiDung ──┐
            ├──► PhieuNhap ──► LoHang ──► SanPham
NhaCungCap ─┘                    │
                                 │
NguoiDung ──┐                    │
            ├──► HoaDon ──► ChiTietHoaDon
KhachHang ──┘              (FK → LoHang, SanPham)
```

### Views & Indexes

- `vw_TonKhoTheoSanPham` — Tổng tồn kho theo SP (SUM SoLuong GROUP BY MaSP)
- `IX_LoHang_FEFO` — Index cho query FEFO (First-Expire-First-Out)
- `IX_SanPham_TenSP`, `IX_SanPham_GiaBan`, ... — Index cho sort columns

---

## 🔨 Hướng dẫn phát triển module mới

Ví dụ: Phát triển module **Nhập Kho**

### Bước 1: Kiểm tra backend (thường đã có sẵn)

```
✅ domain/entity/ImportReceipt.java      — Entity
✅ domain/entity/Batch.java              — Entity
✅ domain/repository/IBatchRepository.java — Interface
✅ infrastructure/repository/BatchRepositoryImpl.java — DAO
✅ service/IBatchService.java             — Interface
✅ service/impl/BatchServiceImpl.java     — Logic
✅ common/ServiceFactory.getBatchService() — DI
```

> Nếu thiếu method nào → bổ sung vào Interface + Impl.

### Bước 2: Tạo View Interface

```java
// presentation/view/IImportView.java
public interface IImportView {
    // Đọc data từ form
    int getSelectedProductId();
    String getSoLo();
    // ...

    // Hiển thị
    void displayBatches(List<Batch> batches);
    void showInfo(String msg);
    // ...
}
```

### Bước 3: Tạo Presenter

```java
// presentation/presenter/ImportPresenter.java
public class ImportPresenter {
    private final IImportView view;
    private final IBatchService batchService;

    public ImportPresenter(IImportView view, IBatchService batchService) {
        this.view = view;
        this.batchService = batchService;
    }

    public void doImport() {
        // Logic nhập kho — KHÔNG import Swing
    }
}
```

### Bước 4: Tạo Panel (implements IImportView)

```java
// presentation/ImportPanel.java
public class ImportPanel extends JPanel implements IImportView {
    private final ImportPresenter presenter;

    public ImportPanel() {
        presenter = new ImportPresenter(this, ServiceFactory.getBatchService());
        initComponents(); // Swing layout
        // Wire events → presenter.doXxx()
    }

    // implement IImportView methods
}
```

### Bước 5: Đăng ký vào MainFrame

```java
// MainFrame.java — thêm panel vào CardLayout
contentPanel.add(new ImportPanel(), "import");
```

---

## 🔑 Tài khoản demo

| Username | Password | Vai trò | Quyền |
|----------|----------|---------|-------|
| `admin` | `admin123` | Admin | Toàn quyền |
| `nv1` | `123456` | Nhân viên | Chỉ Bán hàng + Xem SP |

### Phân quyền

| Chức năng | Admin | Nhân viên |
|-----------|:-----:|:---------:|
| Dashboard | ✅ | ❌ |
| Sản Phẩm (CRUD) | ✅ | Chỉ xem |
| Nhập Kho | ✅ | ❌ |
| Tồn Kho | ✅ | ❌ |
| Bán Hàng (POS) | ✅ | ✅ |
| Khách Hàng | ✅ | ✅ |

---

## ⌨️ Lệnh thường dùng

```bash
# Build (compile)
mvn compile

# Build + Chạy
mvn compile exec:java

# Clean build
mvn clean compile

# Chạy test (nếu có)
mvn test

# Package thành JAR
mvn package

# Chạy file SQL
sqlcmd -S localhost -d QuanLyCuaHangThuoc -i database/xxx.sql -C
```

---

## 🔧 Troubleshooting

### Lỗi kết nối database

```
Communication link failure / Connection refused
```

**Giải pháp:**
1. Kiểm tra SQL Server đang chạy: `Windows Services > SQL Server (MSSQLSERVER)`
2. Kiểm tra port 1433: `netstat -an | findstr 1433`
3. Sửa `db.url` trong `application.properties` cho đúng server name
4. Nếu dùng Named Instance: `db.url=jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=...`

### Lỗi tiếng Việt bị lỗi font

```
Tên SP hiện ra ký tự lạ: "Thu?c c?m"
```

**Giải pháp:** Khi INSERT data vào SQL Server, luôn dùng `N'...'`:

```sql
INSERT INTO SanPham (TenSP, DonViTinh) VALUES (N'Thuốc cảm', N'Hộp');
```

### Lỗi sort: "A column has been specified more than once"

**Nguyên nhân:** Tie-breaker `MaSP ASC` bị trùng khi sort theo chính MaSP.  
**Đã fix:** `ProductRepositoryImpl.mapSortColumn()` kiểm tra trùng trước khi append.

### Maven build lỗi

```bash
# Xóa cache + build lại
mvn clean compile -U
```

---

## 📦 Dependencies (pom.xml)

| Library | Version | Mục đích |
|---------|:-------:|---------|
| `mssql-jdbc` | 12.8.1 | SQL Server JDBC driver |
| `HikariCP` | 5.1.0 | Connection pooling |
| `FlatLaf` | 3.2.5 | Modern Look & Feel |
| `JFreeChart` | 1.5.3 | Biểu đồ Dashboard (TODO) |
