# eProject Store Bán Thuốc - Tổng quan dự án

Hệ thống quản lý cửa hàng thuốc được xây dựng bằng Java Swing + SQL Server, tập trung vào kiến trúc tách lớp rõ ràng để dễ bảo trì và mở rộng theo từng module.

## 1) Mục tiêu dự án

- Quản lý nghiệp vụ cửa hàng thuốc theo luồng: nhập kho -> tồn kho -> bán hàng -> hóa đơn -> báo cáo.
- Ưu tiên tính ổn định dữ liệu và an toàn giao dịch (transaction, rollback, khóa dòng dữ liệu khi bán).
- Tách riêng UI và business logic để có thể thay đổi giao diện mà không ảnh hưởng logic.

## 2) Công nghệ và phụ thuộc chính

- Java 17, Maven.
- SQL Server (JDBC `mssql-jdbc`).
- HikariCP cho connection pool.
- FlatLaf cho giao diện Swing hiện đại.
- JFreeChart (sẵn sàng cho dashboard/biểu đồ).

File cấu hình tổng quan:
- `pom.xml`
- `src/main/resources/application.properties`

## 3) Kiến trúc mã nguồn

Dự án theo hướng 4 lớp chính:

- `presentation`: giao diện Swing (Frame/Panel/Dialog), presenter, view interface.
- `service`: business logic thông qua service interface + implementation.
- `infrastructure`: repository implementation + truy cập SQL.
- `domain`: entity, dto, repository interface.

Một số package dùng chung:
- `common`: `Session`, `ServiceFactory`, màu sắc UI và utility.
- `infrastructure/database`: `DatabaseHelper` (HikariCP).

Luồng phụ thuộc:

`View (UI) -> Presenter -> Service -> Repository -> Database`

## 4) Phong cách code (Code Style + Design Style)

### 4.1 Nguyên tắc thiết kế được áp dụng

- DIP: lớp trên làm việc qua interface (`IProductService`, `IProductRepository`...).
- SRP: mỗi class tập trung một trách nhiệm (ví dụ `AuthServiceImpl` chỉ xử lý luồng đăng nhập).
- Constructor Injection: service nhận repository qua constructor.
- "Factory DI đơn giản": `ServiceFactory` cấp phát service và wiring dependency.

### 4.2 Quy ước đặt tên

- Interface: tiền tố `I` (`IAuthService`, `IInvoiceRepository`...).
- Service impl/repo impl: hậu tố `Impl`.
- Lớp UI: `*Frame`, `*Panel`, `*Dialog`.
- Presenter: `*Presenter`.
- Entity/DTO: đặt tên theo nghiệp vụ (`Product`, `Batch`, `Invoice`, `CartItem`...).

### 4.3 Mẫu code truy cập DB

- Dùng `PreparedStatement` để tránh SQL Injection.
- Dùng `try-with-resources` để đóng `Connection/Statement/ResultSet`.
- Ném `RuntimeException` có ngữ cảnh nghiệp vụ khi truy vấn lỗi.
- Có helper ánh xạ dòng (`mapRow`) trong repository.

### 4.4 Mẫu code transaction

- Các nghiệp vụ nhiều bước dùng:
  - `setAutoCommit(false)`
  - xử lý từng bước
  - `commit()`
  - `rollback()` nếu lỗi
  - khôi phục `setAutoCommit(true)` trong `finally`

Điều này được áp dụng rõ trong:
- `SaleServiceImpl.checkout(...)`
- `ProductRepositoryImpl.bulkUpdateStatus(...)`

### 4.5 Phong cách UI

- Dùng token màu từ `common.AppColors` để đồng bộ theme.
- UI wiring event rõ ràng (button/search/header click).
- Với MVP: View không giữ business logic, delegate sang presenter.

## 5) Logic nghiệp vụ cốt lõi

### 5.1 Đăng nhập + phân quyền

Thành phần:
- `presentation/LoginFrame`
- `service/impl/AuthServiceImpl`
- `infrastructure/repository/UserRepositoryImpl`
- `common/Session`

