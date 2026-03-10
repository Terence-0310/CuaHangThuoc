package model;

public class User {
	private int id;
	private String username;
	private String password;
	private Role role;
	private boolean deleted;

	public User() {
	}

	public User(int id, String username, String password, Role role, boolean deleted) {
		this.id = id;
		this.username = username;
		this.password = password;
		this.role = role;
		this.deleted = deleted;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}
