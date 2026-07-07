package dk.acto.fafnir.sso.provider.unilogin;

import lombok.Value;

@Value
public class JwtCacheRetrieveResult {
    String jwt;
    String cacheReason;

    public static JwtCacheRetrieveResult success(String jwt) {
        return new JwtCacheRetrieveResult(jwt, null);
    }

    public static JwtCacheRetrieveResult miss(String cacheReason) {
        return new JwtCacheRetrieveResult(null, cacheReason);
    }

    public boolean isSuccess() {
        return jwt != null && !jwt.isEmpty();
    }
}
