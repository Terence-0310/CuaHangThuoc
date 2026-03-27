# 📘 Hướng Dẫn Kiến Trúc Dự Án — Apothecary Pro (eProject_StoreBanThuoc)

> **Phiên bản**: 1.0.0 &nbsp;|&nbsp; **Cập nhật**: 27/03/2026  
> **Công nghệ**: Java 17, Swing (FlatLaf), SQL Server, Maven, HikariCP  
> **Kiến trúc**: Clean Architecture + MVP (Model-View-Presenter)

---

## 1. Tổng Quan Cây Thư Mục

```
eProject-StoreBanThuoc/
├── pom.xml                              ← Maven build config (Java 17)
├── database/                            ← SQL scripts (schema + seed + migration)
│   ├── 01_create_database.sql            ← DDL: tạo database & tables
│   ├── 02_seed_data.sql                  ← Dữ liệu mẫu
│   ├── 03_stored_procedures.sql          ← Stored Procedures
│   ├── 04_verify_data.sql                ← Script kiểm tra dữ liệu
│   ├── 05–12_*.sql                       ← Migration scripts (theo thứ tự)
│   └── seed_temp.sql                     ← Dữ liệu test tạm
│
└── src/main/
    ├── resources/
    │   └── application.properties        ← ★ Cấu hình DB, pool, business rules
    │
    └── java/
        ├── App.java                      ← ★ ENTRY POINT: khởi tạo FlatLaf → LoginFrame
        │
        ├── common/                       ← Utilities & Cross-cutting concerns
        │   ├── AppColors.java             ← Bảng màu Design System (Apothecary Pro)
        │   ├── CurrencyFormatter.java     ← Format tiền VNĐ (#,### ₫)
        │   ├── DateUtils.java             ← Utility xử lý ngày (FEFO check)
        │   ├── DatePickerField.java       ← UI Component: Calendar popup
        │   ├── DocumentPrinter.java       ← In phiếu trả hàng / hủy hàng (Graphics2D)
        │   ├── ServiceFactory.java        ← ★ DI Container (Dependency Injection)
        │   ├── Session.java               ← ★ Quản lý phiên đăng nhập + online status
        │   └── SystemLogger.java          ← Audit log (ghi nhật ký hệ thống vào DB)
        │
        ├── domain/                        ← ★ TẦNG LÕI — Entity, DTO, Repository Interface
        │   ├── entity/                    ← Business Entities (POJO)
        │   │   ├── User.java              ← Người dùng (Admin/Nhân viên)
        │   │   ├── Product.java           ← Sản phẩm (thuốc)
        │   │   ├── Batch.java             ← Lô hàng (hạn sử dụng, FEFO)
        │   │   ├── Customer.java          ← Khách hàng
        │   │   ├── Supplier.java          ← Nhà cung cấp
        │   │   ├── Invoice.java           ← Hóa đơn bán hàng
        │   │   ├── InvoiceDetail.java     ← Chi tiết hóa đơn
        │   │   └── ImportReceipt.java     ← Phiếu nhập kho
        │   │
        │   ├── dto/                       ← Data Transfer Objects
        │   │   ├── CartItem.java           ← Mặt hàng trong giỏ POS
        │   │   ├── HeldOrder.java          ← Đơn hàng tạm giữ
        │   │   ├── ImportCartItem.java     ← Mặt hàng trong giỏ nhập kho
        │   │   ├── PagedResult.java        ← Generic: kết quả phân trang
        │   │   ├── InvoiceFilterCriteria.java ← Filter tìm kiếm hóa đơn
        │   │   ├── InvoiceListDTO.java     ← DTO hiển thị bảng hóa đơn
        │   │   ├── InvoiceDetailDTO.java   ← DTO chi tiết hóa đơn
        │   │   ├── InventoryBatchDTO.java  ← DTO hiển thị kho (batch + sản phẩm)
        │   │   ├── UserListDTO.java        ← DTO danh sách người dùng
        │   │   ├── CustomerPurchaseHistoryDTO.java ← Lịch sử mua hàng KH
        │   │   ├── RevenueDTO.java         ← Doanh thu theo ngày/tháng/quý
        │   │   ├── BusinessMetricDTO.java  ← KPI tổng quan
        │   │   ├── TopSellingDTO.java      ← Top sản phẩm bán chạy
        │   │   ├── TopCustomerDTO.java     ← Top khách hàng VIP
        │   │   ├── TopSupplierDTO.java     ← Top nhà cung cấp
        │   │   └── StockAlertDTO.java      ← Cảnh báo tồn kho
        │   │
        │   └── repository/                ← ★ Repository INTERFACES (Dependency Inversion)
        │       ├── IUserRepository.java
        │       ├── IProductRepository.java
        │       ├── IBatchRepository.java
        │       ├── ICustomerRepository.java
        │       ├── ISupplierRepository.java
        │       ├── IInvoiceRepository.java
        │       ├── IInvoiceDetailRepository.java
        │       ├── IInventoryRepository.java
        │       └── IReportRepository.java
        │
        ├── infrastructure/                ← TẦNG HẠ TẦNG — Database & Repository Impl
        │   ├── database/
        │   │   └── DatabaseHelper.java    ← ★ HikariCP connection pool
        │   │
        │   └── repository/                ← ★ Repository IMPLEMENTATIONS
        │       ├── UserRepositoryImpl.java
        │       ├── ProductRepositoryImpl.java
        │       ├── BatchRepositoryImpl.java
        │       ├── CustomerRepositoryImpl.java
        │       ├── SupplierRepositoryImpl.java
        │       ├── InvoiceRepositoryImpl.java
        │       ├── InvoiceDetailRepositoryImpl.java
        │       ├── InventoryRepositoryImpl.java
        │       ├── ReportRepositoryImpl.java
        │       └── NhapKhoDAO.java         ← Legacy DAO (nhập kho)
        │
        ├── service/                       ← TẦNG NGHIỆP VỤ — Business Logic
        │   ├── IAuthService.java           ← Đăng nhập & phân quyền
        │   ├── ISaleService.java           ← ★ Bán hàng POS (FEFO logic)
        │   ├── IProductService.java        ← CRUD sản phẩm
        │   ├── IBatchService.java          ← Quản lý lô hàng (trả/hủy)
        │   ├── ICustomerService.java       ← CRUD khách hàng
        │   ├── ISupplierService.java       ← CRUD nhà cung cấp
        │   ├── IInvoiceService.java        ← Xem & quản lý hóa đơn
        │   ├── IInventoryService.java      ← Quản lý kho
        │   ├── IImportService.java         ← Nhập kho
        │   ├── IUserService.java           ← CRUD người dùng
        │   ├── IReportService.java         ← Báo cáo thống kê
        │   ├── InvoicePdfService.java      ← Xuất PDF hóa đơn (OpenPDF)
        │   │
        │   └── impl/                      ← Service Implementations
        │       ├── AuthServiceImpl.java
        │       ├── SaleServiceImpl.java    ← ★ Core: FEFO checkout logic
        │       ├── ProductServiceImpl.java
        │       ├── BatchServiceImpl.java   ← ★ Trả hàng NCC / Hủy hàng (transaction)
        │       ├── CustomerServiceImpl.java
        │       ├── SupplierServiceImpl.java
        │       ├── InvoiceServiceImpl.java
        │       ├── InventoryServiceImpl.java
        │       ├── ImportServiceImpl.java
        │       ├── UserServiceImpl.java
        │       └── ReportServiceImpl.java
        │
        └── presentation/                  ← TẦNG GIAO DIỆN — Swing UI (MVP Pattern)
            ├── LoginFrame.java            ← ★ Màn hình đăng nhập
            ├── MainFrame.java             ← ★ Frame chính (sidebar + CardLayout)
            ├── ProductPanel.java          ← Panel sản phẩm (legacy, vẫn hoạt động)
            │
            ├── panel/                     ← Các Panel chính
            │   ├── DashboardPanel.java     ← Tổng quan doanh thu (alias cho ReportPanel)
            │   ├── POSPanel.java           ← ★ Bán hàng POS
            │   ├── ProductPanel.java       ← Quản lý sản phẩm
            │   ├── InventoryPanel.java     ← Quản lý kho
            │   ├── ImportPanel.java        ← Nhập kho
            │   ├── SupplierPanel.java      ← Quản lý NCC
            │   ├── CustomerPanel.java      ← Quản lý khách hàng
            │   ├── InvoicePanel.java       ← Lịch sử hóa đơn
            │   ├── ReportPanel.java        ← ★ Dashboard báo cáo
            │   └── UserManagementPanel.java ← Quản lý người dùng (Admin)
            │
            ├── dialog/                    ← Popup Dialogs
            │   ├── ProductDetailDialog.java ← Xem chi tiết sản phẩm
            │   ├── ProductFormDialog.java   ← Thêm/sửa sản phẩm
            │   ├── SupplierDetailDialog.java ← Chi tiết NCC (tab: thông tin / phiếu nhập / lô)
            │   ├── InvoiceDetailDialog.java ← Chi tiết hóa đơn
            │   ├── ReturnInvoiceDialog.java ← ★ Trả hàng khách
            │   └── BatchActionDialog.java   ← ★ Trả hàng NCC / Hủy hàng
            │
            ├── presenter/                 ← ★ Presenters (MVP: điều phối View ↔ Service)
            │   ├── POSPresenter.java
            │   ├── ProductPresenter.java
            │   ├── InventoryPresenter.java
            │   ├── ImportPresenter.java
            │   ├── SupplierPresenter.java
            │   ├── CustomerPresenter.java
            │   ├── InvoicePresenter.java
            │   ├── ReportPresenter.java
            │   └── UserManagementPresenter.java
            │
            ├── view/                      ← View Interfaces (MVP contract)
            │   ├── IPOSView.java
            │   ├── IProductView.java
            │   ├── IInventoryManagementView.java
            │   ├── IImportView.java
            │   ├── ISupplierManagementView.java
            │   ├── ICustomerManagementView.java
            │   ├── IInvoiceView.java
            │   ├── IReportView.java
            │   └── IUserManagementView.java
            │
            └── component/                 ← Reusable UI Components
                ├── RevenueCard.java        ← Card hiển thị doanh thu trên Dashboard
                ├── SearchBar.java          ← Thanh tìm kiếm dùng chung
                └── StyledTable.java        ← Placeholder: JTable tùy chỉnh
```

