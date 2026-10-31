package com.mentenova.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.requireCsrfProtectionMatcher(
                        new AndRequestMatcher(
                                CsrfFilter.DEFAULT_CSRF_MATCHER,
                                new OrRequestMatcher(
                                        new AntPathRequestMatcher("/psicologos/**"),
                                        new AntPathRequestMatcher("/especialidades/**"),
                                        new AntPathRequestMatcher("/citas/**")
                                )
                        )
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/registro/**", "/css/**", "/images/**", "/error").permitAll()
                        .requestMatchers("/dashboard", "/dashboard/**")
                        .hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers("/mis-citas", "/mis-citas/**").hasRole("PACIENTE")
                        .requestMatchers("/agenda", "/agenda/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "PSICOLOGO")
                        .requestMatchers("/citas", "/citas/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers("/reportes/ingresos", "/reportes/ingresos/**", "/api/reportes/ingresos", "/api/reportes/ingresos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/reportes", "/reportes/**", "/api/reportes", "/api/reportes/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers("/mi-disponibilidad", "/psicologos/*/disponibilidad", "/psicologos/*/disponibilidad/**", "/api/disponibilidad", "/api/disponibilidad/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/recepcion/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers("/psicologo/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "PSICOLOGO")
                        .requestMatchers("/paciente/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "PSICOLOGO", "PACIENTE")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/authenticate")
                        .usernameParameter("correo")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler())
                        .failureHandler(loginFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication)
                    throws IOException, ServletException {
                boolean esPsicologo = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_PSICOLOGO"));
                boolean esPaciente = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));
                String destino = esPsicologo ? "/agenda" : (esPaciente ? "/mis-citas" : "/dashboard");
                response.sendRedirect(request.getContextPath() + destino);
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                org.springframework.security.core.AuthenticationException exception)
                    throws IOException, ServletException {
                response.sendRedirect(request.getContextPath() + "/login?error=true");
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}