package dk.acto.fafnir.client;

import dk.acto.fafnir.api.model.FafnirUser;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Data
@Builder
public class JwtAuthentication implements Authentication, UserDetails {
    @Singular
    private final List<GrantedAuthority> authorities;
    private final FafnirUser details;

    @Override
    public String getCredentials() {
        return getSubject();
    }

    @Override
    public Principal getPrincipal() {
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return getSubject() != null;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated){
        throw new IllegalArgumentException("Authentication is immutable.");
    }

    @Override
    public String getName() {
        return Optional.ofNullable(details)
                .map(FafnirUser::getName)
                .orElse(null);
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return getSubject();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private String getSubject() {
        return Optional.ofNullable(details)
            .map(FafnirUser::getSubject)
            .orElse(null);
    }

    public String getMetaId() {
        return Optional.ofNullable(details)
                .map(FafnirUser::getMetaId)
                .orElse(null);
    }

    public boolean hasMetaId () {
        return Optional.ofNullable(details)
                .map(FafnirUser::getMetaId).isPresent();
    }

    public boolean isInOrganisation() {
        return Optional.ofNullable(details)
                .map(FafnirUser::getOrganisationId)
                .isPresent();
    }
}
