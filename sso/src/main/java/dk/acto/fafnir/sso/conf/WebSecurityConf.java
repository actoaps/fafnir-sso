package dk.acto.fafnir.sso.conf;

import dk.acto.fafnir.sso.saml.UpdateableRelyingPartyRegistrationRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class WebSecurityConf {
    private final UpdateableRelyingPartyRegistrationRepository updateableRelyingPartyRegistrationRepository;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var filter = new Saml2MetadataFilter(
                (RelyingPartyRegistrationResolver) new DefaultRelyingPartyRegistrationResolver(updateableRelyingPartyRegistrationRepository),
                new OpenSamlMetadataResolver());


        http.httpBasic().disable();
        http.formLogin().disable();
        http.csrf().disable();
        http.saml2Login()
                .loginPage("/")
                .defaultSuccessUrl("/saml/callback", true)
                .relyingPartyRegistrationRepository(updateableRelyingPartyRegistrationRepository);
        http.addFilterBefore(filter, Saml2WebSsoAuthenticationFilter.class);
        http.authorizeRequests()
                .antMatchers("/saml/callback")
                .authenticated();
        return http.build();
    }
}
