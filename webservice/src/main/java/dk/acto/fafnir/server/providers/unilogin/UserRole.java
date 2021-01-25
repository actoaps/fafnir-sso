package dk.acto.fafnir.server.providers.unilogin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class UserRole {
	private final String name;
	private final String type;

	@Override
	public String toString() {
		return name + '@' + type;
	}
}