# Hệ thống quản lý cửa hàng thuốc

Ứng dụng desktop Java Swing quản lý cửa hàng thuốc với quản lý lô hàng và nguyên tắc FEFO (First Expire First Out).

## Tính năng chính

- ✅ **Đăng nhập/Phân quyền**: Hệ thống phân quyền theo vai trò (Admin, Quản lý, Nhân viên)
- ✅ **Dashboard**: Tổng quan với thống kê và cảnh báo
- ✅ **Bán hàng**: Bán hàng theo nguyên tắc FEFO (lô hết hạn trước xuất trước)
- ✅ **Nhập hàng**: Quản lý nhập hàng theo lô với kiểm tra hạn sử dụng
- ✅ **Quản lý sản phẩm**: CRUD sản phẩm với nhiều loại (Thuốc, Dược mỹ phẩm, TPCN, etc.)
- ✅ **Quản lý lô hàng**: Theo dõi tồn kho và hạn sử dụng theo từng lô
- ✅ **Quản lý khách hàng**: Quản lý thông tin khách hàng
- ✅ **Quản lý nhà cung cấp**: Quản lý thông tin nhà cung cấp
- ✅ **Báo cáo**: Báo cáo bán hàng với biểu đồ (JFreeChart)
- ✅ **Quản lý người dùng**: Quản lý người dùng (chỉ Admin)

## Công nghệ sử dụng

- **Java 17**: Ngôn ngữ lập trình
- **Java Swing**: Giao diện desktop
- **Maven**: Quản lý dependencies
- **SQL Server**: Database
- **HikariCP**: Connection pooling
- **JFreeChart**: Vẽ biểu đồ

## Cấu trúc dự án

```
src/main/java/
├── app/              # Main application classes
│   ├── LoginFrame.java
│   └── MainFrame.java
├── common/           # Common utilities
│   └── ConnectDB.java
├── components/       # Custom components
│   └── ProductTable.java
├── dao/              # Data Access Objects
│   ├── NguoiDungDao.java
│   ├── SanPhamDao.java
│   ├── HoaDonBanDao.java
│   ├── PhieuNhapDao.java
│   ├── KhachHangDao.java
│   └── NhaCungCapDao.java
├── entity/           # Entity classes
│   ├── NguoiDung.java
│   ├── SanPham.java
│   ├── LoHang.java
│   ├── HoaDonBan.java
│   ├── PhieuNhap.java
│   ├── KhachHang.java
│   └── NhaCungCap.java
└── panels/           # UI Panels
    ├── DashboardPanel.java
    └── BaoCaoPanel.java
```

## Cài đặt và chạy

### Yêu cầu

- Java 17+
- Maven 3.6+
- SQL Server với database `CuaHangThuoc_Batch`

### Cấu hình database

1. Chạy script SQL trong `database/CuaHangThuoc_Batch.sql` để tạo database
2. Cập nhật thông tin kết nối trong `src/main/java/common/ConnectDB.java`:
   - `SERVER_NAME`: Tên server SQL Server
   - `PORT`: Port (mặc định 1433)
   - `DATABASE_NAME`: Tên database
   - `USER`: Username
   - `PASSWORD`: Password

### Chạy ứng dụng

```bash
# Compile
mvn compile

# Chạy ứng dụng
mvn exec:java

# Hoặc chạy test kết nối database
mvn exec:java@test-db
```

## Tài khoản mặc định

Sau khi chạy script SQL, có các tài khoản mặc định:

- **Admin**: `admin` / `123`
- **Quản lý**: `ql01` / `123`
- **Nhân viên**: `nv01` / `123`

## Nguyên tắc nghiệp vụ

### FEFO (First Expire First Out)

- Lô nào có hạn sử dụng gần nhất thì phải xuất trước
- Không cho bán lô đã hết hạn
- Tự động tách số lượng bán nếu cần qua nhiều lô

### Quản lý lô hàng

- Mỗi sản phẩm có thể có nhiều lô khác nhau
- Tồn kho được tính từ tổng các lô còn hiệu lực
- Kiểm tra hạn sử dụng khi nhập và bán hàng

### Phân quyền

- **Nhân viên**: Bán hàng, xem sản phẩm, lô hàng, khách hàng
- **Quản lý**: Tất cả quyền của Nhân viên + Nhập hàng
- **Admin**: Full quyền + Quản lý người dùng

## Phát triển tiếp

Các tính năng đang phát triển:

- [ ] Panel Bán hàng hoàn chỉnh
- [ ] Panel Nhập hàng hoàn chỉnh
- [ ] Panel Quản lý Sản phẩm
- [ ] Panel Quản lý Lô hàng
- [ ] Panel Quản lý Khách hàng
- [ ] Panel Quản lý Nhà cung cấp
- [ ] Panel Quản lý Người dùng
- [ ] Dashboard với dữ liệu thực từ database
- [ ] Báo cáo với filter và export

## License

Generated for educational purposes.
