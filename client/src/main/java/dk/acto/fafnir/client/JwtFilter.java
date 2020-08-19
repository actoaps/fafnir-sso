package dk.acto.fafnir.client;

import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

@Component
@AllArgsConstructor
public class JwtFilter extends GenericFilterBean {
    private final JwtValidator validator;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        var cast = (HttpServletRequest) request;
        Optional.ofNullable(cast.getHeader("Authorization"))
                .map(validator::decodeToken)
                .ifPresent(x -> SecurityContextHolder.getContext().setAuthentication(x));

        chain.doFilter(request, response);
    }
}
