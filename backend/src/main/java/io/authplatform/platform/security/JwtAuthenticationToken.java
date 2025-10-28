package io.authplatform.platform.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.UUID;

/**
 * Authentication token for JWT-based authentication.
 *
 * <p>This token represents a successfully authenticated user via JWT.
 * It contains:
 * <ul>
 *   <li>User ID from the Users table</li>
 *   <li>Organization ID for multi-tenant isolation</li>
 *   <li>Granted authorities (roles) from JWT claims</li>
 *   <li>Original JWT for audit and additional claims access</li>
 * </ul>
 *
 * <p>This token is set in the SecurityContext after successful JWT validation
 * and user provisioning (JIT - Just-In-Time).
 *
 * <h2>Usage Example</h2>
 * <pre>
 * // In JwtAuthenticationFilter after successful validation
 * JwtAuthenticationToken authentication = new JwtAuthenticationToken(
 *     user.getId(),
 *     organizationId,
 *     authorities,
 *     decodedJwt
 * );
 * authentication.setAuthenticated(true);
 * SecurityContextHolder.getContext().setAuthentication(authentication);
 * </pre>
 *
 * <h2>Accessing in Controllers</h2>
 * <pre>
 * &#64;GetMapping("/api/v1/profile")
 * public ResponseEntity&lt;UserProfile&gt; getProfile() {
 *     JwtAuthenticationToken auth = (JwtAuthenticationToken)
 *         SecurityContextHolder.getContext().getAuthentication();
 *
 *     UUID userId = auth.getUserId();
 *     String organizationId = auth.getOrganizationId();
 *     Jwt jwt = auth.getJwt();
 *
 *     return ResponseEntity.ok(profileService.getProfile(userId));
 * }
 * </pre>
 *
 * @see org.springframework.security.core.Authentication
 * @see org.springframework.security.oauth2.jwt.Jwt
 * @since 0.2.0
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID userId;
    private final String organizationId;
    private final Jwt jwt;

    /**
     * Constructs a JWT authentication token.
     *
     * @param userId User ID from the Users table
     * @param organizationId Organization ID for multi-tenant isolation
     * @param authorities Granted authorities (roles) from JWT claims
     * @param jwt Original JWT token
     */
    public JwtAuthenticationToken(
            UUID userId,
            String organizationId,
            Collection<? extends GrantedAuthority> authorities,
            Jwt jwt) {
        super(authorities);
        this.userId = userId;
        this.organizationId = organizationId;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    /**
     * Returns the credentials (JWT token).
     *
     * @return JWT token
     */
    @Override
    public Object getCredentials() {
        return jwt;
    }

    /**
     * Returns the principal (user ID).
     *
     * @return User ID from the Users table
     */
    @Override
    public Object getPrincipal() {
        return userId;
    }

    /**
     * Gets the user ID.
     *
     * @return User ID
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * Gets the organization ID for multi-tenant isolation.
     *
     * @return Organization ID
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * Gets the original JWT token.
     *
     * <p>The JWT contains additional claims that may be useful:
     * <ul>
     *   <li>sub: Keycloak user ID</li>
     *   <li>email: User email address</li>
     *   <li>preferred_username: Username</li>
     *   <li>roles: User roles</li>
     *   <li>exp: Expiration timestamp</li>
     *   <li>iat: Issued-at timestamp</li>
     * </ul>
     *
     * @return JWT token
     */
    public Jwt getJwt() {
        return jwt;
    }

    @Override
    public String toString() {
        return "JwtAuthenticationToken{" +
                "userId=" + userId +
                ", organizationId='" + organizationId + '\'' +
                ", authorities=" + getAuthorities() +
                ", authenticated=" + isAuthenticated() +
                '}';
    }
}
