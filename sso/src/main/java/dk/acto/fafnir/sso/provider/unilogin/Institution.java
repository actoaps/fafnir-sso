package dk.acto.fafnir.sso.provider.unilogin;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Institution {
	String id;
	String name;
	List<String> roles;
}
