package dk.acto.fafnir.sso.provider.unilogin;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Builder
@AllArgsConstructor
public class Institution {
	public String id;
	public String name;
	public List<String> roles;
}
