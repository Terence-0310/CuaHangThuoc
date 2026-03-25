# Module Trả Hàng NCC + Hủy Hàng + Kiểm Kho

## Background

Hệ thống quản lý nhà thuốc hiện tại chưa xử lý được nghiệp vụ:
- Hàng nhập về bị lỗi → cần trả NCC
- Hàng hết hạn / hư hỏng → cần hủy, ghi nhận tổn thất
- Đối chiếu tồn kho thực tế vs hệ thống → cần kiểm kho

**Database hiện tại**: `PhieuNhap` (MaPN, MaND, MaNCC, NgayNhap, TongTien), `LoHang` (MaLo, MaSP, MaPN, SoLo, HanSuDung, SoLuong, GiaNhap), `NhaCungCap` (MaNCC, TenNCC, SoDT, DiaChi).

---

## Proposed Changes

### Database Schema

#### [NEW] [11_return_destroy_stockcheck.sql](file:///d:/eproject/eProject-StoreBanThuoc/database/11_return_destroy_stockcheck.sql)

6 bảng mới (3 master + 3 detail):

| Bảng | Mục đích | FK chính |
|------|----------|----------|
| `PhieuTraHang` | Header phiếu trả NCC | → PhieuNhap, NhaCungCap, NguoiDung |
| `ChiTietTraHang` | Detail lô hàng trả | → PhieuTraHang, LoHang, SanPham |
| `PhieuHuyHang` | Header phiếu hủy | → NguoiDung |
| `ChiTietHuyHang` | Detail lô hàng hủy | → PhieuHuyHang, LoHang, SanPham |
| `PhieuKiemKho` | Header phiếu kiểm kho | → NguoiDung |
| `ChiTietKiemKho` | Detail kiểm từng lô | → PhieuKiemKho, LoHang, SanPham |

Script sử dụng `IF NOT EXISTS` → idempotent, safe to re-run.

---

### Auto Migration

#### [MODIFY] [DatabaseHelper.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/infrastructure/database/DatabaseHelper.java)

Thêm `ensureAdvancedTables()` method: khi `getConnection()` lần đầu, tự chạy `CREATE TABLE IF NOT EXISTS` cho 6 bảng. Pattern tương tự `Session.ensureColumnExists()`.

---

### Module 1: Trả Hàng NCC

#### [NEW] [ReturnToSupplierPanel.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/presentation/panel/ReturnToSupplierPanel.java)

**Layout**: JSplitPane (Form trái + Table phải)

**Form trái**:
- ComboBox chọn NCC → filter PhieuNhap theo NCC
- ComboBox chọn PhieuNhap → load LoHang trong phiếu
- Bảng lô hàng (TenSP, SoLo, SoLuong, GiaNhap, HSD)
- Spinner SoLuongTra + TextField LyDo + Nút "Thêm vào DS Trả"
- Bảng "DS chuẩn bị trả" + Nút "Tạo Phiếu Trả"

**Table phải**: Lịch sử phiếu trả (MaPTH, NCC, NgayTra, TongTien, TrangThai) + Pagination

**Business Logic**:
1. `SoLuongTra <= LoHang.SoLuong` (validate)
2. Tạo `PhieuTraHang` + `ChiTietTraHang`
3. `UPDATE LoHang SET SoLuong -= @SoLuongTra`
4. Trạng thái: Chờ xử lý → (Admin confirm) → Đã hoàn tiền / Đã đổi hàng

---

### Module 2: Hủy Hàng

#### [NEW] [DestroyPanel.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/presentation/panel/DestroyPanel.java)

**Layout**: JSplitPane (Form trái + Table phải)

**Form trái**:
- Filter: ComboBox (Hết hạn / Sắp hết hạn / Tất cả)
- Bảng lô hàng matching filter
- Spinner SoLuongHuy + ComboBox LyDo (Hết hạn / Hư hỏng / Biến chất / Khác)
- Nút "Thêm vào DS Hủy" + Bảng DS chuẩn bị hủy + Nút "Tạo Phiếu Hủy"

