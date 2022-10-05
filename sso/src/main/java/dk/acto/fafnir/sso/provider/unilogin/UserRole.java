package dk.acto.fafnir.sso.provider.unilogin;

import lombok.*;

@Value
@Builder
public class UserRole {
	String name;
	String type;

	@Override
	public String toString() {
		return name + '@' + type;
	}
}
