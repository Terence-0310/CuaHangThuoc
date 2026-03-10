package service;

import model.Permission;
import model.Role;

import java.util.*;

public class RolePermission {

    private static final Map<Role, Set<Permission>> MAP = Map.of(
        Role.ADMIN, EnumSet.allOf(Permission.class),
        Role.QUANLY, EnumSet.of(
            Permission.MEDICINE_MANAGE,
            Permission.REPORT_VIEW
        ),
        Role.NHANVIEN, EnumSet.of(
            Permission.MEDICINE_MANAGE
        )
    );

    public static Set<Permission> getPermissions(Role role) {
        return MAP.getOrDefault(role, EnumSet.noneOf(Permission.class));
    }
}
