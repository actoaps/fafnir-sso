package dk.acto.auth.services;

import dk.acto.auth.ActoConf;

import javax.servlet.http.HttpServletResponse;

public interface CallbackService extends BasicService {
    void callback (HttpServletResponse response,  ActoConf actoConf, String code);
}
