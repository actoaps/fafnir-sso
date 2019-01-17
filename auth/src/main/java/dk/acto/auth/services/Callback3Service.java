package dk.acto.auth.services;

import dk.acto.auth.ActoConf;

import javax.servlet.http.HttpServletResponse;

public interface Callback3Service extends BasicService {
    void callback (HttpServletResponse response, ActoConf actoConf, String user, String timestamp, String auth);
}