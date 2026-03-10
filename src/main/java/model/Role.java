package model;

public enum Role {
    ADMIN("Admin", new String[]{"Admin", "Administrator", "Quản trị", "Quan Tri", "1"}),
    QUANLY("Quản lý", new String[]{"Quản lý", "QuanLy", "Quan ly", "Manager", "2"}),
    NHANVIEN("Nhân viên", new String[]{"Nhân viên", "NhanVien", "Nhan vien", "Staff", "Employee", "3"});

    private final String label;
    private final String[] aliases;

    Role(String label, String[] aliases) { 
        this.label = label; 
        this.aliases = aliases;
    }

    public String getLabel() { return label; }
    
    /**
     * Lấy Role từ chuỗi theo nhiều ngôn ngữ và định dạng khác nhau.
     * Khuyến nghị sau này nên dùng cột RoleID (1, 2, 3) hoặc RoleCode cố định thay vì dùng cột Tên hiển thị để phân quyền
     */
    public static Role fromString(String text) {
        if (text == null || text.trim().isEmpty()) return NHANVIEN;
        
        String normalized = text.trim();
        
        // Tìm kiếm tất cả các alias có thể có
        for (Role role : Role.values()) {
            for (String alias : role.aliases) {
                if (normalized.equalsIgnoreCase(alias)) {
                    return role;
                }
            }
        }
        
        return NHANVIEN; // Default
    }
}
