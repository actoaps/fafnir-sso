package dk.acto.auth.services;

import javax.servlet.http.HttpServletResponse;

public interface Callback2Service extends BasicService {
	void callback(HttpServletResponse response, String value1, String value2);
}