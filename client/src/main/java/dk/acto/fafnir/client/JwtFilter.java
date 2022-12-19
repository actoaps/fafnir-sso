package dk.acto.fafnir.client;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

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
