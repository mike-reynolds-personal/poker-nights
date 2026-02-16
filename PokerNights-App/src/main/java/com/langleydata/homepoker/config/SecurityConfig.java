package com.langleydata.homepoker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

   @Override  
   protected void configure(HttpSecurity http) throws Exception {
	   http.authorizeRequests().anyRequest().permitAll()
	   .and().csrf().disable();
	   /* Permit all requests so Spring-security doesn't kick-in and
	    * want to present the user with a login box */
   }
}