Flow:
1. Người dùng nhập username/password tại `LoginFrame`.
2. `AuthServiceImpl.login(...)` kiểm tra dữ liệu đầu vào.
3. `UserRepositoryImpl.findByCredentials(...)` truy vấn tài khoản.
4. Kiểm tra trạng thái tài khoản (bị khóa hay không).
5. Lưu session qua `Session.setCurrentUser(user)`.
6. Mở `MainFrame`, áp dụng phân quyền menu theo vai trò.

### 5.2 Quản lý sản phẩm (module hoàn thiện nhất)

Thành phần:
- UI view: `presentation/ProductPanel` (lớp đầy đủ, không phải placeholder).
- Presenter: `presentation/presenter/ProductPresenter`.
- Service: `service/impl/ProductServiceImpl`.
- Repository: `infrastructure/repository/ProductRepositoryImpl`.

Nghiệp vụ đã có:
- CRUD sản phẩm.
- Soft delete qua `TrangThai`.
- Tìm kiếm theo tên sản phẩm.
- Lọc theo trạng thái (Đang bán/Ngừng bán/Tất cả).
- Phân trang server-side (`OFFSET ... FETCH NEXT`).
- Sắp xếp server-side với whitelist cột sắp xếp.
- Bulk action (ngừng/khôi phục nhiều sản phẩm).
- Tính giá sỉ theo phần trăm giảm.

Chi tiết kỹ thuật đáng chú ý:
- `ProductPresenter` không import Swing/AWT, giữ state phân trang/sort/chọn.
- `globalSelectedIds` giữ ID đã tick xuyên trang.
- `mapSortColumn(...)` trong repository dùng whitelist map UI -> DB để tránh SQL Injection.
- Sort có tie-breaker theo `MaSP` (khi cần) để dữ liệu ổn định.

### 5.3 Bán hàng POS + FEFO

Thành phần:
- Service: `service/impl/SaleServiceImpl`.
- Repository liên quan: `BatchRepositoryImpl`, `InvoiceRepositoryImpl`, `InvoiceDetailRepositoryImpl`, `CustomerRepositoryImpl`.

Nghiệp vụ:
1. Kiểm tra giỏ hàng không rỗng.
2. Find-or-create khách hàng theo số điện thoại.
3. Tạo hóa đơn.
4. Duyệt từng item giỏ hàng theo FEFO:
   - Lấy danh sách lô hết hạn sớm trước (`ORDER BY HanSuDung ASC`).
   - Khóa dòng bằng `WITH (UPDLOCK, ROWLOCK)` để tránh race condition.
   - Trừ tồn kho theo lô.
   - Ghi chi tiết hóa đơn, snapshot `GiaVon` từ lô.
5. Cập nhật tổng tiền hóa đơn.
6. Commit toàn bộ; lỗi thì rollback.

Giá trị nghiệp vụ:
- Bán đúng lô sắp hết hạn trước (giảm tồn quá hạn).
- Tránh 2 nhân viên bán trùng số lượng cuối cùng.
- Bảo toàn lịch sử giá vốn theo thời điểm bán.

### 5.4 Nhập kho / kho / dashboard

- Backend cho nhập kho, lô hàng, hóa đơn, report đã có nhiều thành phần.
- UI cho các module này phần lớn đang placeholder hoặc đang triển khai tiếp.
- `MainFrame` đã setup card + menu cho các khu vực này để mở rộng dần.

## 6) Cơ sở dữ liệu và migration

Thư mục `database/` chứa script tạo schema, seed data, migration và index.

Các mốc script quan trọng:
- `01_create_database.sql`: tạo DB và bảng nền.
- `02_seed_data.sql`: dữ liệu mẫu.
- `03_stored_procedures.sql`: view + stored procedures.
- `05_migration_financial_fix.sql`: điều chỉnh mô hình giá nhập/giá vốn/phương thức thanh toán.
- `06_edge_case_fixes.sql`: lock FEFO, trigger admin, bổ sung cột liên quan.
- `07_unique_lot_constraint.sql`: unique lô theo sản phẩm.
- `08_phieunhap_giasi_fix.sql`, `09_nha_cung_cap.sql`, `10_index_sort_columns.sql`: bổ sung nghiệp vụ và tối ưu.

