package service;

import model.Permission;
import model.User;

public class PermissionService {

	public static boolean has(User user, Permission permission) {
		return RolePermission.getPermissions(user.getRole()).contains(permission);
	}
}
