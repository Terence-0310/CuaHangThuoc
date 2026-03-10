package utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Role;

/**
 * PermissionManager - Quản lý phân quyền tập trung (RBAC Architecture)
 */
public class PermissionManager {

    private static final Map<String, List<Role>> permissions = new HashMap<>();

    static {
        // Cấu hình quyền truy cập cho từng chức năng (Action)
        permissions.put("dashboard", Arrays.asList(Role.ADMIN, Role.QUANLY, Role.NHANVIEN));
        permissions.put("banhang", Arrays.asList(Role.ADMIN, Role.QUANLY, Role.NHANVIEN));
        permissions.put("nhaphang", Arrays.asList(Role.ADMIN));
        permissions.put("sanpham", Arrays.asList(Role.ADMIN, Role.QUANLY, Role.NHANVIEN));
        permissions.put("lohang", Arrays.asList(Role.ADMIN, Role.QUANLY, Role.NHANVIEN));
        permissions.put("khachhang", Arrays.asList(Role.ADMIN, Role.QUANLY, Role.NHANVIEN));
        permissions.put("nhacungcap", Arrays.asList(Role.ADMIN, Role.QUANLY));
        permissions.put("baocao", Arrays.asList(Role.ADMIN, Role.QUANLY, Role.NHANVIEN));
        permissions.put("nguoidung", Arrays.asList(Role.ADMIN));
    }

    /**
     * Kiểm tra quyền truy cập dựa trên vai trò
     */
    public static boolean hasAccess(String vaiTro, String action) {
        if (vaiTro == null) return false;
        Role userRole = Role.fromString(vaiTro);
        
        // (Requirement: DIAGNOSE_LOGIC) In log kiểm tra giá trị thực tế
        System.out.println("[RBAC_DIAGNOSTIC] VaiTro DB: '" + vaiTro + "' -> Parsed Role: " + userRole.name() + " | Action: " + action);
        
        List<Role> allowedRoles = permissions.get(action.toLowerCase());
        return allowedRoles != null && allowedRoles.contains(userRole);
    }
}
