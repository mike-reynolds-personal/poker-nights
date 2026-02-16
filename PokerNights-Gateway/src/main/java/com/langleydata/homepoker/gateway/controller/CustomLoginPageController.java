package com.langleydata.homepoker.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CustomLoginPageController {
	private static String TEMPLATE_FILE = "static/views/login.html";
	private static String baseUrl = "/oauth2/authorization/";
	private final Logger logger = LoggerFactory.getLogger(CustomLoginPageController.class);

	private static String TEMPLATE;
	static {
		TEMPLATE = ControllerUtils.getTemplate(TEMPLATE_FILE);
	}
	
	@Autowired
	private ReactiveClientRegistrationRepository clientRegRepo;

	
	@GetMapping(value = "/signin", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String doLogin(@RequestParam(required = false) final String param) {
		
		StringBuffer rows = new StringBuffer();
		
		if ("logout".equals(param)) {
			rows.append("<html>alert(\"You have been logged out!\");</html>");
			return rows.toString();
		} else if ("error".equals(param)) {
			rows.append("<html>alert(\"There was an error logging you in!\");</html>");
			return rows.toString();
		}
		
		Iterable<ClientRegistration> clientRegistrations = null;
		ResolvableType type = ResolvableType.forInstance(clientRegRepo).as(Iterable.class);
		if (type != ResolvableType.NONE && ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
			clientRegistrations = (Iterable<ClientRegistration>) clientRegRepo;
		}

		clientRegistrations.forEach(
				registration -> {
					rows.append("<tr><td class=\"pt-2\" style=\"text-align: center;\">").append(getButton(registration.getRegistrationId(), registration.getClientName())).append("</td></tr>");
				});
		
		return TEMPLATE.replace("{loginProviders}", rows.toString());
		
	}

	@GetMapping(value = "/signout")
	public String doLogout() {
		return null;
	}
	/**
	 * 
	 * @param provider
	 * @param client
	 * @return
	 */
	private String getButton(final String provider, final String client) {
		StringBuffer build = new StringBuffer();
		
		String imgSource = "";
		if ("google".equals(provider)) {
			imgSource = "/gateway-content/images/google-icon.svg";
		} else if ("facebook".equals(provider)) {
			imgSource = "/gateway-content/images/facebook-icon.svg";
		}
		
		
		build.append("<div>")
			.append("<a class=\"btn btn-outline-dark p-2\" href=\"").append(baseUrl).append(provider).append("\" role=\"button\" style=\"text-transform:none;min-width:240px\">")
			.append("<img width=\"25px\" class=\"mr-2 m-1\"")
			.append("src=\"").append(imgSource).append("\"/>")
			.append("Continue&nbsp;with&nbsp;").append(provider).append("</a>"); 
		return build.toString();
	}
}