Cơ chế bảo vệ dữ liệu nổi bật:
- Trigger không cho hệ thống mất admin hoạt động cuối cùng.
- Ràng buộc unique lô theo sản phẩm.
- Check constraint tồn kho không âm.
- View tổng hợp tồn kho và đối soát doanh thu.

## 7) Hiện trạng source code

Trạng thái thực tế khi đọc source:

- Hoàn thiện tốt:
  - Đăng nhập, phân quyền, session.
  - Kiến trúc service/repository có đủ interface + impl.
  - Product module đầy đủ (UI + presenter + service + repo).
  - FEFO checkout và transaction.
- Đang tiếp tục:
  - Các panel như `ImportPanel`, `InventoryPanel`, `POSPanel`, `DashboardPanel` trong package `presentation.panel`.

Lưu ý cấu trúc:
- Hiện có 2 lớp trùng tên `ProductPanel`:
  - `presentation/ProductPanel` (bản đầy đủ, đang được dùng bởi `MainFrame`).
  - `presentation/panel/ProductPanel` (placeholder).
Nên thống nhất về sau để tránh nhầm lẫn khi bảo trì.

## 8) Hướng dẫn chạy chuẩn (đã tối ưu)

### Cách 1 - 1 lệnh (khuyến nghị)

Điều kiện:
- Docker Desktop đang chạy.
- Có `Java 17+`, `Maven`, và `sqlcmd`.

Chạy từ thư mục project:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-project.ps1
```

Script sẽ tự làm theo thứ tự:
1. Kiểm tra Docker daemon.
2. Tạo/chạy container SQL Server `eproject-sql` (port `1433`).
3. Chờ SQL sẵn sàng.
4. Chạy full migration.
5. Mở app (`App` -> `LoginFrame`).

Tài khoản mặc định:
- `admin / admin123`

### Cách 2 - chạy thủ công

1. Bật SQL Server (hoặc Docker SQL) tại `localhost:1433`.
2. Chạy migration:
   - `powershell -ExecutionPolicy Bypass -File .\database\run_all_migrations.ps1 -Server localhost -User sa -Pass "Admin@123456" -TargetDb QuanLyCuaHangThuoc`
3. Chạy app:
   - `mvn exec:java "-Dexec.mainClass=App"`

### Dừng hệ thống nhanh

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-project.ps1
```

## 9) Định hướng phát triển tiếp

- Hoàn tất UI cho Nhập kho, Tồn kho, POS, Khách hàng, Dashboard.
- Đồng bộ hoàn toàn MVP cho các module còn lại.
- Bổ sung test tự động cho service/presenter quan trọng.
- Chuẩn hóa cấu trúc package để loại bỏ class placeholder trùng tên.
- Nâng cấp bảo mật đăng nhập (hash password thay vì lưu plain text).

## 10) Kết luận tổng quan

Đây là codebase có nền tảng kiến trúc tốt cho đồ án desktop:
- Layering rõ ràng, tách biệt trách nhiệm.
- Có nhiều quyết định kỹ thuật hướng đến an toàn dữ liệu và khả năng mở rộng.
- Module sản phẩm và checkout FEFO là 2 điểm sáng rõ nét về phong cách code và logic nghiệp vụ.

Nếu tiếp tục hoàn thiện đầy đủ UI cho các module còn lại, dự án có thể đạt mức vận hành nghiệp vụ khép kín cho cửa hàng thuốc từ đầu đến cuối.

## 11) Demand planning module (new)

Phan mo rong cho de tai chuoi nha thuoc da duoc them:

- Migration mo hinh du lieu + data mart: `database/42_demand_planning_datamart.sql`.
- Pipeline ETL/forecast/optimization thong qua stored procedures:
  - `sp_DemandPlanning_RefreshDataMart`
  - `sp_DemandPlanning_GenerateForecast`
  - `sp_DemandPlanning_RunOptimization`
  - `sp_DemandPlanning_Evaluate`
- Java integration:
  - `service/IDemandPlanningService`
  - `infrastructure/repository/DemandPlanningRepositoryImpl`
  - `tools/DemandPlanningRunner`
- Script demo nhanh:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\run-demand-planning.ps1`

Bao cao danh gia chi tiet: `docs/demand-planning-report.md`.
