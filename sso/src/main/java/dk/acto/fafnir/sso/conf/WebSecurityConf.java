package dk.acto.fafnir.sso.conf;

import dk.acto.fafnir.sso.saml.UpdateableRelyingPartyRegistrationRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class WebSecurityConf extends WebSecurityConfigurerAdapter {
    private final UpdateableRelyingPartyRegistrationRepository updateableRelyingPartyRegistrationRepository;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic().disable();
        http.formLogin().disable();
        http.csrf().disable();
        http.saml2Login()
                .loginPage("/")
                .defaultSuccessUrl("/saml/callback", true)
                .relyingPartyRegistrationRepository(updateableRelyingPartyRegistrationRepository);
        http.authorizeRequests()
                .antMatchers("/saml/callback")
                .authenticated();
    }
}
