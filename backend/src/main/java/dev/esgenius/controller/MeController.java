package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.dto.MeResponse;
import dev.esgenius.ratelimit.UsageLimiter;
import dev.esgenius.service.Caller;
import dev.esgenius.service.DocumentAccessPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class MeController {

    private final DocumentAccessPolicy documentAccessPolicy;
    private final UsageLimiter usageLimiter;

    public MeController(DocumentAccessPolicy documentAccessPolicy, UsageLimiter usageLimiter) {
        this.documentAccessPolicy = documentAccessPolicy;
        this.usageLimiter = usageLimiter;
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(HttpServletRequest request) {
        Optional<AuthenticatedUser> identity = AuthenticatedUser.from(request);
        Caller caller = documentAccessPolicy.resolve(identity);
        UsageLimiter.PerUserLimitSnapshot snapshot = usageLimiter.snapshot(caller);
        String email = identity.map(AuthenticatedUser::email).orElse(null);
        return ResponseEntity.ok(new MeResponse(
                email,
                caller.admin(),
                new MeResponse.Limits(
                        toLimit(snapshot.uploadsPerDay()),
                        toLimit(snapshot.analysesPerDay()),
                        toLimit(snapshot.assistantAsksPerHour()))));
    }

    private static MeResponse.Limit toLimit(UsageLimiter.LimitWindow window) {
        if (window == null) {
            return null;
        }
        return new MeResponse.Limit(window.limit(), window.used(), window.resetsInSeconds());
    }
}
