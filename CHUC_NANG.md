# MEPHAR - Quản lý Nhà Thuốc
## Tài liệu Hệ Thống Chức Năng Chi Tiết

**Phiên bản:** 2.0  
**Ngày cập nhật:** Tháng 3, 2026  
**Công nghệ:** Java Swing | SQL Server | FlatLaf | DAO Pattern

---

## Mục Lục
1. [Tổng Quan](#1-tổng-quan)
2. [Bán Hàng](#2-bán-hàng)
3. [Nhập Hàng](#3-nhập-hàng)
4. [Sản Phẩm](#4-sản-phẩm)
5. [Lô Hàng](#5-lô-hàng)
6. [Khách Hàng](#6-khách-hàng)
7. [Nhà Cung Cấp](#7-nhà-cung-cấp)
8. [Báo Cáo & Thống Kê](#8-báo-cáo--thống-kê)
9. [Người Dùng](#9-người-dùng)

---

## 1. Tổng Quan

### 📊 Chức Năng Chính
- **Dashboard thông tin tổng hợp:** Hiển thị các KPI chính của hệ thống
- **Quản lý bán hàng theo FEFO:** Đảm bảo bán hàng hết hạn trước tiên
- **Theo dõi tồn kho:** Quản lý tồn kho trong thời gian thực
- **Cảnh báo hết hạn:** Thông báo tự động về lô hàng sắp hết hạn
- **Cảnh báo tồn kho thấp:** Nhắc nhở khi tồn kho dưới mức tối thiểu

### 🔧 Thành Phần Chính
- **DashboardPanel:** Panel hiển thị tổng quan và thống kê
- **MainFrame:** Frame chính với menu Sidebar điều hướng
- **LoginFrame:** Màn hình xác thực người dùng

### 📁 Bảng SQL Liên Quan
- `NguoiDung` - Thông tin người dùng hệ thống
- `HoaDonBan` - Các hóa đơn bán hàng
- `LoHang` - Tồn kho các lô hàng

### 🔗 Các Stored Procedure Chính
- `sp_Login` - Xác thực người dùng
- `sp_HoaDonBan_Create` - Tạo hóa đơn mới
- `sp_HoaDonBan_Sell_FEFO` - Bán hàng theo FEFO
- `sp_PhieuNhap_Create` - Tạo phiếu nhập mới

---

## 2. Bán Hàng

### 📋 Chức Năng Chính
- **Tạo hóa đơn mới:** Khởi tạo một hóa đơn bán hàng trống
- **Thêm sản phẩm vào hóa đơn:** Lựa chọn sản phẩm và số lượng
- **Bán hàng theo FEFO:** Tự động lấy từ lô hàng sớm nhất hết hạn
- **Tính toán giá bán:** Nhập giá bán và tính tổng tiền
- **Quản lý khách hàng:** Chọn hoặc thêm khách hàng cho hóa đơn
- **Xem lịch sử bán hàng:** Danh sách tất cả hóa đơn đã tạo
- **Tìm kiếm hóa đơn:** Tìm kiếm theo ngày, khách hàng, số hóa đơn

### 🛠️ Các Methods Quan Trọng (HoaDonBanDao)
| Method | Chức Năng | Tham Số |
|--------|-----------|---------|
| `createHoaDon()` | Tạo hóa đơn mới | maNguoiDung, maKhachHang, ghiChu |
| `sellProductFEFO()` | Bán hàng theo FEFO | maHoaDon, maSanPham, soLuongCanBan, giaBan |
| `getAll()` | Lấy danh sách hóa đơn | - |
| `findById()` | Tìm hóa đơn theo ID | maHoaDon |
| `addItem()` | Thêm sản phẩm vào hóa đơn | maHoaDon, maSanPham, soLuong, giaBan |
| `getItems()` | Lấy danh sách sản phẩm trong hóa đơn | maHoaDon |

### 🗄️ Bảng SQL Liên Quan
```sql
HoaDonBan (Hóa Đơn Bán)
├── MaHoaDon (PK)
├── MaNguoiDung (FK) → NguoiDung
├── MaKhachHang (FK) → KhachHang
├── NgayBan (DateTime)
├── TongTien (Decimal)
└── GhiChu (nvarchar)

ChiTietHoaDon (Chi Tiết Hóa Đơn)
├── MaChiTiet (PK)
├── MaHoaDon (FK) → HoaDonBan
├── MaLoHang (FK) → LoHang
├── SoLuong (Int)
├── GiaBan (Decimal)
└── ThanhTien (Decimal)
```

### 🎨 UI Component
- **BanHangPanel:** Giao diện quản lý bán hàng
  - Biểu mẫu tạo hóa đơn
  - Danh sách sản phẩm để lựa chọn
  - Bảng chi tiết hóa đơn
  - Nút xác nhận bán hàng

---

## 3. Nhập Hàng

### 📋 Chức Năng Chính
- **Tạo phiếu nhập mới:** Khởi tạo một phiếu nhập từ nhà cung cấp
- **Thêm lô hàng vào phiếu:** Thêm sản phẩm với số lô và hạn sử dụng
- **Quản lý nhà cung cấp:** Chọn hoặc thêm nhà cung cấp
- **Nhập thông tin lô hàng:** Số lô, hạn sử dụng, giá nhập, số lượng
- **Xem lịch sử nhập hàng:** Danh sách tất cả phiếu nhập
- **Tìm kiếm phiếu nhập:** Tìm kiếm theo ngày, nhà cung cấp
- **Cập nhật tồn kho:** Tự động cập nhật khi nhập xong

### 🛠️ Các Methods Quan Trọng (PhieuNhapDao)
| Method | Chức Năng | Tham Số |
|--------|-----------|---------|
| `createPhieuNhap()` | Tạo phiếu nhập mới | maNguoiDung, maNCC, ghiChu |
| `addItemBatch()` | Thêm lô hàng vào phiếu | maPhieuNhap, maSanPham, soLo, hanSuDung, giaNhap, soLuong |
| `getAll()` | Lấy danh sách phiếu nhập | - |
| `findById()` | Tìm phiếu nhập theo ID | maPhieuNhap |
| `getItems()` | Lấy danh sách sản phẩm trong phiếu | maPhieuNhap |
| `complete()` | Hoàn thành phiếu nhập | maPhieuNhap |

### 🗄️ Bảng SQL Liên Quan
```sql
PhieuNhap (Phiếu Nhập)
├── MaPhieuNhap (PK)
├── MaNguoiDung (FK) → NguoiDung
├── MaNCC (FK) → NhaCungCap
├── NgayNhap (DateTime)
├── TongTien (Decimal)
└── GhiChu (nvarchar)

ChiTietPhieuNhap (Chi Tiết Phiếu Nhập)
├── MaChiTiet (PK)
├── MaPhieuNhap (FK) → PhieuNhap
├── MaSanPham (FK) → SanPham
├── SoLo (nvarchar)
├── HanSuDung (Date)
├── GiaNhap (Decimal)
├── SoLuong (Int)
└── ThanhTien (Decimal)

LoHang (Lô Hàng) - Được tạo từ ChiTietPhieuNhap
├── MaLoHang (PK)
├── MaSanPham (FK) → SanPham
├── SoLo (nvarchar)
├── HanSuDung (Date)
├── SoLuongNhap (Int)
├── SoLuongTon (Int)
├── GiaNhap (Decimal)
├── TrangThai (nvarchar)
└── NgayNhap (DateTime)
```

### 🎨 UI Component
- **NhapHangPanel:** Giao diện quản lý nhập hàng
  - Biểu mẫu tạo phiếu nhập
  - Danh sách sản phẩm để lựa chọn
  - Bảng chi tiết phiếu nhập
  - Nút xác nhận nhập hàng

---

## 4. Sản Phẩm

### 📋 Chức Năng Chính
- **Xem danh sách sản phẩm:** Hiển thị tất cả sản phẩm hiện có
- **Thêm sản phẩm mới:** Nhập tên, mô tả sản phẩm
- **Chỉnh sửa thông tin sản phẩm:** Cập nhật tên, mô tả, trạng thái
- **Xóa sản phẩm:** Xóa mềm (đánh dấu đã xóa)
- **Tìm kiếm sản phẩm:** Tìm kiếm theo tên sản phẩm
- **Lọc sản phẩm:** Lọc theo trạng thái hoạt động
- **Phân trang:** Hiển thị dữ liệu phân trang (nếu có nhiều sản phẩm)

### 🛠️ Các Methods Quan Trọng (SanPhamDao)
| Method | Chức Năng | Tham Số |
|--------|-----------|---------|
| `getAll()` | Lấy danh sách sản phẩm | - |
| `findById()` | Tìm sản phẩm theo ID | maSanPham |
| `searchByName()` | Tìm kiếm theo tên | keyword |
| `insert()` | Thêm sản phẩm mới | sanPham |
| `update()` | Cập nhật sản phẩm | sanPham |
| `delete()` | Xóa sản phẩm (xóa mềm) | maSanPham |

### 🗄️ Bảng SQL Liên Quan
```sql
SanPham (Sản Phẩm)
├── MaSanPham (PK)
├── TenSanPham (nvarchar)
├── MoTa (nvarchar)
├── DonViTinh (nvarchar)
├── TrangThai (bit)
├── MucTonToiThieu (Int)
├── DaXoa (bit)
└── NgayTao (DateTime)

LoHang (Lô Hàng) - Liên kết
└── MaSanPham (FK) → SanPham
```

### 🎨 UI Component
- **SanPhamPanel:** Giao diện quản lý sản phẩm
  - Bảng danh sách sản phẩm
  - Nút Thêm, Sửa, Xóa
  - Ô tìm kiếm sản phẩm
  - Dialog nhập thông tin sản phẩm

---

## 5. Lô Hàng

### 📋 Chức Năng Chính
- **Xem danh sách lô hàng:** Hiển thị tất cả lô hàng trong kho
- **Xem chi tiết lô hàng:** Thông tin số lô, hạn sử dụng, tồn kho
- **Theo dõi tồn kho:** Cập nhật số lượng tồn kho real-time
- **Lọc lô hàng theo sản phẩm:** Xem tất cả lô của một sản phẩm
- **Cảnh báo hết hạn:** Danh sách lô sắp hết hạn
- **Sắp xếp theo FEFO:** Hiển thị lô hàng theo thứ tự hạn sử dụng
- **Xóa lô hàng hết hạn:** Xóa mềm khi hết hạn hoặc hết tồn

### 🛠️ Các Methods Quan Trọng (LoHangDao)
| Method | Chức Năng | Tham Số |
|--------|-----------|---------|
| `getAll()` | Lấy danh sách lô hàng | - |
| `getByMaSanPham()` | Lấy lô của một sản phẩm | maSanPham |
| `getAvailableForSale()` | Lô còn tồn và chưa hết hạn | maSanPham |
| `findById()` | Tìm lô hàng theo ID | maLoHang |
| `updateSoLuongTon()` | Cập nhật tồn kho | maLoHang, newSoLuong |
| `getExpiringSoon()` | Lấy lô sắp hết hạn (< 30 ngày) | - |

### 🗄️ Bảng SQL Liên Quan
```sql
LoHang (Lô Hàng)
├── MaLoHang (PK)
├── MaSanPham (FK) → SanPham
├── MaPhieuNhap (FK) → PhieuNhap
├── SoLo (nvarchar)
├── HanSuDung (Date)
├── SoLuongNhap (Int)
├── SoLuongTon (Int)
├── GiaNhap (Decimal)
├── TrangThai (nvarchar) - 'Đang bán', 'Hết hạn', 'Hết tồn'
├── NgayNhap (DateTime)
└── DaXoa (bit)

ChiTietHoaDon (Chi Tiết Hóa Đơn) - Liên kết
└── MaLoHang (FK) → LoHang
```

### 🎨 UI Component
- **LoHangPanel:** Giao diện quản lý lô hàng
  - Bảng danh sách lô hàng
  - Lọc theo sản phẩm
  - Cột hiển thị: Sản phẩm, Số Lô, Hạn SD, Tồn, Giá Nhập
  - Cảnh báo hết hạn sắp hết hạn
  - Sắp xếp theo thời gian hạn

---

## 6. Khách Hàng

### 📋 Chức Năng Chính
- **Xem danh sách khách hàng:** Hiển thị tất cả khách hàng
- **Thêm khách hàng mới:** Nhập thông tin cơ bản của khách hàng
- **Chỉnh sửa thông tin khách hàng:** Cập nhật tên, điện thoại, email, địa chỉ
- **Xóa khách hàng:** Xóa mềm (đánh dấu đã xóa)
- **Tìm kiếm khách hàng:** Tìm kiếm theo tên hoặc số điện thoại
- **Xem lịch sử mua hàng:** Danh sách các hóa đơn của khách hàng
- **Quản lý hồ sơ bệnh án:** Lưu trữ hồ sơ bệnh án nếu có

### 🛠️ Các Methods Quan Trọng (KhachHangDao)
| Method | Chức Năng | Tham Số |
|--------|-----------|---------|
| `getAll()` | Lấy danh sách khách hàng | - |
| `findById()` | Tìm khách hàng theo ID | maKhachHang |
| `searchByName()` | Tìm kiếm theo tên | keyword |
| `searchByPhone()` | Tìm kiếm theo số điện thoại | soDienThoai |
| `insert()` | Thêm khách hàng mới | khachHang |
| `update()` | Cập nhật thông tin khách hàng | khachHang |
| `delete()` | Xóa khách hàng (xóa mềm) | maKhachHang |

### 🗄️ Bảng SQL Liên Quan
```sql
KhachHang (Khách Hàng)
├── MaKhachHang (PK)
├── HoTen (nvarchar)
├── SoDienThoai (nvarchar)
├── Email (nvarchar)
├── DiaChi (nvarchar)
├── HoSoBenhAn (nvarchar)
├── DaXoa (bit)
└── NgayTao (DateTime)

HoaDonBan (Hóa Đơn Bán) - Liên kết
└── MaKhachHang (FK) → KhachHang
```

### 🎨 UI Component
- **KhachHangPanel:** Giao diện quản lý khách hàng
  - Bảng danh sách khách hàng
  - Nút Thêm, Sửa, Xóa
  - Ô tìm kiếm khách hàng
  - Dialog nhập thông tin khách hàng
  - Tab lịch sử mua hàng

---

## 7. Nhà Cung Cấp

### 📋 Chức Năng Chính
- **Xem danh sách nhà cung cấp:** Hiển thị tất cả nhà cung cấp
- **Thêm nhà cung cấp mới:** Nhập thông tin cơ bản
- **Chỉnh sửa thông tin nhà cung cấp:** Cập nhật tên, điện thoại, email, địa chỉ
- **Xóa nhà cung cấp:** Xóa mềm (đánh dấu đã xóa)
- **Tìm kiếm nhà cung cấp:** Tìm kiếm theo tên hoặc số điện thoại
- **Xem lịch sử cung cấp:** Danh sách các phiếu nhập từ nhà cung cấp
- **Đánh giá chất lượng:** Theo dõi hiệu suất cung cấp

### 🛠️ Các Methods Quan Trọng (NhaCungCapDao)
| Method | Chức Năng | Tham Số |
|--------|-----------|---------|
| `getAll()` | Lấy danh sách nhà cung cấp | - |
| `findById()` | Tìm nhà cung cấp theo ID | maNCC |
| `searchByName()` | Tìm kiếm theo tên | keyword |
| `insert()` | Thêm nhà cung cấp mới | nhaCungCap |
| `update()` | Cập nhật thông tin nhà cung cấp | nhaCungCap |
| `delete()` | Xóa nhà cung cấp (xóa mềm) | maNCC |

### 🗄️ Bảng SQL Liên Quan
```sql
NhaCungCap (Nhà Cung Cấp)
├── MaNCC (PK)
├── TenNCC (nvarchar)
├── SoDienThoai (nvarchar)
├── Email (nvarchar)
├── DiaChi (nvarchar)
├── DaXoa (bit)
└── NgayTao (DateTime)

PhieuNhap (Phiếu Nhập) - Liên kết
└── MaNCC (FK) → NhaCungCap
```

### 🎨 UI Component
- **NhaCungCapPanel:** Giao diện quản lý nhà cung cấp
  - Bảng danh sách nhà cung cấp
  - Nút Thêm, Sửa, Xóa
  - Ô tìm kiếm nhà cung cấp
  - Dialog nhập thông tin nhà cung cấp
  - Tab lịch sử cung cấp

---

## 8. Báo Cáo & Thống Kê

### 📊 Chức Năng Chính
- **Doanh thu hôm nay:** Tính tổng doanh thu của ngày hiện tại
- **Số hóa đơn hôm nay:** Đếm số lượng hóa đơn bán được trong ngày
- **Cảnh báo hết hạn:** Danh sách lô hàng sắp hết hạn (< 30 ngày)
- **Cảnh báo tồn kho thấp:** Danh sách sản phẩm có tồn kho dưới mức tối thiểu
- **Thống kê theo tháng:** Biểu đồ doanh thu theo tháng
- **Thống kê theo sản phẩm:** Sản phẩm bán chạy nhất, ít bán nhất
- **Báo cáo tồn kho:** Chi tiết tồn kho từng sản phẩm
- **Báo cáo chi phí:** Tính chi phí nhập và lợi nhuận bán

### 🛠️ Các Methods Quan Trọng (ThongKeDao)
| Method | Chức Năng | Tham Số |
|--------|-----------|---------|
| `getThongKeNgay()` | Thống kê doanh thu ngày hôm nay | - |
| `getThongKeThang()` | Thống kê doanh thu tháng | - |
| `getCanhBaoHetHan()` | Danh sách lô sắp hết hạn | - |
| `getCanhBaoTonKho()` | Danh sách sản phẩm tồn thấp | - |
| `getTopSanPhamBanChay()` | Top sản phẩm bán chạy | - |
| `getChiTietTonKho()` | Chi tiết tồn kho | - |

### 🗄️ Bảng SQL Liên Quan
```sql
HoaDonBan (Hóa Đơn Bán)
├── MaHoaDon (PK)
├── NgayBan (DateTime)
├── TongTien (Decimal)
└── ...

ChiTietHoaDon (Chi Tiết Hóa Đơn)
├── MaChiTiet (PK)
├── MaHoaDon (FK) → HoaDonBan
├── MaLoHang (FK) → LoHang
├── SoLuong (Int)
├── GiaBan (Decimal)
└── ThanhTien (Decimal)

LoHang (Lô Hàng)
├── MaLoHang (PK)
├── HanSuDung (Date)
├── SoLuongTon (Int)
└── GiaNhap (Decimal)
```

### 📈 Các Chỉ Số Chính (KPI)
| Chỉ Số | Mô Tả | Công Thức |
|--------|-------|----------|
| **Doanh Thu Ngày** | Tổng tiền từ HĐ bán hôm nay | SUM(HoaDonBan.TongTien) WHERE CAST(NgayBan AS DATE) = TODAY |
| **Số HĐ Ngày** | Số lượng HĐ bán hôm nay | COUNT(MaHoaDon) WHERE CAST(NgayBan AS DATE) = TODAY |
| **Tồn Kho Tổng** | Tổng số lượng tồn trong kho | SUM(LoHang.SoLuongTon) |
| **Giá Trị Tồn** | Giá trị toàn bộ tồn kho | SUM(LoHang.SoLuongTon * LoHang.GiaNhap) |
| **Sản Phẩm Tồn Thấp** | Số SP có tồn < mức tối thiểu | COUNT(SanPham) WHERE (SELECT SUM(SoLuongTon) FROM LoHang) < MucTonToiThieu |
| **Lô Sắp Hết Hạn** | Số lô hết hạn trong 30 ngày | COUNT(LoHang) WHERE DATEDIFF(DAY, TODAY, HanSuDung) BETWEEN 0 AND 30 |

### 🎨 UI Component
- **BaoCaoPanel:** Giao diện báo cáo và thống kê
  - Các thẻ hiển thị KPI chính
  - Biểu đồ doanh thu theo thời gian
  - Bảng cảnh báo hết hạn
  - Bảng cảnh báo tồn kho
  - Các nút xuất báo cáo (PDF, Excel)

---

## 9. Người Dùng

### 📋 Chức Năng Chính
- **Đăng nhập:** Xác thực tài khoản người dùng
- **Xem danh sách người dùng:** Hiển thị tất cả tài khoản người dùng
- **Thêm người dùng mới:** Tạo tài khoản mới với tên đăng nhập và mật khẩu
- **Chỉnh sửa thông tin người dùng:** Cập nhật tên, email, số điện thoại, vai trò
- **Xóa người dùng:** Xóa mềm (đánh dấu đã xóa)
- **Phân quyền:** Gán vai trò (Admin, Nhân viên bán hàng, Nhân viên nhập hàng, etc.)
- **Đặt lại mật khẩu:** Cấp lại mật khẩu mặc định hoặc thay đổi mật khẩu
- **Xem lịch sử hoạt động:** Theo dõi log hoạt động người dùng (nếu có)

### 🛠️ Các Methods Quan Trọng (NguoiDungDao)
| Method | Chức Năng | Tham Số |
|--------|-----------|---------|
| `login()` | Xác thực đăng nhập | tenDangNhap, matKhau |
| `getAll()` | Lấy danh sách người dùng | - |
| `findById()` | Tìm người dùng theo ID | maNguoiDung |
| `insert()` | Thêm người dùng mới | nguoiDung |
| `update()` | Cập nhật thông tin người dùng | nguoiDung |
| `delete()` | Xóa người dùng (xóa mềm) | maNguoiDung |
| `changePassword()` | Thay đổi mật khẩu | maNguoiDung, matKhauCu, matKhauMoi |
| `resetPassword()` | Đặt lại mật khẩu mặc định | maNguoiDung |

### 🗄️ Bảng SQL Liên Quan
```sql
NguoiDung (Người Dùng)
├── MaNguoiDung (PK)
├── TenDangNhap (nvarchar) - Unique
├── MatKhau (nvarchar) - Mã hóa
├── VaiTro (nvarchar) - 'Admin', 'Nhân viên', etc.
├── HoTen (nvarchar)
├── Email (nvarchar)
├── SoDienThoai (nvarchar)
├── DaXoa (bit)
└── NgayTao (DateTime)

HoaDonBan (Hóa Đơn Bán) - Liên kết
└── MaNguoiDung (FK) → NguoiDung

PhieuNhap (Phiếu Nhập) - Liên kết
└── MaNguoiDung (FK) → NguoiDung
```

### 🎨 UI Component
- **LoginFrame:** Màn hình đăng nhập
  - Ô nhập tên đăng nhập
  - Ô nhập mật khẩu
  - Nút Đăng nhập, Thoát
  - Icon và giao diện Flat Design

- **NguoiDungPanel:** Giao diện quản lý người dùng
  - Bảng danh sách người dùng
  - Nút Thêm, Sửa, Xóa
  - Ô tìm kiếm người dùng
  - Dialog nhập thông tin người dùng
  - Dropdown chọn vai trò

### 🔐 Bảo Mật
- **Mật khẩu:** Được mã hóa trước khi lưu vào CSDL
- **Xác thực:** Sử dụng Stored Procedure `sp_Login` để xác thực
- **Phân quyền:** Các module được kiểm soát dựa trên vai trò người dùng
- **Log hoạt động:** Ghi lại các thao tác quan trọng (tùy chọn)

### 👥 Các Vai Trò (Roles)
| Vai Trò | Quyền Hạn |
|---------|-----------|
| **Admin** | Toàn bộ quyền - Quản lý tất cả module, quản lý người dùng |
| **Nhân viên Bán Hàng** | Bán hàng, xem báo cáo, xem sản phẩm, xem khách hàng |
| **Nhân viên Nhập Hàng** | Nhập hàng, quản lý lô hàng, xem sản phẩm, xem nhà cung cấp |
| **Quản lý Kho** | Xem tồn kho, lô hàng, sản phẩm, báo cáo |
| **Quản lý Bán Hàng** | Bán hàng, xem khách hàng, xem báo cáo |

---

## Kiến Trúc Hệ Thống

### 🏗️ Cấu Trúc Lớp
```
app/
├── LoginFrame.java          → Màn hình đăng nhập
├── MainFrame.java           → Frame chính với Sidebar
└── JframeProduct.java       → Frame sản phẩm (nếu có)

panels/
├── DashboardPanel.java      → Panel Tổng quan
├── BanHangPanel.java        → Panel Bán hàng
├── NhapHangPanel.java       → Panel Nhập hàng
├── SanPhamPanel.java        → Panel Sản phẩm
├── LoHangPanel.java         → Panel Lô hàng
├── KhachHangPanel.java      → Panel Khách hàng
├── NhaCungCapPanel.java     → Panel Nhà cung cấp
├── BaoCaoPanel.java         → Panel Báo cáo & Thống kê
└── NguoiDungPanel.java      → Panel Quản lý người dùng

dao/
├── HoaDonBanDao.java        → DAO cho Hóa đơn bán
├── PhieuNhapDao.java        → DAO cho Phiếu nhập
├── SanPhamDao.java          → DAO cho Sản phẩm
├── LoHangDao.java           → DAO cho Lô hàng
├── KhachHangDao.java        → DAO cho Khách hàng
├── NhaCungCapDao.java       → DAO cho Nhà cung cấp
├── NguoiDungDao.java        → DAO cho Người dùng
├── ChiTietHoaDonDao.java    → DAO cho Chi tiết HĐ
├── ChiTietPhieuNhapDao.java → DAO cho Chi tiết Phiếu nhập
├── ProductDao.java          → DAO cho Product
└── ThongKeDao.java          → DAO cho Thống kê & Báo cáo

entity/
├── HoaDonBan.java           → Entity Hóa đơn bán
├── PhieuNhap.java           → Entity Phiếu nhập
├── SanPham.java             → Entity Sản phẩm
├── LoHang.java              → Entity Lô hàng
├── KhachHang.java           → Entity Khách hàng
├── NhaCungCap.java          → Entity Nhà cung cấp
├── NguoiDung.java           → Entity Người dùng
├── ChiTietHoaDon.java       → Entity Chi tiết HĐ
├── ChiTietPhieuNhap.java    → Entity Chi tiết Phiếu nhập
└── Product.java             → Entity Product

common/
├── ConnectDB.java           → Kết nối CSDL
├── ColorScheme.java         → Sơ đồ màu
├── IconHelper.java          → Trợ giúp Icon
└── ...
```

### 🔄 Luồng Xử Lý Chính

#### Luồng Bán Hàng (FEFO)
```
1. Tạo HoaDonBan mới → createHoaDon()
   ↓
2. Chọn sản phẩm & số lượng
   ↓
3. Gọi sellProductFEFO()
   ├── Tìm LoHang có sẵn & chưa hết hạn
   ├── Sắp xếp theo FEFO (hạn soonest first)
   ├── Trừ tồn từ LoHang
   └── Tạo ChiTietHoaDon
   ↓
4. Xác nhận bán hàng
   ↓
5. Tính toán TongTien HoaDonBan
```

#### Luồng Nhập Hàng
```
1. Tạo PhieuNhap mới → createPhieuNhap()
   ↓
2. Chọn nhà cung cấp & sản phẩm
   ↓
3. Nhập thông tin lô → addItemBatch()
   ├── Số lô, hạn SD, giá nhập, số lượng
   ├── Tạo ChiTietPhieuNhap
   └── Cập nhật TongTien PhieuNhap
   ↓
4. Xác nhận nhập hàng
   ↓
5. Tạo LoHang mới từ ChiTietPhieuNhap
```

### 📊 Quan Hệ Dữ Liệu (ER Diagram)
```
                    ┌─────────────┐
                    │  NguoiDung  │
                    └──────┬──────┘
                           │
                ┌──────────┼──────────┐
                │                     │
         ┌──────▼──────┐      ┌──────▼──────┐
         │ HoaDonBan   │      │ PhieuNhap   │
         └──────┬──────┘      └──────┬──────┘
                │                     │
                │         ┌───────────┼───────────┐
                │         │           │           │
         ┌──────▼──────────────┐  ┌──▼──┐     ┌──▼─────────┐
         │ ChiTietHoaDon       │  │NCC  │     │ ChiTietPhiếu│
         └──────┬──────────────┘  └─────┘     └──┬──────────┘
                │                              │
         ┌──────▼──────┐                ┌─────▼──────┐
         │  LoHang     │                │ LoHang     │
         └──────┬──────┘                └─────┬──────┘
                │                            │
                └─────────┬──────────────────┘
                          │
                   ┌──────▼──────┐
                   │  SanPham    │
                   └─────────────┘

         ┌──────────────┐
         │ KhachHang    │ ◄─── HoaDonBan
         └──────────────┘
```

---

## Công Nghệ & Thư Viện Sử Dụng

| Thành Phần | Công Nghệ | Phiên Bản |
|-----------|-----------|----------|
| **Ngôn Ngữ** | Java | 11+ |
| **GUI Framework** | Java Swing | Built-in |
| **Look & Feel** | FlatLaf | Latest |
| **CSDL** | SQL Server | 2016+ |
| **Build Tool** | Maven | 3.6+ |
| **JDBC Driver** | SQL Server JDBC | 9.2+ |
| **Icon Library** | Material Design Icons | Via FlatLaf |

---

## Quy Ước Đặt Tên

### Tên Lớp
- **Entity:** `SanPham.java` (Tên chung)
- **DAO:** `SanPhamDao.java` (Entity + Dao)
- **Panel:** `SanPhamPanel.java` (Entity + Panel)
- **Frame:** `LoginFrame.java`, `MainFrame.java`

### Tên Method
- **Truy Vấn:** `getAll()`, `findById()`, `searchByName()`
- **Thêm:** `insert()`
- **Sửa:** `update()`
- **Xóa:** `delete()`
- **Stored Procedure:** `sp_EntityName_Action()` (VD: `sp_HoaDonBan_Create()`)

### Tên Biến
- **Khóa chính:** `ma[EntityName]` (VD: `maSanPham`)
- **Khóa ngoại:** `ma[EntityName]` (VD: `maNguoiDung`)
- **Biến tạm:** `camelCase` (VD: `soLuongTon`)
- **Hằng số:** `UPPER_SNAKE_CASE` (VD: `MAX_RETRY`)

---

## Yêu Cầu Hệ Thống

### Phần Cứng
- **RAM:** Tối thiểu 2GB, khuyến nghị 4GB+
- **Storage:** 500MB cho ứng dụng + CSDL
- **Processor:** Dual-core 2.0 GHz trở lên

### Phần Mềm
- **OS:** Windows 7+, macOS, Linux
- **Java JDK:** Version 11 hoặc cao hơn
- **SQL Server:** 2016 hoặc cao hơn
- **Maven:** 3.6 hoặc cao hơn (nếu compile từ source)

### Network
- Kết nối đến SQL Server (Local hoặc Remote)
- Cấu hình connection string trong `ConnectDB.java`

---

## Hướng Dẫn Triển Khai

### 1. Chuẩn Bị Cơ Sở Dữ Liệu
```sql
-- Tạo database MEPHAR (nếu chưa có)
CREATE DATABASE MEPHAR;
GO

-- Chạy script database (cung cấp riêng)
-- Để tạo các bảng: NguoiDung, SanPham, LoHang, HoaDonBan, etc.
```

### 2. Cấu Hình Kết Nối
Sửa file `common/ConnectDB.java`:
```java
public static Connection getCon() {
    String url = "jdbc:sqlserver://localhost:1433;databaseName=MEPHAR";
    String user = "sa";
    String password = "your_password";
    // ...
}
```

### 3. Biên Dịch & Chạy
```bash
# Compile với Maven
mvn clean compile

# Chạy ứng dụng
mvn javafx:run
# Hoặc chạy file JAR (nếu đã build)
java -jar MEPHAR.jar
```

### 4. Đăng Nhập Mặc Định
Sử dụng tài khoản admin mặc định (cung cấp riêng):
- **Username:** admin
- **Password:** password123

---

## Ghi Chú & Lưu Ý

⚠️ **Quan Trọng:**
- Tất cả xóa dữ liệu đều là **xóa mềm** (soft delete) - chỉ đánh dấu `DaXoa = 1`
- Bán hàng phải tuân theo quy tắc **FEFO** (First Expired, First Out)
- Kiểm tra tồn kho và hạn sử dụng **trước khi bán**
- Thường xuyên **backup CSDL** để tránh mất dữ liệu
- Mật khẩu người dùng phải được **mã hóa** trước khi lưu

💡 **Tối Ưu Hóa:**
- Sử dụng **pagination** cho danh sách lớn (>1000 bản ghi)
- Implement **caching** để tăng tốc độ truy vấn
- Tối ưu **các index trên CSDL** cho các cột tìm kiếm
- Sử dụng **connection pooling** để quản lý kết nối tốt hơn

🔧 **Bảo Trì:**
- Review **log lỗi** định kỳ
- Update **thư viện phụ thuộc** khi có version mới
- Test **tất cả các chức năng** sau khi update
- Giữ **tài liệu cập nhật** theo thay đổi mã nguồn

---

## Liên Hệ & Hỗ Trợ

**Lưu ý:** Đây là tài liệu sơ bộ và có thể được cập nhật dựa trên phát triển thực tế của project.

Để báo cáo lỗi hoặc yêu cầu tính năng mới, vui lòng liên hệ đội phát triển.

---

**Tài liệu được tạo ngày:** Tháng 3, 2026  
**Phiên bản:** 2.0  
**Trạng thái:** Final
