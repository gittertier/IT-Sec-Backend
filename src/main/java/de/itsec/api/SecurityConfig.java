package de.itsec.api;

import de.itsec.api.authfilter.CsrfCookieFilter;
import de.itsec.api.authfilter.JsonUsernamePasswordAuthenticationFilter;
import de.itsec.api.authfilter.RateLimitingFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, AuthenticationConfiguration authConfig) {

    AuthenticationManager authenticationManager = authConfig.getAuthenticationManager();

    JsonUsernamePasswordAuthenticationFilter jsonFilter =
        new JsonUsernamePasswordAuthenticationFilter();
    jsonFilter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
    jsonFilter.setAuthenticationManager(authenticationManager);
    jsonFilter.setFilterProcessesUrl("/api/v1/public/login");
    jsonFilter.setUsernameParameter("username");
    jsonFilter.setPasswordParameter("password");
    jsonFilter.setAuthenticationSuccessHandler(
        (req, res, auth) -> {
          req.getSession(true);

          res.setStatus(HttpServletResponse.SC_OK);
          res.setContentType(MediaType.APPLICATION_JSON_VALUE);
          res.getWriter().write("{\"message\":\"login successful\"}");
        });
    jsonFilter.setAuthenticationFailureHandler(
        (req, res, ex) -> {
          res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          res.setContentType(MediaType.APPLICATION_JSON_VALUE);
          res.getWriter().write("{\"error\":\"bad credentials\"}");
        });

    http.csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers("/login")
                    .ignoringRequestMatchers("/api/v1/public/login"))

        // auth
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/public/**")
                    .permitAll()
                    .requestMatchers("/login")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())

        // login filter before credentials checking for brute force
        .addFilterBefore(new RateLimitingFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterAt(jsonFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)

        // sessoin fixation
        .sessionManagement(
            session ->
                session
                    .sessionFixation()
                    .migrateSession()
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false))

        // set STS cookies
        .headers(
            headers ->
                headers.httpStrictTransportSecurity(
                    hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/v1/public/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("SESSION")
                    .logoutSuccessHandler(
                        (req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT)))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    (req, res, e) -> {
                      res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                      res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      res.getWriter().write("{\"error\":\"Nicht authentifiziert\"}");
                    }));

    return http.build();
  }

  @Bean
  public PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }
}