---

## 2. Phân Tầng Clean Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│  LoginFrame → MainFrame → Panel/Dialog → Presenter      │
│  (Swing UI, chỉ gọi Service Interface, KHÔNG gọi DB)   │
├─────────────────────────────────────────────────────────┤
│                   ← View Interface (IPOSView...) →      │
├─────────────────────────────────────────────────────────┤
│                     SERVICE LAYER                        │
│  IAuthService, ISaleService, IProductService...          │
│  Impl: AuthServiceImpl, SaleServiceImpl...               │
│  (Business logic, validation, transaction management)    │
├─────────────────────────────────────────────────────────┤
│                   ← Repository Interface →               │
├─────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER                        │
│  Entity: User, Product, Batch, Invoice, Customer...      │
│  DTO: CartItem, PagedResult, InvoiceListDTO...           │
│  Interface: IUserRepository, IProductRepository...       │
│  (Lớp lõi, KHÔNG phụ thuộc bất kỳ lớp nào khác)        │
├─────────────────────────────────────────────────────────┤
│                  INFRASTRUCTURE LAYER                    │
│  DatabaseHelper (HikariCP), *RepositoryImpl              │
│  (JDBC, SQL Server, CHỈ implement domain interfaces)    │
└─────────────────────────────────────────────────────────┘
```

### Quy tắc phụ thuộc (Dependency Rule)

| Từ (Caller)              | Tới (Callee)                   | Tuân thủ? |
|--------------------------|--------------------------------|:---------:|
| `presentation.panel.*`   | `service.I*Service`            | ✅         |
| `presentation.presenter` | `service.I*Service`            | ✅         |
| `presentation.presenter` | `presentation.view.I*View`     | ✅         |
| `service.impl.*`         | `domain.repository.I*Repo`     | ✅         |
| `service.impl.*`         | `domain.entity.*`              | ✅         |
| `infrastructure.repo.*`  | `domain.repository.I*Repo`     | ✅         |
| `common.ServiceFactory`  | Wires tất cả (DI Container)   | ✅         |

> **Quan trọng**: `presentation` **KHÔNG BAO GIỜ** import trực tiếp `infrastructure.*`. Mọi truy cập DB đều đi qua `ServiceFactory` → `IService` → `IRepository` → `RepositoryImpl`.

---

## 3. Luồng Khởi Động Ứng Dụng

```
App.java:main()
  │
  ├── 1. UIManager.setLookAndFeel(FlatLightLaf)
  │       → Set font "Segoe UI", border radius, scrollbar style
  │
  └── 2. SwingUtilities.invokeLater()
          └── new LoginFrame().setVisible(true)
                │
                ├── ServiceFactory.getAuthService() → AuthServiceImpl(IUserRepository)
                │
                └── doLogin()
                    ├── authService.login(username, password)
                    │   └── IUserRepository.findByCredentials() → SQL Server
                    │
                    ├── Session.setCurrentUser(user) → lưu user vào static field
                    ├── Session.goOnline()            → UPDATE NguoiDung SET DangOnline=1
                    │
                    └── new MainFrame().setVisible(true)
                        │
                        ├── Tạo sidebar buttons (Dashboard, POS, Import, QUẢN LÝ, QUẢN TRỊ)
                        ├── Tạo 9 panels qua CardLayout:
                        │   ├── ReportPanel   ("dashboard")
                        │   ├── POSPanel      ("pos")
                        │   ├── ProductPanel  ("product")
                        │   ├── ImportPanel   ("import")
                        │   ├── InventoryPanel("inventory")
                        │   ├── SupplierPanel ("supplier")
                        │   ├── CustomerPanel ("customer")
                        │   ├── InvoicePanel  ("invoice")
                        │   └── UserMgmtPanel ("usermgmt")
                        │
                        └── applyPermissions()
                            └── Admin → mở tất cả
                                NV    → ẩn Dashboard, Import, NCC, Kho, Hóa đơn, Quản trị
