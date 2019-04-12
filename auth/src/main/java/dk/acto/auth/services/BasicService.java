package dk.acto.auth.services;

import javax.servlet.http.HttpServletResponse;

public interface BasicService {
	void authenticate(HttpServletResponse response);
}