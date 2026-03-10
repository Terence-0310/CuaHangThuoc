# Tóm tắt dự án: eProject_StoreBanThuoc

Tài liệu này tóm tắt nhanh cấu trúc, những tính năng đã được triển khai, những phần còn thiếu và hướng phát triển tiếp theo cho dự án eProject_StoreBanThuoc.

---

## 1. Mục tiêu
Ứng dụng desktop (Swing) quản lý nhà thuốc theo nguyên tắc FEFO (First Expire First Out). Hỗ trợ nghiệp vụ bán hàng, nhập hàng theo lô, quản lý tồn kho, quản lý người dùng và phân quyền.

## 2. Những phần đã có (Implemented)
- Cấu trúc project rõ ràng: `database`, `dao`, `model`, `service`, `ui`, `app`.
- Kết nối DB cơ bản: `database/ConnectDB.java` (sử dụng DriverManager với connection string hiện nằm trong file).
- Authentication & session:
  - `dao/UserDAO.findByUsername(String)` — lấy user từ bảng `NguoiDung`.
  - `service/AuthService.login(...)` — kiểm tra tồn tại, kiểm tra `DaXoa`, so sánh mật khẩu (plaintext), trả về `User` khi thành công.
  - `app/SessionManager` — lưu `currentUser`, logout, kiểm tra trạng thái đăng nhập.
- Phân quyền:
  - `model/Role.java` (Admin, QuanLy, NhanVien).
  - `model/Permission.java` (USER_MANAGE, MEDICINE_MANAGE, REPORT_VIEW).
  - `service/RolePermission` — ánh xạ role -> tập permission (Admin = tất cả, QuanLy = MEDICINE_MANAGE + REPORT_VIEW, NhanVien = MEDICINE_MANAGE).
  - `service/PermissionService` — hàm tiện ích `has(User, Permission)`.
- UI cơ bản:
  - `ui/LoginForm.java` — form đăng nhập, validate form, gọi AuthService, lưu session và mở `Dashboard`.
  - `ui/Dashboard.java` — header (hiển thị user & role), sidebar menu (ẩn/hiện menu theo permission), placeholder panels cho User/Medicine/Report, logout.
- Cơ sở dữ liệu: có file script `database/CuaHangThuoc_Batch.sql` chứa schema khá đầy đủ (bảng: NguoiDung, KhachHang, NhaCungCap, SanPham, LoHang, PhieuNhap, triggers, index FEFO, nhiều chú thích nghiệp vụ và tham chiếu stored-proc). Đây là một tài nguyên quan trọng để triển khai DAO/Service tiếp theo.

## 3. Những phần còn thiếu / cần hoàn thiện (Not implemented / Partial)
- Bảo mật mật khẩu: mật khẩu lưu/so sánh plaintext (không hashing). Cần dùng bcrypt/argon2.
- Config: connection string & credentials nằm trong `ConnectDB.java` (hardcoded). Nên tách thành file cấu hình hoặc biến môi trường.
- User management CRUD: chỉ có `findByUsername`. Thiếu create/update/list/soft-delete (mặc dù DB có trigger soft-delete).
- Các DAO/Service nghiệp vụ chính: thiếu các DAO cho SanPham, LoHang, PhieuNhap, HoaDon, ChiTietHoaDon, KhachHang, NhaCungCap, v.v.
- UI module thực tế: Dashboard hiển thị menu nhưng các panels là placeholder (chưa có JTable, form CRUD, báo cáo thật sự).
- Stored-proc & logic FEFO: SQL đề xuất stored-procs (ví dụ `sp_HoaDonBan_Sell_FEFO`) nhưng Java chưa gọi/triển khai các proc hoặc logic tương đương.
- Connection pooling, logging, unit tests, và xử lý lỗi nâng cao chưa được thêm.

## 4. File chính & vai trò (quick reference)
- `pom.xml` — dependencies: mssql-jdbc, lombok.
- `database/ConnectDB.java` — kết nối DB (hardcoded config).
- `database/CuaHangThuoc_Batch.sql` — script tạo schema & hướng nghiệp vụ.
- `model/` — `User.java`, `Role.java`, `Permission.java`.
- `dao/UserDAO.java` — `findByUsername`.
- `service/AuthService.java` — login logic.
- `service/RolePermission.java`, `service/PermissionService.java` — phân quyền.
- `app/SessionManager.java` — quản lý phiên.
- `ui/LoginForm.java`, `ui/Dashboard.java` — UI chính (login + dashboard).

## 5. Đề xuất hành động tiếp theo (ưu tiên)
1. Di chuyển cấu hình DB ra `src/main/resources/config.properties` hoặc dùng biến môi trường (bảo mật). (ngắn, an toàn)
2. Thêm hashing mật khẩu (bcrypt). Cập nhật flow tạo user & login để dùng hashing. (quan trọng)
3. Tạo các phương thức DAO thiếu cho `NguoiDung` (list, create, update, softDelete) và UI `UserManagementPanel` (JTable + CRUD dialog). (trung bình)
4. Triển khai DAO & UI cho `SanPham`, `LoHang`, `PhieuNhap`, `HoaDon` theo schema; ưu tiên implement hoặc gọi stored-proc FEFO cho bán hàng. (trung-cao)
5. Thêm connection pooling (HikariCP), logging (SLF4J), và unit tests. (tăng chất lượng)

## 6. Lưu ý bảo mật & vận hành
- Không lưu mật khẩu DB trong mã nguồn. Nếu repo chia sẻ, ngay lập tức thay password bằng placeholder và sử dụng file config/biến môi trường.
- Không dùng so sánh plaintext cho mật khẩu; dùng bcrypt/argon2 với salt.
- Kiểm tra trigger DB (soft-delete) trước khi thiết kế chức năng xóa trong UI để tránh xung đột hành vi.

## 7. Cách chạy nhanh (dev)
- Mở project trong Eclipse/IDE.
- Chạy class `ui.LoginForm` (phương thức `main`) để mở giao diện.
- Hoặc build bằng Maven: `mvn compile` rồi chạy class từ IDE.

## 8. Ghi chú dành cho developer (quick tips)
- Để tách cấu hình DB: sửa `database/ConnectDB.java` để đọc file `config.properties` (URL, server, port, db, user, password) thay vì hardcode.
- Để thêm hashing: thêm dependency `org.mindrot:jbcrypt` hoặc `spring-security-core`, thay đổi logic tạo user (hash password trước khi lưu) và `AuthService` (so sánh hash).
- Khi triển khai bán hàng theo FEFO: tham khảo index `IX_LoHang_FEFO` và stored-proc/ghi chú trong `CuaHangThuoc_Batch.sql`.

---

Nếu bạn muốn, tôi có thể ngay lập tức thực hiện một trong các thay đổi sau (chọn 1):
- A) Di chuyển cấu hình DB ra `config.properties` và cập nhật `ConnectDB` (ít rủi ro).
- B) Thêm hashing mật khẩu (bcrypt) và cập nhật `AuthService` cùng ví dụ tạo user.
- C) Tạo skeleton `UserManagementPanel` + mở rộng `UserDAO` (list/create/softDelete) và test nhanh.

Hãy chọn A / B / C hoặc yêu cầu khác, tôi sẽ thực hiện và kiểm tra build ngay lập tức.
