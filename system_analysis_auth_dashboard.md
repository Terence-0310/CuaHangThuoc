# TÀI LIỆU PHÂN TÍCH HỆ THỐNG
## 1. MÔ ĐUN XÁC THỰC, PHÂN QUYỀN VÀ QUẢN TRỊ NGƯỜI DÙNG (AUTH & USER MANAGEMENT)
## 2. MÔ ĐUN BẢNG ĐIỀU KHIỂN TỔNG QUAN (DASHBOARD)

---

### YÊU CẦU TỔNG QUAN
Tài liệu này đi sâu vào việc phân tích luồng logic, cơ sở dữ liệu và yêu cầu nghiệp vụ đối với hai nhóm chức năng cốt lõi của phần mềm Quản lý Nhà thuốc: **An ninh & Tài khoản (Login, CRUD, Phân quyền)** và **Cái nhìn tổng quan (Dashboard)**.

---

### PHẦN 1: MÔ ĐUN XÁC THỰC & PHÂN QUYỀN (LOGIN & AUTHORIZATION)

#### 1.1 Khái quát nghiệp vụ
- **Mô tả:** Hệ thống yêu cầu người dùng phải định danh trước khi truy cập bất kỳ chức năng nào. Thông tin đăng nhập được lưu giữ xuyên suốt phiên làm việc (Session) tĩnh.
- **Đối tượng (Actors):**
  - **Quản trị viên (Admin):** Toàn quyền truy cập tất cả menu (Hệ thống, Nhân sự, Kho, Kế toán).
  - **Nhân viên (Thu ngân / Bán hàng):** Truy cập hạn chế, chỉ thấy giao diện POS, Lịch sử hóa đơn cá nhân, Chấm công, và xem Tồn kho. Không được quyền xoá hay thay đổi tài khoản người khác.

#### 1.2 Thiết kế Dữ liệu (Database Schema)
Bảng **`NguoiDung`** (Người dùng hệ thống):
- `MaND` (INT, Primary Key, Identity): Mã định danh duy nhất.
- `TenDangNhap` (VARCHAR): Tên đăng nhập (Unique), cấm chứa ký tự đặc biệt chữ Việt.
- `MatKhau` (VARCHAR): Mật khẩu truy cập. (Cần được mã hoá bcrypt/SHA-256 trong phiên bản nâng cao).
- `HoTen` (NVARCHAR): Tên hiển thị của chủ tài khoản.
- `VaiTro` (NVARCHAR): Xác định quyền hạn (`Quản trị viên` hoặc `Nhân viên`).
- `TrangThai` (BIT): `1` (Đang hoạt động) / `0` (Đã bị khóa ngưng truy cập).

#### 1.3 Quy trình Đăng nhập (Login Flow)
1. Người dùng nhập `TenDangNhap` và `MatKhau`.
2. Hệ thống truy vấn CSDL:
   - Nếu không tồn tại -> Báo lỗi "Tài khoản không tồn tại hoặc sai thông tin".
   - Nếu tồn tại nhưng `TrangThai = 0` -> Báo lỗi "Tài khoản của bạn đã bị khóa tĩnh".
   - Nếu hợp lệ -> Lưu object `NguoiDung` vào lớp tĩnh `Session`.
3. Khởi tạo `MainFrame`. Dựa vào `Session.VaiTro`, hệ thống tự động ẩn hoặc vô hiệu hoá các Tab (Ví dụ: Tab Nhân sự, Tab Quản lí Kho, Tab Nhà Cung Cấp sẽ bị `.setVisible(false)` nếu VaiTro là Nhân viên).

---

### PHẦN 2: CHỨC NĂNG QUẢN TRỊ NGƯỜI DÙNG (CRUD TÀI KHOẢN)

Được đặt tại `UserManagementPanel` (chỉ hiển thị cho Admin).

#### 2.1 Nghiệp vụ cụ thể (CRUD)
- **C (Create - Thêm mới):** Admin tạo tài khoản cho nhân sự mới. Không cho phép trùng `TenDangNhap`. Mật khẩu mặc định tự gán (ví dụ: `123456`) hoặc admin nhập thủ công.
- **R (Read - Đọc dữ liệu):** Hiển thị danh sách bảng tài khoản gồm (Mã, Tên, User, Vai trò, Trạng thái). Có công cụ tìm kiếm nhanh THEO tên hoặc username.
- **U (Update - Chỉnh sửa):** 
  - Admin có quyền thay tên, chỉnh quyền (Role) cho tài khoản, hoặc đặt lại (Reset) mật khẩu nếu nhân viên quên.
  - *Quy tắc ràng buộc:* Không cho phép đổi tên đăng nhập. Không cho phép Admin tự thu hồi quyền Admin của chính mình khi đang đăng nhập, tránh tình trạng "cả hệ thống không còn admin nào".