```

---

## 4. Truy Vết Luồng Nghiệp Vụ — Bán Hàng POS (FEFO)

Đây là luồng nghiệp vụ quan trọng nhất, áp dụng **FEFO (First-Expire, First-Out)**.

```
★ User click "Thanh Toán" trên POSPanel
│
├── 1. POSPanel (View) → gọi POSPresenter.processCheckout()
│   File: presentation/panel/POSPanel.java
│   File: presentation/presenter/POSPresenter.java
│   ● POSPanel implements IPOSView
│   ● Presenter nhận IPOSView + ISaleService qua constructor
│   ● processCheckout() chạy trên SwingWorker (background thread)
│
├── 2. POSPresenter → gọi ISaleService.checkout(cart, phone, name, payMethod)
│   File: service/ISaleService.java (interface)
│   File: service/impl/SaleServiceImpl.java (implementation)
│   ● SaleServiceImpl nhận: IInvoiceRepo, IInvoiceDetailRepo, IBatchRepo, ICustomerRepo
│   ● ĐI QUA ServiceFactory: ServiceFactory.getSaleService()
│
├── 3. SaleServiceImpl.checkout() — ★ CORE BUSINESS LOGIC
│   │
│   ├── 3a. conn.setAutoCommit(false)  ← BẮT ĐẦU TRANSACTION
│   │
│   ├── 3b. customerRepo.findByPhone(conn, soDT)
│   │        → Nếu không có → customerRepo.insert(conn, customer)
│   │        File: domain/repository/ICustomerRepository.java
│   │        File: infrastructure/repository/CustomerRepositoryImpl.java
│   │
│   ├── 3c. invoiceRepo.insert(conn, invoice) → MaHD
│   │        File: domain/repository/IInvoiceRepository.java
│   │        File: infrastructure/repository/InvoiceRepositoryImpl.java
│   │
│   ├── 3d. Với MỖI CartItem trong giỏ hàng:
│   │   │
│   │   ├── batchRepo.getBatchesForSale(conn, maSP)  ← FEFO query
│   │   │   SQL: SELECT ... WHERE SoLuong > 0 AND HanSuDung > GETDATE()
│   │   │        ORDER BY HanSuDung ASC  ← cái nào hết hạn sớm → bán trước
│   │   │   File: infrastructure/repository/BatchRepositoryImpl.java
│   │   │
│   │   ├── Vòng lặp FEFO: tự động tách dòng theo lô
│   │   │   VD: Mua 10 Panadol → Lô A (HSD 01/04, còn 7) + Lô B (HSD 15/05, 3)
│   │   │
│   │   ├── batchRepo.deductStock(conn, maLo, soLuong)  ← trừ tồn kho
│   │   │   SQL: UPDATE LoHang SET SoLuong = SoLuong - ? WHERE MaLo = ?
│   │   │        AND SoLuong >= ?  ← UPDLOCK chống Race Condition
│   │   │
│   │   └── detailRepo.insert(conn, invoiceDetail)  ← ghi chi tiết hóa đơn
│   │       File: infrastructure/repository/InvoiceDetailRepositoryImpl.java
│   │
│   ├── 3e. invoiceRepo.updateTotal(conn, maHD)  ← tính lại tổng tiền
│   │
│   └── 3f. conn.commit()  ← KẾT THÚC TRANSACTION
│            (Nếu lỗi → conn.rollback())
│
├── 4. POSPresenter nhận maHD → gọi view.showCheckoutSuccess()
│   ● Hiện dialog "Thanh toán thành công! Mã HĐ: xxx"
│   ● Tùy chọn: In PDF hóa đơn qua InvoicePdfService
│   │
│   └── File: service/InvoicePdfService.java (OpenPDF, zero UI dependency)
│
└── 5. POSPanel reset giao diện: clearCart(), refreshProductTable()
```

### Luồng phụ thuộc thực tế:

```
POSPanel ──implements──→ IPOSView
    │
    └── creates ──→ POSPresenter(this, ServiceFactory.getSaleService())
                         │                         │
                         │              ┌───────────┘
                         ▼              ▼
                    IPOSView      ISaleService
                                       │
                                       ▼
                              SaleServiceImpl
                          ┌────────┼────────┐────────┐
                          ▼        ▼        ▼        ▼
                   IInvoiceRepo  IDetailRepo IBatchRepo ICustomerRepo
                          │        │        │        │
                          ▼        ▼        ▼        ▼
                   InvoiceRepoImpl DetailRepoImpl BatchRepoImpl CustomerRepoImpl
                          │        │        │        │
                          └────────┴────────┴────────┘
                                       │
                                       ▼
                              DatabaseHelper.getConnection()
                                       │
                                       ▼
                              SQL Server (HikariCP pool)
