package dk.acto.auth.providers.unilogin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
public class Institution {
	private String id;
	private String name;
	private List<String> roles;

	public Institution(String id, String name) {
		this.id = id;
		this.name = name;
	}
}
