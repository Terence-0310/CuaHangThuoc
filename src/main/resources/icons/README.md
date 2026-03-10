# Icons Folder

Thư mục này chứa các file icon cho ứng dụng.

## Cách thêm icon:

1. **Tải icon từ các nguồn miễn phí:**
   - [Flaticon](https://www.flaticon.com/)
   - [Icons8](https://icons8.com/)
   - [Material Icons](https://fonts.google.com/icons)
   - [Font Awesome](https://fontawesome.com/icons)

2. **Định dạng icon:**
   - PNG với nền trong suốt (transparent background)
   - Kích thước khuyến nghị: 64x64 hoặc 128x128 pixels
   - Màu sắc: Nên dùng màu đơn sắc hoặc phù hợp với theme pastel

3. **Đặt tên file:**
   - dashboard.png - Icon cho Tổng quan
   - sales.png - Icon cho Bán hàng
   - import.png - Icon cho Nhập hàng
   - product.png - Icon cho Sản phẩm
   - batch.png - Icon cho Lô hàng
   - customer.png - Icon cho Khách hàng
   - supplier.png - Icon cho Nhà cung cấp
   - report.png - Icon cho Báo cáo
   - user.png - Icon cho Người dùng

4. **Cách sử dụng trong code:**
   ```java
   // Thay vì emoji:
   var label = new JLabel("📊 Tổng quan");
   
   // Dùng IconHelper:
   var label = IconHelper.createIconLabel("dashboard.png", "Tổng quan", 24);
   ```

## Lưu ý:
- Nếu không có icon file, ứng dụng sẽ tự động dùng emoji làm fallback
- Icon sẽ được tự động resize theo kích thước yêu cầu
- Đảm bảo file icon có trong thư mục này trước khi chạy ứng dụng
