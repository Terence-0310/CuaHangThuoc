# README TỔNG QUAN DỰ ÁN

## 1) Giới thiệu

**Pharmacy Store Management (MerPhar)** là ứng dụng desktop Java Swing quản lý cửa hàng thuốc theo hướng thực tế vận hành, bao gồm bán hàng, nhập kho, quản lý lô - hạn dùng, khách hàng, hóa đơn, người dùng và nhân sự/chấm công/tính lương.

Mục tiêu của dự án:
- Chuẩn hóa quy trình vận hành cửa hàng thuốc trên một hệ thống thống nhất.
- Giảm sai sót thủ công trong bán hàng, tồn kho, xuất lô, chấm công và payroll.
- Hỗ trợ demo nghiệp vụ đầy đủ cho nhiều vai trò (Admin/Nhân viên).

---

## 2) Công nghệ sử dụng

- **Ngôn ngữ**: Java 17
- **UI**: Java Swing + FlatLaf
- **Cơ sở dữ liệu**: Microsoft SQL Server
- **Kết nối DB**: JDBC + HikariCP
- **Build tool**: Maven
- **Testing**: JUnit 5
- **Thư viện phụ trợ**: JFreeChart, OpenPDF, JCalendar, SLF4J

---

## 3) Kiến trúc & cấu trúc mã nguồn

Dự án tổ chức theo hướng tách lớp rõ ràng:
- `presentation`: UI (Frame/Panel/Dialog)
- `service`: nghiệp vụ ứng dụng
- `domain`: entity, DTO, repository interface
- `infrastructure`: truy cập dữ liệu (DAO/Repository, DB helper)
- `common`: tiện ích dùng chung, session, factory

Điểm nổi bật:
- Có tách interface repository/service để dễ bảo trì.
- UI chạy theo panel/module, dễ mở rộng theo chức năng.
- Cấu hình DB ưu tiên biến môi trường, giảm hard-code mật khẩu trong repo.

---

## 4) Chức năng chính

- **Đăng nhập & phân quyền**: Admin / Nhân viên
- **Dashboard/Report**: tổng quan số liệu vận hành
- **Bán hàng (POS)**:
  - Quản lý giỏ hàng
  - Thanh toán và tạo hóa đơn
  - Hỗ trợ FEFO trực quan khi xuất lô
  - QR thanh toán theo thông tin cấu hình hiện tại
- **Quản lý kho**:
  - Lô hàng, hạn dùng, tồn kho
  - FEFO để ưu tiên xuất lô gần hết hạn
- **Nhập kho**: tạo phiếu nhập, cập nhật tồn
- **Khách hàng/Nhà cung cấp/Sản phẩm/Hóa đơn**
- **Nhân sự (HRM)**:
  - Chấm công
  - Xếp ca
  - Bảng lương tháng
  - Hỗ trợ mô hình ca 24/24 (6-14, 14-22, 22-6) và phụ cấp ca đêm

---

## 5) Cài đặt và chạy nhanh

### Yêu cầu
- JDK 17+
- Maven 3.9+
- SQL Server đang hoạt động

### Cách chạy nhanh (khuyến nghị)
- Chạy file: `ChayDuAn.bat`
- Script sẽ chạy ứng dụng theo chế độ nhanh.

### Tùy chọn chạy migration DB
Trong PowerShell:

```powershell
$env:MEPHAR_RUN_DB_MIGRATION="1"
.\ChayDuAn.ps1
```

---

## 6) Cấu hình cơ sở dữ liệu

Ưu tiên biến môi trường:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `DB_DRIVER`

File cấu hình fallback:
- `src/main/resources/application.properties`

Lưu ý:
- Không nên commit mật khẩu thật vào repo.
- Dùng `.env.example` làm mẫu cấu hình môi trường.

---

## 7) Lệnh Maven thường dùng

- Build compile:
```bash
mvn clean compile
```

- Chạy test:
```bash
mvn test
```

- Kiểm tra kết nối DB qua execution `test-db`:
```bash
mvn exec:java@test-db
```

- Chạy ứng dụng:
```bash
mvn exec:java
```

---

## 8) Dữ liệu demo và migration

Thư mục `database/` chứa script migration/seed:
- Seed nhân viên, sản phẩm, lô hàng
- Seed lịch làm theo 3 ca
- Seed payroll demo từ dữ liệu attendance

Script tổng:
- `database/run_all_migrations.ps1`

---

## 9) Tài khoản demo tham khảo

- Admin: `admin / admin123`
- Nhân viên: `nhanvien01 / nv123`

> Tài khoản có thể thay đổi theo dữ liệu seed hiện tại của máy.

---

## 10) Ghi chú phát triển

- Tránh sửa trực tiếp business logic đang ổn định nếu không có test đi kèm.
- Các lớp placeholder/legacy UI đã được cô lập dần để giảm nhầm lẫn.
- Nếu phát sinh lỗi runtime, ưu tiên kiểm tra:
  1. Kết nối SQL Server
  2. Biến môi trường DB
  3. Dữ liệu seed/migration mới nhất

