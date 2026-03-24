# 🏭 Tính Năng Tiếp Theo: Quản Lý Nhà Cung Cấp (Supplier Management)

> **Ngày tạo**: 2026-03-24
> **Ưu tiên**: 🔴 Cao — Module sidebar đã có button, database + backend đã sẵn sàng, chỉ cần UI
> **Trạng thái hiện tại**: Placeholder ("Đang phát triển...")

---

## 1. Tổng Quan

Module **Quản Lý Nhà Cung Cấp** cho phép Admin quản lý danh sách nhà cung cấp thuốc — thêm/sửa/vô hiệu hóa NCC, tìm kiếm, phân trang. Module này đã có **đầy đủ backend** (Entity → Repository → Service → ServiceFactory), chỉ cần tạo **Presentation layer** (SupplierPanel).

### Những gì ĐÃ CÓ (đã code xong, KHÔNG SỬA):

| Layer | File | Trạng thái |
|-------|------|-----------|
| **Database** | `database/09_nha_cung_cap.sql` | ✅ Bảng `NhaCungCap` + FK + 5 seed |
| **Entity** | `domain/entity/Supplier.java` | ✅ 7 fields: maNCC, tenNCC, soDT, diaChi, email, trangThai, ngayTao |
| **Repository Interface** | `domain/repository/ISupplierRepository.java` | ✅ insert, update, softDelete, getById, getAll, getActive, search |
| **Repository Impl** | `infrastructure/repository/SupplierRepositoryImpl.java` | ✅ SQL Server + PreparedStatement + setNString |
| **Service Interface** | `service/ISupplierService.java` | ✅ add, update, delete, getById, getAll, getActive, search |
| **Service Impl** | `service/impl/SupplierServiceImpl.java` | ✅ Validation + delegate to repo |
| **DI Container** | `common/ServiceFactory.java` | ✅ `getSupplierService()` đã có |
| **MainFrame** | `presentation/MainFrame.java` | ✅ Sidebar button + card "supplier" đã wired |

### Những gì CẦN LÀM (chỉ Presentation layer):

| File cần tạo | Mô tả |
|--------------|-------|
| `presentation/panel/SupplierPanel.java` | Panel chính: Form + Table + Pagination |
| Sửa `MainFrame.java` dòng 140 | Thay `createPlaceholder(...)` bằng `new SupplierPanel()` |

---

## 2. Yêu Cầu Chức Năng

### 2.1 Bảng Dữ Liệu (Table)

| Cột | DB Column | Sortable | Alignment | Ghi chú |
|-----|-----------|----------|-----------|---------|
| STT | — | ❌ | Center | Auto-increment theo trang |
| Mã NCC | MaNCC | ✅ | Center | PK |
| Tên NCC | TenNCC | ✅ | Left | NVARCHAR — dùng `setNString` |
| Số ĐT | SoDT | ❌ | Center | VARCHAR |
| Địa chỉ | DiaChi | ✅ | Left | NVARCHAR |
| Email | Email | ✅ | Left | VARCHAR |
| Trạng thái | TrangThai | ✅ | Center | BIT: 1="Hoạt động" (xanh), 0="Ngừng HT" (đỏ) |
| Ngày tạo | NgayTao | ✅ | Center | DATETIME → format dd/MM/yyyy |

### 2.2 Form Nhập Liệu (Left side)

| Field | Type | Editable | Ghi chú |
|-------|------|----------|---------|
| Mã NCC | JTextField | ❌ (read-only, bg=NEUTRAL) | Auto-fill khi chọn row |
| Tên NCC | JTextField | ✅ | **Required** — validate trước khi save |
| Số ĐT | JTextField | ✅ | Optional — chỉ cho nhập số + dấu cách |
| Địa chỉ | JTextField | ✅ | Optional |
| Email | JTextField | ✅ | Optional — validate format email nếu có nhập |
| Trạng thái | JComboBox | ✅ | "Hoạt động" / "Ngừng hợp tác" |
| Ngày tạo | JTextField | ❌ (read-only) | Auto-fill khi chọn row |