```

---

## 5. ServiceFactory — DI Container

File: `common/ServiceFactory.java`

Đây là trái tim của hệ thống Dependency Injection. **Mọi Service đều được lấy từ đây**.

```java
// Cách sử dụng trong UI:
IAuthService     auth   = ServiceFactory.getAuthService();
ISaleService     sale   = ServiceFactory.getSaleService();
IProductService  prod   = ServiceFactory.getProductService();
IBatchService    batch  = ServiceFactory.getBatchService();
ICustomerService cust   = ServiceFactory.getCustomerService();
IInvoiceService  inv    = ServiceFactory.getInvoiceService();
IReportService   report = ServiceFactory.getReportService();
ISupplierService supp   = ServiceFactory.getSupplierService();
IUserService     user   = ServiceFactory.getUserService();
```

**Nguyên tắc**: 
- Repository singletons được tạo 1 lần duy nhất (`private static final`)
- Service được tạo mới mỗi lần gọi `getXxxService()` (inject repo qua constructor)
- UI **KHÔNG BAO GIỜ** `new` trực tiếp `XxxServiceImpl`

---

## 6. Phân Quyền (Authorization)

File: `common/Session.java` + `presentation/MainFrame.java`

| Vai trò | Quyền truy cập |
|---------|----------------|
| **Admin** | Tất cả: Dashboard, POS, Nhập kho, Sản phẩm, Kho, NCC, KH, Hóa đơn, Quản trị người dùng |
| **Nhân viên** | POS, Sản phẩm (xem), Khách hàng |

Xử lý tại `MainFrame.applyPermissions()`:
- Admin → hiện tất cả menu sidebar
- Nhân viên → ẩn: Dashboard, Nhập kho, NCC, Kho, Hóa đơn, Quản trị
- Default view: Admin → Dashboard, NV → POS

---

## 7. Cơ Sở Dữ Liệu

### 7.1 Cấu hình kết nối

File: `src/main/resources/application.properties`

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=QuanLyCuaHangThuoc;...
db.user=sa
db.password=123456
db.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
db.pool.maximumPoolSize=10
```

