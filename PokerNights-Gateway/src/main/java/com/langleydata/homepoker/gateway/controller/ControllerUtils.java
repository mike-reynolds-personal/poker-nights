package com.langleydata.homepoker.gateway.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

public class ControllerUtils {

	/** Load a file from the classpath
	 * 
	 * @param file
	 * @return
	 */
	public  static String getTemplate(final String file) {
		String template = "";
		try (InputStream inputStream = new ClassPathResource(file).getInputStream()) {
			template = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return template;
	}
}
