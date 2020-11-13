package dk.acto.fafnir.providers.unilogin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

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
