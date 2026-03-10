# Đánh giá Kiến trúc Phân quyền (RBAC Architecture Review)

Tài liệu này phân tích kiến trúc phân quyền dựa trên vai trò (Role-Based Access Control) vừa được triển khai cho hệ thống MEPHAR.

## 1. KIẾN TRÚC PHÂN TRẬT TỰ (ARCHITECTURE DESIGN)

Hệ thống sử dụng một lớp tiện ích tập trung mang tên `PermissionManager` nằm tại gói `utils`.

### Thành phần cốt lõi:
- **Enum Role**: Định nghĩa các vai trò chuẩn trong hệ thống (`ADMIN`, `QUANLY`, `NHANVIEN`). Mỗi Role đi kèm với một nhãn (label) khớp với dữ liệu trong Database.
- **Mapping (Action-to-Roles)**: Sử dụng một `Map<String, List<Role>>` tĩnh (static context) để định nghĩa quyền truy cập. Mỗi chức năng (action identifier) như `nhaphang` sẽ được gán cho một danh sách các vai trò được phép truy cập.

```java
// Ví dụ cấu hình trong PermissionManager
static {
    permissions.put("nhaphang", Arrays.asList(Role.ADMIN, Role.QUANLY));
    permissions.put("nguoidung", Arrays.asList(Role.ADMIN));
}
```

### Cách thức hoạt động:
Khi UI yêu cầu hiển thị một chức năng, nó sẽ gọi `PermissionManager.hasAccess(vaiTroString, actionId)`. Lớp này sẽ chuyển đổi chuỗi `vaiTroString` sang Enum `Role` tương ứng và kiểm tra xem vai trò đó có nằm trong danh sách được phép của `actionId` hay không.

## 2. ĐÁNH GIÁ MỨC ĐỘ CLEAN CODE (SEPARATION OF CONCERNS)

Kiến trúc mới đã đạt được sự tách biệt hoàn toàn giữa **Logic nghiệp vụ** và **Giao diện người dùng**:

- **Tách biệt logic**: `SidebarPanel` giờ đây không còn chứa bất kỳ câu lệnh `if-else` nào liên quan đến logic vai trò cụ thể (ví dụ: `if (role.equals("NhanVien"))`). 
- **Tính declarative**: UI chỉ đơn thuần khai báo những gì nó muốn hiển thị và cung cấp định danh hành động. Quyết định hiển thị hay không phụ thuộc hoàn toàn vào `PermissionManager`.

```java
// Logic sạch trong SidebarPanel
addMenuItemIfAllowed(menuPanel, "Nhập hàng", "nhaphang", role);

private void addMenuItemIfAllowed(JPanel parent, String text, String action, String role) {
    if (utils.PermissionManager.hasAccess(role, action)) {
        addMenuItem(parent, text, action);
    }
}
```

## 3. PHÂN TÍCH RỦI RO XUNG ĐỘT (CONFLICT ANALYSIS)

### Tính độc lập (Isolation):
Khi thêm một chức năng mới chỉ dành cho **Admin**, lập trình viên **KHÔNG** cần phải sửa bất kỳ dòng code nào liên quan đến các Panel của Nhân viên hay Quản lý. 
- Mọi cấu hình đều nằm tập trung tại `PermissionManager`.
- Các role không có quyền sẽ tự động được hệ thống bỏ qua mà không làm ảnh hưởng đến bố cục (nhờ `BoxLayout` tự động co giãn).

### Khả năng mở rộng (Scalability):
Nếu tương lai cần thêm role mới như `KETOAN`:
1. Thêm `KETOAN` vào Enum `Role`.
2. Cập nhật phương thức `fromString` để nhận diện label.
3. Thêm role này vào các danh sách quyền trong khối `static`.
=> Toàn bộ hệ thống sẽ tự động cập nhật mà không cần thay đổi code tại hàng chục file UI khác nhau.

## 4. HƯỚNG DẪN THÊM CHỨC NĂNG MỚI

Để thêm một menu mới (Ví dụ: "Kho hàng") và cấp quyền cho nó:

**Bước 1: Cấu hình phân quyền**
Mở `utils/PermissionManager.java`, thêm định danh hành động và cấp quyền cho các vai trò mong muốn:
```java
permissions.put("khohang", Arrays.asList(Role.ADMIN, Role.QUANLY));
```

**Bước 2: Thêm menu vào UI**
Mở `panels/SidebarPanel.java`, gọi phương thức `addMenuItemIfAllowed` trong hàm `initialize`:
```java
addMenuItemIfAllowed(menuPanel, "Kho hàng", "khohang", role);
```

**Kết quả**: Menu "Kho hàng" sẽ tự động xuất hiện với Admin/Quản lý và biến mất hoàn toàn (không để lại khoảng trống) đối với Nhân viên.
