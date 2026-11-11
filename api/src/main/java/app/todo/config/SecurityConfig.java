package app.todo.config;

import app.todo.auth.SessionCookieAuthenticationFilter;
import app.todo.auth.SessionService;
import app.todo.common.ClientHeaderFilter;
import app.todo.common.ProblemAuthenticationEntryPoint;
import app.todo.common.ProblemWriter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Spring Security for the API (sdd.md section 4, ADR-0004, ADR-0007): a stateless chain with no HTTP session. All
 * {@code /api/v1/todos/**} paths and {@code /api/v1/auth/session} need a session and are answered 401
 * {@code UNAUTHENTICATED} through the entry point; sign-up, login, logout and any other path pass security (an unknown
 * route is then answered 404 by the error model). The order of checks on a protected unsafe request is: session (401),
 * client header (403), then the dispatcher's 404/405, 415, 400, 404.
 *
 * <p>The two filters are created here and are not beans, so Spring Boot cannot also register them as servlet filters.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /**
     * Authentication is by session cookie only (the session filter), so Spring Security manages no users. Declaring this
     * keeps Spring Boot from creating its default user with a generated password that it would write to the log.
     */
    @Bean
    UserDetailsService noUserAccounts() {
        return username -> {
            throw new UsernameNotFoundException("Spring Security manages no user accounts");
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SessionService sessions, ProblemWriter problemWriter,
            ProblemAuthenticationEntryPoint entryPoint) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // the client header is the cross-site defence (ADR-0007)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/api/v1/todos", "/api/v1/todos/**", "/api/v1/auth/session").authenticated()
                        .anyRequest().permitAll())
                .headers(Customizer.withDefaults())
                .addFilterBefore(new SessionCookieAuthenticationFilter(sessions), AnonymousAuthenticationFilter.class)
                .addFilterAfter(new ClientHeaderFilter(problemWriter), AuthorizationFilter.class);
        return http.build();
    }
}