### 7.2 Connection Pool

File: `infrastructure/database/DatabaseHelper.java`

- Sử dụng **HikariCP** (hiệu suất cao nhất trong Java)
- Static initializer: đọc `application.properties` → tạo `HikariDataSource`
- API: `DatabaseHelper.getConnection()` → trả `Connection` từ pool
- Shutdown: `DatabaseHelper.close()` (gọi khi app tắt)

### 7.3 SQL Migration Order

```
01_create_database.sql     ← Tạo DB, bảng chính (NguoiDung, SanPham, LoHang, KhachHang, HoaDon...)
02_seed_data.sql           ← Dữ liệu mẫu (admin, nhân viên, sản phẩm)
03_stored_procedures.sql   ← SP: sp_TraHangKhach (trả hàng khách)
04–12_*.sql                ← Migration lần lượt (ALTER, INDEX, constraints)
```

### 7.4 Bảng dữ liệu chính

| Bảng | Mô tả | Quan hệ |
|------|--------|---------|
| `NguoiDung` | User hệ thống | — |
| `SanPham` | Sản phẩm (thuốc) | — |
| `LoHang` | Lô hàng (FEFO core) | → SanPham, PhieuNhap |
| `NhaCungCap` | Nhà cung cấp | — |
| `PhieuNhap` | Phiếu nhập kho | → NhaCungCap, NguoiDung |
| `KhachHang` | Khách hàng | — |
| `HoaDon` | Hóa đơn bán | → KhachHang, NguoiDung |
| `ChiTietHoaDon` | Chi tiết HĐ | → HoaDon, SanPham, LoHang |
| `SystemLogs` | Audit log | Auto-create bởi SystemLogger |

