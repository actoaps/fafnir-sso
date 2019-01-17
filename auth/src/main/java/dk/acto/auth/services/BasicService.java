package dk.acto.auth.services;

import dk.acto.auth.ActoConf;

import javax.servlet.http.HttpServletResponse;

public interface BasicService {
    void authenticate(HttpServletResponse response, ActoConf actoConf);
}