package com.vn.backend.configs;


import com.vn.backend.configs.jwt.JwtAuthenticationFilter;
import com.vn.backend.constants.AppConst;
import com.vn.backend.enums.Role;
import com.vn.backend.services.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Qualifier("delegatedAuthenticationEntryPoint")
    private AuthenticationEntryPoint authEntryPoint;

    private final AccessDeniedHandler accessDeniedHandler;

    public WebSecurityConfig(CustomUserDetailsService customUserDetailsService, AccessDeniedHandler accessDeniedHandler, AuthenticationEntryPoint authEntryPoint) {
        this.customUserDetailsService = customUserDetailsService;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authEntryPoint = authEntryPoint;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        AppConfig appConfig = new AppConfig();
        auth.userDetailsService(customUserDetailsService)
                .passwordEncoder(appConfig.passwordEncoder());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeRequests(requests -> {
                    requests
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/api/files/download/**").permitAll()
                            .requestMatchers("/api/subjects/create").hasRole("ADMIN")
                            .requestMatchers("/api/questions/**").hasAnyRole("TEACHER","ADMIN")
                            .requestMatchers("/api/answers/**").hasAnyRole("TEACHER","ADMIN")
                            .requestMatchers("/api/approval-requests/**").hasAnyRole("TEACHER","ADMIN")
                            .requestMatchers("/swagger-ui/**", "/v2/**", "/authenticate", "/swagger-resources/**","/v3/api-docs/**").permitAll()
                            .requestMatchers("/ws/**").permitAll()
//                            .requestMatchers(HttpMethod.POST, AppConst.API+"/subjects/create").hasAuthority("ROLE_ADMIN")
                            .anyRequest().authenticated();
                })
                .exceptionHandling(handling -> {
                    handling
                            .authenticationEntryPoint(authEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler);
                });
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