---

## 8. MVP Pattern (Model-View-Presenter)

Mỗi module tuân theo pattern:

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   IXxxView   │ ←── │ XxxPresenter │ ──→ │  IXxxService │
│  (interface) │     │  (mediator)  │     │  (interface) │
└──────┬───────┘     └──────────────┘     └──────────────┘
       │ implements
┌──────┴───────┐
│  XxxPanel    │
│ (Swing UI)   │
└──────────────┘
```

### Ví dụ module POS:

| Layer | File | Vai trò |
|-------|------|---------|
| View Interface | `view/IPOSView.java` | Contract: displayProducts(), showCheckoutSuccess()... |
| View (Panel) | `panel/POSPanel.java` | UI Swing, implements IPOSView |
| Presenter | `presenter/POSPresenter.java` | Điều phối: nhận event từ View → gọi Service → cập nhật View |
| Service Interface | `service/ISaleService.java` | Contract: checkout(), searchProducts()... |
| Service Impl | `service/impl/SaleServiceImpl.java` | Business logic: FEFO, transaction |

### Danh sách đầy đủ các module MVP:

| Module | View | Presenter | Service |
|--------|------|-----------|---------|
| POS | `IPOSView` → `POSPanel` | `POSPresenter` | `ISaleService` |
| Sản phẩm | `IProductView` → `ProductPanel` | `ProductPresenter` | `IProductService` |
| Kho | `IInventoryManagementView` → `InventoryPanel` | `InventoryPresenter` | `IInventoryService` |
| Nhập kho | `IImportView` → `ImportPanel` | `ImportPresenter` | `IImportService` |
| NCC | `ISupplierManagementView` → `SupplierPanel` | `SupplierPresenter` | `ISupplierService` |
| Khách hàng | `ICustomerManagementView` → `CustomerPanel` | `CustomerPresenter` | `ICustomerService` |
| Hóa đơn | `IInvoiceView` → `InvoicePanel` | `InvoicePresenter` | `IInvoiceService` |
| Báo cáo | `IReportView` → `ReportPanel` | `ReportPresenter` | `IReportService` |
| QL Người dùng | `IUserManagementView` → `UserManagementPanel` | `UserManagementPresenter` | `IUserService` |

---

## 9. Design System — Apothecary Pro

File: `common/AppColors.java`

| Token | Hex | Sử dụng |
|-------|-----|---------|
| `PRIMARY` | `#0056B3` | Nút chính, header bảng, sidebar active |
| `PRIMARY_DARK` | `#003D80` | Hover state |
| `SIDEBAR_BG` | `#1B2A4A` | Nền sidebar (dark navy) |
| `SIDEBAR_TEXT` | `#BBC7DB` | Text sidebar |
| `NEUTRAL` | `#F8F9FA` | Nền chính của app |
| `SUCCESS` | `#28A745` | Trạng thái thành công |
| `DANGER` | `#D32F2F` | Cảnh báo, nút xóa |
| `TEXT_PRIMARY` | `#212529` | Text chính |
| `TEXT_SECONDARY` | `#6C757D` | Text phụ |
| `TABLE_ROW_ALT` | `#F0F4F8` | Dòng chẵn trong bảng |

