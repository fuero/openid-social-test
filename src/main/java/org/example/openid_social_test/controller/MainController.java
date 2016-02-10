package org.example.openid_social_test.controller;

import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController implements ErrorController {
	
	private static final String PATH = "/error";

	@RequestMapping(value = PATH)
	public String error(HttpServletRequest request, Model model) {
		int code = getErrorCode(request);
		Throwable throwable = (Throwable) request
				.getAttribute("javax.servlet.error.exception");
		String errorMessage = getExceptionMessage(throwable, code);
		String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
		if (requestUri == null) {
			requestUri = "Unknown";
		}

		String message = MessageFormat.format(
				"{0} returned for {1} with message {2}", code, requestUri,
				errorMessage);

		model.addAttribute("errorMessage", message);
		model.addAttribute("errorCode", code);
		return "error";
	}

	@Override
	public String getErrorPath() {
		return PATH;
	}

	private int getErrorCode(HttpServletRequest request) {
		Integer code = (Integer) request
				.getAttribute("javax.servlet.error.status_code");
		if (code == null)
			return 0;
		return code;
	}

	private String getExceptionMessage(Throwable throwable, int statusCode) {
		if (throwable != null) {
			for (Throwable i = throwable; i != null; i = i.getCause()) {
				if (i.getCause() == null) {
					return i.getMessage();
				}
			}
		}
		if (statusCode >= 200 && statusCode < 600) { 
			HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
			return httpStatus.getReasonPhrase();
		}
		return "";
	}

	@RequestMapping("/")
	public String index() {
		return "index";
	}
}
