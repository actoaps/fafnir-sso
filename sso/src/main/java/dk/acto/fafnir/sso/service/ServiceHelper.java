package dk.acto.fafnir.sso.service;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public interface ServiceHelper {
    static Object functionalRedirectTo(HttpServletResponse response, Supplier<String> function) throws IOException {
        response.sendRedirect(function.get());
        return null;
    }

    static String getLocaleStr(String acceptLanguageHeader, String... acceptedLocales) {
        var foundLocale = getSupportedLocale(acceptLanguageHeader, acceptedLocales).orElse(Locale.ENGLISH);
        return "en".equals(foundLocale.getLanguage()) ? "" : "_" + foundLocale.getLanguage();
    }

    private static Optional<Locale> getSupportedLocale(String acceptLanguageHeader, String... acceptedLocales) {
        return getSupportedLocale(
                acceptLanguageHeader,
                Arrays.stream(acceptedLocales).map(Locale::forLanguageTag).toArray(Locale[]::new)
        );
    }

    private static Optional<Locale> getSupportedLocale(String acceptLanguageHeader, Locale[] acceptedLocales) {
        var languages = Locale.LanguageRange.parse(acceptLanguageHeader);
        return Optional.ofNullable(Locale.lookup(languages, Arrays.asList(acceptedLocales)));
    }

}