---

## 10. Hướng Dẫn Cho Developer Mới

### 10.1 Yêu Cầu Hệ Thống

| Công cụ | Phiên bản |
|---------|-----------|
| JDK | 17+ |
| Maven | 3.8+ |
| SQL Server | 2019+ (hoặc Express) |
| IDE | IntelliJ IDEA / Eclipse / VS Code |

### 10.2 Clone & Chạy

```bash
# 1. Clone
git clone https://github.com/BaoVuong150/eProject-StoreBanThuoc.git
cd eProject-StoreBanThuoc

# 2. Cài đặt Database
#    Mở SQL Server Management Studio (SSMS)
#    Chạy lần lượt các file trong thư mục database/:
#    01_create_database.sql → 02_seed_data.sql → 03_stored_procedures.sql → 04 đến 12

# 3. Cấu hình kết nối
#    Mở src/main/resources/application.properties
#    Sửa db.url, db.user, db.password cho phù hợp máy local

# 4. Build & Run
mvn clean compile exec:java
```

### 10.3 Tài Khoản Mặc Định

| Username | Password | Vai trò |
|----------|----------|---------|
| `admin` | `admin123` | Admin |
| `nhanvien01` | `nv123` | Nhân viên |

### 10.4 Core Files — Đọc Đầu Tiên

