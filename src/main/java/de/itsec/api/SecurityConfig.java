package de.itsec.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http.authorizeHttpRequests(
            authorize -> {
              authorize.requestMatchers("/").permitAll();
              authorize.requestMatchers("/error").permitAll();
              authorize.requestMatchers("/api/v1/public/**").permitAll();
              authorize.anyRequest().authenticated();
            })
        .formLogin(Customizer.withDefaults())
        .logout(LogoutConfigurer::permitAll)
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
    return http.build();
  }

  @Bean
  public PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }
}