**Table phải**: Lịch sử phiếu hủy (MaPHH, NgayHuy, TongTonThat, LyDo) + Pagination

**Business Logic**:
1. `SoLuongHuy <= LoHang.SoLuong`
2. Tạo `PhieuHuyHang` + `ChiTietHuyHang`
3. `UPDATE LoHang SET SoLuong -= @SoLuongHuy`
4. Tổn thất = SoLuongHuy × GiaNhap

---

### Module 3: Kiểm Kho

#### [NEW] [StockCheckPanel.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/presentation/panel/StockCheckPanel.java)

**Layout**: Top (controls) + Center (table) + Bottom (summary)

**Top**: Nút "Tạo Phiếu Kiểm Mới" + ComboBox status filter

**Center**: Bảng chính khi đang kiểm: TenSP, SoLo, HSD, TonHeThong, **TonThucTe** (editable spinner), ChenhLech (auto = ThucTe - HeThong)

**Bottom**: Summary (Tổng lô kiểm, Lô chênh lệch, Tổng chênh lệch) + Nút "Hoàn Thành" + Nút "Điều Chỉnh Kho"

**Business Logic**:
1. Tạo phiếu → load tất cả `LoHang WHERE SoLuong > 0`
2. Nhân viên nhập `TonThucTe` cho từng lô
3. `ChenhLech = TonThucTe - TonHeThong`
4. "Điều Chỉnh Kho" (Admin only): `UPDATE LoHang SET SoLuong = @TonThucTe`

---

### Sidebar & Dashboard

#### [MODIFY] [MainFrame.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/presentation/MainFrame.java)

Thêm 3 button vào sidebar:

```
CHỨC NĂNG
├── Dashboard
├── Bán Hàng
├── Sản Phẩm
├── Nhập Kho
├── Trả Hàng NCC    ← NEW
├── Quản Lý Kho
├── Hủy Hàng        ← NEW
├── Nhà Cung Cấp
├── Khách Hàng
QUẢN TRỊ
├── Kiểm Kho        ← NEW (Admin only)
├── Người Dùng
```

#### [MODIFY] [ReportPanel.java](file:///d:/eproject/eProject-StoreBanThuoc/src/main/java/presentation/panel/ReportPanel.java)

Thêm 2 stat cards: "Trả hàng tháng này" + "Tổn thất tháng này"

---

## Verification Plan

### Automated Tests
```bash
# Compile check — phải 0 errors
mvn clean compile
```

### Manual Verification

Sau khi implement xong, chạy `mvn compile exec:java` rồi test theo thứ tự:

**1. Trả Hàng NCC**:
- Vào sidebar "Trả Hàng NCC"
- Chọn NCC → Chọn phiếu nhập → Thấy danh sách lô hàng
- Chọn lô → Nhập SL trả > SL tồn → Phải báo lỗi
- Nhập SL trả hợp lệ + lý do → Thêm → Tạo phiếu → Check tồn kho giảm
- Kiểm tra bảng lịch sử có phiếu mới

**2. Hủy Hàng**:
- Vào sidebar "Hủy Hàng"
- Filter "Hết hạn" → Chỉ hiện lô đã hết hạn
- Chọn lô → Nhập SL → Tạo phiếu hủy → Check tồn kho giảm
- Kiểm tra tổn thất = SL × GiaNhap

**3. Kiểm Kho**:
- Vào sidebar "Kiểm Kho" (cần login Admin)
- Tạo phiếu kiểm mới → Load tất cả lô còn tồn
- Nhập TonThucTe khác TonHeThong → ChenhLech tự tính
- Click "Điều chỉnh kho" → Tồn kho thay đổi theo TonThucTe

**4. Dashboard**:
- Quay về Dashboard → Thấy stat cards mới (Trả hàng, Tổn thất)
