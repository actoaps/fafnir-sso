package dk.acto.auth.services;

import javax.servlet.http.HttpServletResponse;

public interface CallbackService extends BasicService {
	void callback(HttpServletResponse response, String code);
}