### 2.3 CRUD Operations

| Action | Button | Màu | Logic |
|--------|--------|-----|-------|
| **Thêm** | "Thêm NCC" | `AppColors.SUCCESS` (#28A745) | Validate → `ServiceFactory.getSupplierService().add(supplier)` → reload |
| **Cập nhật** | "Cập Nhật" | Teal `new Color(0x17, 0xA2, 0xB8)` | Validate → confirm dialog → `service.update(supplier)` → reload |
| **Xóa** (soft delete) | "Ngừng HT" | `AppColors.DANGER` | Confirm → `service.delete(maNCC)` → chỉ SET TrangThai=0 |
| **Làm mới** | "Làm Mới" | `AppColors.SECONDARY` | Clear form, deselect |

> **⚠️ QUAN TRỌNG**: Xóa NCC là **SOFT DELETE** (TrangThai = 0), KHÔNG xóa vật lý. Vì NCC đã liên kết với PhieuNhap qua FK.

### 2.4 Tìm Kiếm + Lọc

- **Tìm kiếm**: Debounce 400ms, search theo TenNCC, SoDT, DiaChi, Email
- **Lọc trạng thái**: ComboBox ["Tất cả", "Hoạt động", "Ngừng hợp tác"]
- **Phân trang**: Server-side, 20 rows/page, SQL `OFFSET/FETCH NEXT`

### 2.5 Phân Quyền

- **Admin**: Xem + CRUD đầy đủ
- **Nhân viên**: Xem danh sách NCC (read-only), KHÔNG có button Thêm/Sửa/Xóa
- Kiểm tra bằng `Session.isAdmin()`

---

## 3. System Design — PHẢI TUÂN THỦ

> [!CAUTION]
> **Đọc kỹ phần này trước khi code.** Nếu không tuân thủ, code mới sẽ xung đột với code cũ.

### 3.1 Kiến Trúc Phân Lớp

```
┌──────────────────────────────────────────────┐
│           presentation/ (UI Layer)           │
│  SupplierPanel.java ← CHỈ Swing, KHÔNG SQL  │
│  Gọi Service qua ServiceFactory              │
├──────────────────────────────────────────────┤
│             service/ (Business)              │
│  ISupplierService ← Interface                │
│  SupplierServiceImpl ← Validation + Logic    │
├──────────────────────────────────────────────┤
│          domain/ (Entity + Repo IF)          │
│  Supplier.java ← POJO                       │
│  ISupplierRepository ← Interface             │
├──────────────────────────────────────────────┤
│     infrastructure/ (Database Access)        │
│  SupplierRepositoryImpl ← SQL + JDBC         │
│  DatabaseHelper ← Connection Pool (HikariCP) │
└──────────────────────────────────────────────┘
```

**Quy tắc vàng:**
- `presentation/` KHÔNG import `java.sql.*` — gọi Service qua `ServiceFactory.getSupplierService()`
- `service/` KHÔNG import `javax.swing.*`
- Tất cả NVARCHAR phải dùng `setNString()` / `getNString()` (tiếng Việt Unicode)

### 3.2 Design System (AppColors)

File tham chiếu: [AppColors.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/common/AppColors.java)

```java
// Palette PHẢI sử dụng:
AppColors.PRIMARY          // #0056B3 — Header, active state
AppColors.PRIMARY_VERY_LIGHT // #E3EFFA — Selection highlight
AppColors.TEXT_PRIMARY     // #212529 — Text chính
AppColors.TEXT_SECONDARY   // #6C757D — Label, hint
AppColors.NEUTRAL          // #F8F9FA — Background
AppColors.NEUTRAL_DARK     // #E9ECEF — Border, divider
AppColors.NEUTRAL_DARKER   // #DEE2E6 — Field border
AppColors.DANGER           // #D32F2F — Delete button
AppColors.SUCCESS          // #28A745 — Add button, status "Hoạt động"
AppColors.TABLE_HEADER_BG  // = PRIMARY
AppColors.TABLE_HEADER_FG  // = WHITE
AppColors.TABLE_ROW_ALT    // #F0F4F8 — Alternating row
```

### 3.3 UI Layout — Copy CHÍNH XÁC từ InventoryPanel

File tham chiếu: [InventoryPanel.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/presentation/panel/InventoryPanel.java)

#### Top Bar
```java
JPanel topBar = new JPanel(new BorderLayout(12, 0));
topBar.setBackground(Color.WHITE);
topBar.setBorder(new EmptyBorder(16, 24, 16, 24));

JLabel lblTitle = new JLabel("Quản Lý Nhà Cung Cấp");
lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));   // ← ĐÚNG 18, không 20
lblTitle.setForeground(AppColors.TEXT_PRIMARY);           // ← ĐÚNG TEXT_PRIMARY, không PRIMARY
```

#### JSplitPane
```java
JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
splitPane.setDividerLocation(280);  // Left form width
splitPane.setDividerSize(1);        // ← 1px divider
splitPane.setBorder(null);
```

#### Form Panel
```java
formPanel.setBorder(new EmptyBorder(20, 24, 20, 24));  // ← padding 20/24
// Field height = 34px, spacing = 12px
field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
field.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(AppColors.NEUTRAL_DARKER, 1),
    new EmptyBorder(0, 10, 0, 10)
));
// Button grid 2x2
JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
```

#### Table Style
```java
table.setRowHeight(32);
table.setShowGrid(false);                                    // ← KHÔNG có grid
table.setIntercellSpacing(new Dimension(0, 0));
table.setSelectionBackground(AppColors.PRIMARY_VERY_LIGHT);
table.setSelectionForeground(AppColors.TEXT_PRIMARY);
table.setAutoCreateRowSorter(false);                         // ← Sort tự handle

JTableHeader header = table.getTableHeader();
header.setFont(new Font("Segoe UI", Font.BOLD, 13));        // ← Bold 13
header.setPreferredSize(new Dimension(0, 36));               // ← Height 36
```

#### Pagination Bar
```java
btnFirst = createPageNavButton("|< Đầu");   // ← text labels, KHÔNG unicode arrows
btnPrev  = createPageNavButton("< Trước");
btnNext  = createPageNavButton("Sau >");
btnLast  = createPageNavButton("Cuối >|");
```

#### ComponentListener (FIX VIEWPORT BUG)
```java
// PHẢI CÓ — fix viewport không fill lần đầu
addComponentListener(new ComponentAdapter() {
    @Override
    public void componentShown(ComponentEvent e) {
        loadPage(currentPage);
    }
});
```

### 3.4 Sort Pattern

```java
// State:
private int sortColumnIndex = 1;    // default: MaNCC
private boolean sortAsc = true;     // default: ASC

// Column → SQL mapping:
private static final String[] SORT_SQL = {
    null,           // 0: STT
    "MaNCC",        // 1: Mã NCC
    "TenNCC",       // 2: Tên NCC
    null,           // 3: Số ĐT (optional)
    "DiaChi",       // 4: Địa chỉ
    "Email",        // 5: Email
    "TrangThai",    // 6: Trạng thái
    "NgayTao"       // 7: Ngày tạo
};

// Header click handler:
header.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        int col = table.columnAtPoint(e.getPoint());
        if (col >= 0 && col < SORT_SQL.length && SORT_SQL[col] != null) {
            if (sortColumnIndex == col) {
                sortAsc = !sortAsc;
            } else {
                sortColumnIndex = col;
                sortAsc = true;
            }
            currentPage = 1;
            loadPage(1);
        }
    }
});

// Build ORDER BY:
private String buildOrderBy() {
    if (sortColumnIndex >= 0 && sortColumnIndex < SORT_SQL.length 
            && SORT_SQL[sortColumnIndex] != null) {
        return SORT_SQL[sortColumnIndex] + (sortAsc ? " ASC" : " DESC");
    }
    return "MaNCC ASC";
}
```

### 3.5 Unified Cell Renderer Pattern

```java
// Một renderer duy nhất xử lý: alt-row + alignment + status coloring
private DefaultTableCellRenderer createCellRenderer() {
    return new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
            if (!sel) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
            }
            // Status column coloring
            if (col == STATUS_COL_INDEX && val != null) {
                String status = val.toString();
                if (!sel) {
                    if ("Hoạt động".equals(status)) {
                        c.setForeground(AppColors.SUCCESS);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setForeground(AppColors.DANGER);
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
            } else if (!sel) {
                c.setForeground(AppColors.TEXT_PRIMARY);
            }
            // Alignment per column
            setHorizontalAlignment(...);
            return c;
        }
    };
}
```

---

## 4. Tích Hợp Vào MainFrame

> [!IMPORTANT]
> Chỉ sửa **1 dòng duy nhất** trong MainFrame.java

```diff
- contentPanel.add(createPlaceholder("Nhà Cung Cấp", "Quản lý nhà cung cấp — Đang phát triển..."), "supplier");
+ contentPanel.add(new presentation.panel.SupplierPanel(), "supplier");
```

Dòng này ở [MainFrame.java dòng 140](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/presentation/MainFrame.java#L140).

---

## 5. Approach Code — Có 2 Cách

### Cách 1: Gọi trực tiếp SQL (như InventoryPanel) — ĐƠN GIẢN
- Panel tự query SQL trong `loadPage()` — có phân trang server-side
- Ưu: nhanh, 1 file xong
- Nhược: vi phạm kiến trúc (presentation → infrastructure trực tiếp)

### Cách 2: Dùng Service (như ProductPanel) — CHUẨN KIẾN TRÚC
- Panel → `ServiceFactory.getSupplierService()` → Service → Repository
- Cần thêm method phân trang vào `ISupplierService` + `SupplierServiceImpl` + `ISupplierRepository` + `SupplierRepositoryImpl`
- Ưu: đúng kiến trúc Clean Architecture
- Nhược: cần sửa 4 file backend

> **Khuyến nghị**: Dùng **Cách 1** trước cho nhanh (consistency với InventoryPanel đang dùng cách này), rồi refactor sau nếu cần.

---

## 6. SQL Query Mẫu

### Count
```sql
SELECT COUNT(*) FROM NhaCungCap WHERE 1=1
  AND (TenNCC LIKE ? OR SoDT LIKE ? OR DiaChi LIKE ? OR Email LIKE ?)  -- search
  AND TrangThai = ?  -- filter (nếu không phải "Tất cả")
```

### Data (phân trang)
```sql
SELECT MaNCC, TenNCC, SoDT, DiaChi, Email, TrangThai, NgayTao
FROM NhaCungCap
WHERE 1=1
  AND (TenNCC LIKE ? OR SoDT LIKE ? OR DiaChi LIKE ? OR Email LIKE ?)
  AND TrangThai = ?
ORDER BY {buildOrderBy()}
OFFSET ? ROWS FETCH NEXT 20 ROWS ONLY
```

---

## 7. Checklist Hoàn Thành

- [ ] Tạo `presentation/panel/SupplierPanel.java`
- [ ] Top bar: title 18pt + filter (ComboBox trạng thái + search box 200px)
- [ ] JSplitPane: dividerLocation=280, dividerSize=1
- [ ] Form panel: 7 fields (MaNCC readonly, TenNCC, SoDT, DiaChi, Email, TrangThai combo, NgayTao readonly)
- [ ] Button grid 2x2: Thêm (SUCCESS) / Cập Nhật (Teal) / Ngừng HT (DANGER) / Làm Mới (SECONDARY)
- [ ] Table: 8 columns, showGrid=false, intercellSpacing(0,0), unified renderer
- [ ] Phân trang server-side 20 rows/page
- [ ] Sort via header click
- [ ] Debounce search 400ms
- [ ] ComponentListener.componentShown → loadPage
- [ ] AdjustRowHeight dynamic
- [ ] Phân quyền: Admin CRUD, Employee read-only
- [ ] Sửa MainFrame.java dòng 140
- [ ] `mvn compile` thành công
- [ ] Test: thêm NCC → hiển thị trong bảng
- [ ] Test: sửa NCC → reload đúng
- [ ] Test: ngừng hợp tác → TrangThai = 0, hiển thị "Ngừng HT" đỏ
- [ ] Test: tìm kiếm + lọc + phân trang

---

## 8. ⚠️ Bẫy Chí Tử — Tránh Những Lỗi Này

### Bẫy 1: Quên `setNString()` cho Unicode
```java
// ❌ SAI — mất tiếng Việt
ps.setString(1, supplier.getTenNCC());

// ✅ ĐÚNG
ps.setNString(1, supplier.getTenNCC());
```

### Bẫy 2: Quên `componentShown` 
Viewport không fill đầy lần đầu vào tab. **PHẢI** có ComponentListener.

### Bẫy 3: Xóa vật lý NCC đang có phiếu nhập
```java
// ❌ SAI — FK constraint error
DELETE FROM NhaCungCap WHERE MaNCC = ?

// ✅ ĐÚNG — soft delete
UPDATE NhaCungCap SET TrangThai = 0 WHERE MaNCC = ?
```

### Bẫy 4: Dùng `BorderLayout.WEST` cho form
```java
// ❌ SAI — Form không fill viewport height
center.add(formPanel, BorderLayout.WEST);

// ✅ ĐÚNG — JSplitPane
JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
splitPane.setDividerLocation(280);
```

### Bẫy 5: showGrid(true) + intercellSpacing default
```java
// ❌ SAI — Không match ProductPanel
table.setShowGrid(true);

// ✅ ĐÚNG
table.setShowGrid(false);
table.setIntercellSpacing(new Dimension(0, 0));
table.setSelectionBackground(AppColors.PRIMARY_VERY_LIGHT);
```

### Bẫy 6: Quên reset currentPage khi sort
```java
// ❌ SAI — sort nhưng vẫn ở trang cũ
loadPage(currentPage);

// ✅ ĐÚNG
currentPage = 1;
loadPage(1);
```

---

## 9. File Tham Chiếu (Copy Pattern Từ Đây)

| Pattern cần copy | File tham chiếu | Ghi chú |
|-----------------|----------------|---------|
| **Layout tổng thể** | [InventoryPanel.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/presentation/panel/InventoryPanel.java) | JSplitPane + topBar + form + table |
| **Design tokens** | [AppColors.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/common/AppColors.java) | Palette chính thức |
| **DI pattern** | [ServiceFactory.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/common/ServiceFactory.java) | `getSupplierService()` đã có |
| **Entity** | [Supplier.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/domain/entity/Supplier.java) | 7 fields |
| **DB Schema** | [09_nha_cung_cap.sql](file:///d:/eproject/eProject-StoreBanThuoc/database/09_nha_cung_cap.sql) | Bảng + FK + seed |
| **MainFrame** | [MainFrame.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/presentation/MainFrame.java) | Dòng 140 cần sửa |
| **Session/Auth** | `common/Session.java` | `Session.isAdmin()` |

---

> **Tóm tắt**: Backend **100% sẵn sàng**. Chỉ cần tạo **1 file mới** (`SupplierPanel.java`) + sửa **1 dòng** trong `MainFrame.java`. Copy pattern từ `InventoryPanel.java`, dùng `ServiceFactory.getSupplierService()` hoặc query SQL trực tiếp đều được.