- **D (Disable/Delete - Xóa/Khóa):**
  - Trong các hệ thống bán thuốc / POS thực tế, **TUYỆT ĐỐI KHÔNG XÓA CỨNG (DELETE)** tài khoản đã phát sinh giao dịch (tạo hoá đơn, phiếu nhập).
  - Giải pháp: Sử dụng cơ chế **Soft-Delete** (Chuyển `TrangThai = 0`). Tài khoản này sẽ bị đẩy ra nếu đang online và không thể đăng nhập lại, nhưng tên và lịch sử hoá đơn vẫn tồn tại nguyên vẹn trên sổ sách.

#### 2.2 Luồng / Logic giao diện UI
- **Khung bên trái:** Form chi tiết gõ thông tin (Tên hiển thị, Tên đăng nhập, Mật khẩu, Vai trò JComboBox, Trạng thái JComboBox). Gồm 3 Nút xử lý đồng thời đóng vai trò bảo vệ hệ thống: "Thêm Mới", "Lưu Cập Nhật" (Chỉ hiện khi chọn danh sách), "Khoá Tài Khoản".
- **Khung bên phải:** `JTable` danh sách toàn bộ Users. Ấn vào dòng nào thì Form thông tin đẩy data qua trái.

---

### PHẦN 3: BẢNG ĐIỀU KHIỂN TỔNG QUAN (DASHBOARD)

Dashboard là điểm nhìn đầu tiên khi đăng nhập thành công. Đây là hệ thống phân tích BI (Business Intelligence) mini, tập trung cung cấp cái nhìn Real-time về sức khỏe nhà thuốc.

#### 3.1 Chỉ số Thượng tầng (Key Performance Indicators - KPIs)
Bao gồm các Component Thẻ (Card Summary) ở vùng trên cùng:
1. **Tổng doanh thu hôm nay:** Dựa trên bảng `HoaDon` -> `SUM(TongTien)` điều kiện `NgayLap = TODAY()`.
2. **Tổng đơn hàng:** Đếm số lượng hoá đơn hợp lệ được xuất ra trong ngày.
3. **Cảnh báo Tồn Kho / Hết Hạn:** Cảnh báo các lô hàng có số lượng sắp cạn, hoặc hạn sử dụng còn dưới 30 ngày.

#### 3.2 Khối Dữ Liệu Bảng (Data Tables & Charts)
Gồm các khu vực bảng dữ liệu rút gọn ở trung tâm tuỳ theo độ ưu tiên:
- **Bảng 1: Lô Thuốc Cận Date / Hết Hạn:**
  - Lấy từ bảng `LoHang` kết nối với `SanPham`.
  - Hiển thị: Mã Thuốc, Tên Thuốc, Số Lô, Ngày Hết Hạn.
  - Phục vụ quá trình "Xả Hàng" hoặc thực hiện trả nhà cung cấp kịp thời.
  
- **Bảng 2: Sản Phẩm Bán Chạy:**
  - Bảng xếp hạng Thuốc/Sản phẩm được bán ra với số lượng/doanh thu lớn nhất.
  - Giúp quản lý lập kế hoạch gọi điện xin Nhà cung cấp chiết khấu nhập khẩu cao hơn cho tháng tới.

#### 3.3 Yêu cầu Trải nghiệm & Tối ưu hoá (Query Optimization)
Các lệnh đọc trên Dashboard đòi hỏi phải gom nhóm (GROUP BY) và tính tổng (SUM) khối dữ liệu hoá đơn và lô hàng. Để phần mềm luôn mượt:
- **Cơ sở dữ liệu:** Tạo các `Index` trên `NgayLap` (Hoá Đơn) và `HanSuDung` (Lô Hàng).
- **Phân luồng Giao diện:** Sử dụng `SwingWorker` bọc tất cả các hàm SELECT dữ liệu (Dashboard Data Loading). Nghĩa là dữ liệu ngầm được tải qua luồng nhỏ (Background Thread), bảng điều khiển và thanh công cụ vẫn phản hồi nhanh chóng mà không gặp lỗi "Not Responding" của Java Swing.

---

### TỔNG KẾT
Ba khối chức năng (Dashboard, Authentication, User Management) tạo thành một khung xương sống cực kỳ vững chắc về chuẩn bảo mật (Security), độ ổn định (Reliability) và trực quan hóa (Visualization) cho môi trường phần mềm quản trị hệ thống Y tế/Nhà thuốc.
