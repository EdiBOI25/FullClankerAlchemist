package fullclanker.alchemist.domain.model;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
public class User {

	public static final String USERNAME_PATTERN = "[a-zA-Z0-9_-]{3,30}";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, options = "COLLATE NOCASE", check = @CheckConstraint(
			constraint = "length(username) between 3 and 30 and username not glob '*[^a-zA-Z0-9_-]*'"))
	@NotNull
	@Pattern(regexp = USERNAME_PATTERN)
	private String username;

	// bcrypt hash, always 60 characters; never the raw password
	@Column(nullable = false, check = @CheckConstraint(constraint = "length(password) = 60"))
	@NotNull
	@Size(min = 60, max = 60)
	private String password;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	@NotNull
	private Role role;

	protected User() {
	}

	public User(String username, String password, Role role) {
		this.username = username;
		this.password = password;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public Role getRole() {
		return role;
	}

}
