package dk.acto.auth.services;

import javax.servlet.http.HttpServletResponse;

public interface Callback3Service extends BasicService {
    void callback (HttpServletResponse response, String user, String timestamp, String auth);
}