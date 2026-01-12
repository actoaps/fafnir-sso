package dk.acto.fafnir.sso.provider.unilogin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class Institution {
	private final String id;
	private final String name;
	private final List<String> roles;
}
