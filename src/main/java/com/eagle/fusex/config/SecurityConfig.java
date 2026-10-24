package com.eagle.fusex.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails medico = User.builder()
                .username("medico")
                .password(passwordEncoder().encode("senha123"))
                .roles("MEDICO")
                .build();

        UserDetails emissorGuia = User.builder()
                .username("eg")
                .password(passwordEncoder().encode("senha123"))
                .roles("EG")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN", "MEDICO", "EG")
                .build();

        UserDetails ocs = User.builder()
                .username("ocs")
                .password(passwordEncoder().encode("senha123"))
                .roles("OCS")
                .build();

        return new InMemoryUserDetailsManager(medico, emissorGuia, admin, ocs);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests((authorize) -> authorize
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/solicitacoes/publico/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/procedimentos/**", "/ocs/**", "/medicos/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/solicitacoes").hasAnyRole("MEDICO", "ADMIN")
                .requestMatchers("/pacientes", "/pacientes/**").hasAnyRole("MEDICO", "ADMIN")
                .requestMatchers("/auth/**").authenticated()
                .requestMatchers("/solicitacoes/*/pre-guia").hasAnyRole("EG", "MEDICO", "ADMIN")
                .requestMatchers("/solicitacoes/*/guia-pdf", "/solicitacoes/*/guia-pdf/**").hasAnyRole("EG", "ADMIN")
                .requestMatchers("/guias/*/validade").hasAnyRole("OCS", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/guias").hasAnyRole("OCS", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/guias").hasAnyRole("OCS", "ADMIN")
                .requestMatchers("/lotes/**").hasAnyRole("OCS", "ADMIN")
                .requestMatchers("/fechamentos/**").hasAnyRole("OCS", "ADMIN")
                .requestMatchers("/encaminhamentos/**").hasAnyRole("MEDICO", "ADMIN")
                .requestMatchers("/importacao/**").hasRole("ADMIN")
                .requestMatchers(
                    "/",
                    "/*.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/static/**",
                    "/error"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"Não autorizado: credenciais ausentes ou inválidas.\"}");
            }));

        return http.build();
    }
}
