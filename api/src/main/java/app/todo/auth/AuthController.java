package app.todo.auth;

import app.todo.auth.api.CurrentUser;
import app.todo.common.UtcTimestamps;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The identity module's operations: {@code signUp}, {@code logIn}, {@code logOut}, {@code getSession} ({@code identity-api-v1.yaml}). */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthService auth;
    private final boolean secureCookie;

    AuthController(AuthService auth, @Value("${todo.cookie.secure:false}") boolean secureCookie) {
        this.auth = auth;
        this.secureCookie = secureCookie;
    }

    @PostMapping(value = "/signup", consumes = "application/json")
    ResponseEntity<SignupResult> signUp(@RequestBody Map<String, Object> request) {
        String identifier = auth.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SignupResult(new UserView(identifier)));
    }

    @PostMapping(value = "/login", consumes = "application/json")
    ResponseEntity<SessionView> logIn(@RequestBody Map<String, Object> request) {
        AuthService.LoggedIn loggedIn = auth.logIn(request);
        ResponseCookie cookie = cookie(loggedIn.session().token(), SessionService.LIFETIME);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new SessionView(new UserView(loggedIn.identifier()), UtcTimestamps.format(loggedIn.session().expiresAt())));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logOut(HttpServletRequest request) {
        auth.logOut(SessionCookieAuthenticationFilter.tokenOf(request));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/session")
    SessionView getSession(Authentication authentication, @AuthenticationPrincipal CurrentUser user) {
        SessionDetails details = (SessionDetails) authentication.getDetails();
        return new SessionView(new UserView(auth.identifierOf(user.userId())), UtcTimestamps.format(details.expiresAt()));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(SessionCookieAuthenticationFilter.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public record UserView(String identifier) {}

    public record SignupResult(UserView user) {}

    public record SessionView(UserView user, String expiresAt) {}
}