Một developer mới **bắt buộc** phải đọc các file sau theo thứ tự:

```
1. application.properties          ← Hiểu cấu hình DB & business rules
2. App.java                        ← Entry point, hiểu luồng khởi tạo
3. common/ServiceFactory.java      ← DI Container, hiểu cách wire dependencies
4. common/Session.java             ← Quản lý phiên đăng nhập
5. common/AppColors.java           ← Design system
6. infrastructure/database/DatabaseHelper.java  ← Cách kết nối DB
7. domain/entity/                  ← Đọc tất cả entity để hiểu data model
8. domain/repository/              ← Đọc interfaces để hiểu contract
9. service/ISaleService.java       ← Core business: bán hàng
10. service/impl/SaleServiceImpl.java ← FEFO logic (nghiệp vụ quan trọng nhất)
```

### 10.5 Thêm Module Mới

Khi cần thêm một module mới (ví dụ: "Quản lý kê đơn"):

```
Bước 1: Tạo Entity
  → domain/entity/Prescription.java

Bước 2: Tạo Repository Interface
  → domain/repository/IPrescriptionRepository.java

Bước 3: Implement Repository
  → infrastructure/repository/PrescriptionRepositoryImpl.java

Bước 4: Tạo Service Interface
  → service/IPrescriptionService.java

Bước 5: Implement Service
  → service/impl/PrescriptionServiceImpl.java

Bước 6: Đăng ký vào ServiceFactory
  → common/ServiceFactory.java
     + private static final IPrescriptionRepository prescRepo = new PrescriptionRepositoryImpl();
     + public static IPrescriptionService getPrescriptionService() { return new PrescriptionServiceImpl(prescRepo); }

Bước 7: Tạo View Interface
  → presentation/view/IPrescriptionView.java

Bước 8: Tạo Presenter
  → presentation/presenter/PrescriptionPresenter.java

Bước 9: Tạo Panel
  → presentation/panel/PrescriptionPanel.java

Bước 10: Đăng ký vào MainFrame
  → contentPanel.add(new PrescriptionPanel(), "prescription");
  → Thêm button vào sidebar
```

---

## 11. Thống Kê Dự Án

| Metric | Giá trị |
|--------|---------|
| **Tổng file Java** | 112 |
| **Entity** | 8 |
| **DTO** | 16 |
| **Repository Interface** | 9 |
| **Repository Impl** | 10 |
| **Service Interface** | 12 |
| **Service Impl** | 11 |
| **Presenter** | 9 |
| **View Interface** | 9 |
| **Panel** | 10 |
| **Dialog** | 6 |
| **Utility (common)** | 8 |
| **SQL Migration** | 18 files |
| **Dependencies** | 7 (MSSQL, HikariCP, FlatLaf, JFreeChart, OpenPDF, JCalendar, SLF4J) |

---

## 12. Dependencies (pom.xml)

| Library | Version | Mục đích |
|---------|---------|----------|
| `mssql-jdbc` | 12.8.1 | Kết nối SQL Server |
| `HikariCP` | 5.1.0 | Connection pooling (hiệu suất cao) |
| `FlatLaf` | 3.2.5 | Modern Look & Feel cho Swing |
| `FlatLaf Extras` | 3.2.5 | Component mở rộng |
| `JFreeChart` | 1.5.3 | Biểu đồ trên Dashboard (báo cáo) |
| `OpenPDF` | 1.3.30 | Xuất PDF hóa đơn |
| `JCalendar` | 1.4 | Date picker (legacy, đã custom DatePickerField) |
| `SLF4J Simple` | 2.0.9 | Logger (HikariCP yêu cầu) |

---

> **Ghi chú**: Tài liệu này được tạo tự động dựa trên **112 file Java** thực tế trong source code. Mọi tên file, package, và luồng phụ thuộc đều được xác minh qua deep scan.
