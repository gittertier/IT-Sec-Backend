package de.itsec.api;

import de.itsec.api.authfilter.CsrfCookieFilter;
import de.itsec.api.authfilter.JsonUsernamePasswordAuthenticationFilter;
import de.itsec.api.authfilter.RateLimitingFilter;
import de.itsec.api.authfilter.TotpPendingAuthenticationFilter;
import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.TotpPendingAuthentication;
import de.itsec.api.repositories.authentication.UserRepository;
import de.itsec.api.services.LoginAttemptService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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

  private static final String[] API_DOCS_WHITELIST = {
    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      AuthenticationConfiguration authConfig,
      TotpPendingAuthenticationFilter totpFiler,
      UserRepository userRepository,
      LoginAttemptService loginAttemptService,
      @Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled) {

    AuthenticationManager authenticationManager = authConfig.getAuthenticationManager();

    JsonUsernamePasswordAuthenticationFilter jsonFilter =
        new JsonUsernamePasswordAuthenticationFilter();
    jsonFilter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
    jsonFilter.setAuthenticationManager(authenticationManager);
    jsonFilter.setLoginAttemptService(loginAttemptService);
    jsonFilter.setFilterProcessesUrl("/api/v1/public/login");
    jsonFilter.setUsernameParameter("username");
    jsonFilter.setPasswordParameter("password");
    jsonFilter.setAuthenticationSuccessHandler(
        (req, res, auth) -> {
          HttpSession session = req.getSession(true);
          res.setContentType(MediaType.APPLICATION_JSON_VALUE);

          boolean totpEnabled =
              userRepository.findByUsername(auth.getName()).map(User::isTotpEnabled).orElse(false);

          if (totpEnabled) {
            // Password was right, but the account has a second factor: keep the
            // session "pending" (no real authentication yet) until the TOTP code
            // is verified at /api/v1/public/login/totp.
            session.setAttribute(
                "TOTP_PENDING",
                new TotpPendingAuthentication(auth.getName(), auth.getAuthorities()));

            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(emptyContext);
            new HttpSessionSecurityContextRepository().saveContext(emptyContext, req, res);

            res.setStatus(HttpServletResponse.SC_ACCEPTED);
            res.getWriter().write("{\"message\":\"totp required\"}");
          } else {
            // No second factor set up yet: this is already a full login. The user
            // can still set up TOTP afterwards from this session (e.g. right after
            // registration), so we do not force them out here.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, req, res);

            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("{\"message\":\"login successful\"}");
          }
        });
    jsonFilter.setAuthenticationFailureHandler(
        (req, res, ex) -> {
          res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          res.setContentType(MediaType.APPLICATION_JSON_VALUE);
          res.getWriter().write("{\"error\":\"bad credentials\"}");
        });

    http.csrf(
            csrf -> {
              csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                  .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                  .ignoringRequestMatchers("/login")
                  .ignoringRequestMatchers("/api/v1/public/csrf")
                  .ignoringRequestMatchers("/api/v1/public/login")
                  .ignoringRequestMatchers("/api/v1/public/login/totp");
              if (apiDocsEnabled) {
                csrf.ignoringRequestMatchers(API_DOCS_WHITELIST);
              }
            })

        // auth
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers("/api/v1/public/**", "/error").permitAll();
              // Onboarding endpoints: reachable while an account is still finishing
              // onboarding (ROLE_ONBOARDING), plus normal logged-in users.
              auth.requestMatchers(
                      "/api/v1/totp/setup",
                      "/api/v1/totp/confirm",
                      "/api/v1/verify-email",
                      "/api/v1/verify-request")
                  .hasAnyRole("USER", "ADMIN", "STAFF", "ONBOARDING");
              // Reading the own profile drives the onboarding UI, so allow it too.
              auth.requestMatchers(HttpMethod.GET, "/api/v1/me")
                  .hasAnyRole("USER", "ADMIN", "STAFF", "ONBOARDING");
              if (apiDocsEnabled) {
                auth.requestMatchers(API_DOCS_WHITELIST).permitAll();
              }
              // Everything else needs a fully onboarded account: authenticated and
              // neither still onboarding nor mid-2FA-login.
              auth.anyRequest()
                  .access(
                      AuthorizationManagers.allOf(
                          AuthenticatedAuthorizationManager.authenticated(),
                          AuthorizationManagers.not(
                              AuthorityAuthorizationManager.hasRole("TOTP_PENDING")),
                          AuthorizationManagers.not(
                              AuthorityAuthorizationManager.hasRole("ONBOARDING"))));
            })
        // login filter before credentials checking for brute force
        .addFilterBefore(new RateLimitingFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(totpFiler, UsernamePasswordAuthenticationFilter.class)
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
