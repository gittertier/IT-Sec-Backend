package de.itsec.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

  // springdoc default endpoints, only exposed when the API docs are enabled
  private static final String[] API_DOCS_WHITELIST = {
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs",
    "/v3/api-docs/**",
    "/v3/api-docs.yaml"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      AuthenticationConfiguration authConfig,
      @Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled) {

    AuthenticationManager authenticationManager = authConfig.getAuthenticationManager();

    JsonUsernamePasswordAuthenticationFilter jsonFilter =
        new JsonUsernamePasswordAuthenticationFilter();
    jsonFilter.setAuthenticationManager(authenticationManager);
    jsonFilter.setFilterProcessesUrl("/api/v1/public/login");
    jsonFilter.setUsernameParameter("username");
    jsonFilter.setPasswordParameter("password");
    // Persist the authenticated context in the session. The filter is wired manually (not via the
    // formLogin DSL), so Spring does not inject the shared session-backed repository for us; without
    // this the default request-scoped repository would drop the login right after the response.
    jsonFilter.setSecurityContextRepository(
        new DelegatingSecurityContextRepository(
            new RequestAttributeSecurityContextRepository(),
            new HttpSessionSecurityContextRepository()));
    jsonFilter.setAuthenticationSuccessHandler(
        (req, res, auth) -> {
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
            auth -> {
              auth.requestMatchers("/api/v1/public/**", "/login", "/error").permitAll();
              if (apiDocsEnabled) {
                auth.requestMatchers(API_DOCS_WHITELIST).permitAll();
              }
              auth.anyRequest().authenticated();
            })

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
                    .deleteCookies("JSESSIONID")
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
