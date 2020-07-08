package dk.acto.auth.client.providers;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

public interface AuthoritiesProvider {
    Collection<GrantedAuthority> mapAuthorities(Map<String, Object> claims);
}
