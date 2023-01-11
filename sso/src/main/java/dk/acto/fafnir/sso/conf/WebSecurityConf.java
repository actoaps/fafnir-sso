package dk.acto.fafnir.sso.conf;

import dk.acto.fafnir.sso.saml.UpdateableRelyingPartyRegistrationRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class WebSecurityConf implements WebMvcConfigurer {
    private final UpdateableRelyingPartyRegistrationRepository updateableRelyingPartyRegistrationRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var filter = new Saml2MetadataFilter(
                new DefaultRelyingPartyRegistrationResolver(updateableRelyingPartyRegistrationRepository),
                new OpenSamlMetadataResolver());

        http.httpBasic().disable();
        http.formLogin().disable();
        http.csrf().disable();
        http.saml2Login()
                .loginPage("/saml/login")
                .defaultSuccessUrl("/saml/callback", true)
                .relyingPartyRegistrationRepository(updateableRelyingPartyRegistrationRepository);
        http.addFilterBefore(filter, Saml2WebSsoAuthenticationRequestFilter.class);
        http.authorizeHttpRequests()
                .requestMatchers("/saml/callback")
                .authenticated()
                .requestMatchers("/**").permitAll();
        return http.build();
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true); // TODO: Use strict slashing
    }
}
