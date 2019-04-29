package dk.acto.auth.providers.unilogin;

import java.util.List;
import java.util.Objects;

public class Institution {
	private String id;
	private String name;
	private List<String> roles;

	public Institution(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public Institution(String id, String name, List<String> roles) {
		this(id, name);
		this.roles = roles;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getRoles() { return roles; }

	public void setRoles(List<String> roles) { this.roles = roles; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Institution that = (Institution) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
