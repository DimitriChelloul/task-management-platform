Security guide — JWT basics and integration (lab)

Goal: secure the API endpoints with JWT authentication for the lab. We'll implement a simple login endpoint that returns a signed JWT and protect /tasks endpoint to require a valid token.

1) Dependencies (pom)

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.11.5</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.11.5</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.11.5</version>
  <scope>runtime</scope>
</dependency>
```

2) Simple auth controller (for demo only)

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final String secret = "replace-with-secure-random-key";

    @PostMapping("/login")
    public Map<String,String> login(@RequestBody AuthRequest req) {
        // In lab: accept any username/password or a fixed user for demo
        String jws = Jwts.builder()
            .setSubject(req.getUsername())
            .claim("roles", List.of("USER"))
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(2, ChronoUnit.HOURS)))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
            .compact();
        return Map.of("token", jws);
    }
}
```

3) Spring Security config (resource server style simplified)

```java
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests()
            .antMatchers("/auth/**","/actuator/health").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilter(new JwtAuthenticationFilter(authenticationManager()));
    }
}
```

4) JWT filter (validate token)
- Filter extracts `Authorization` header, validates signature, sets authentication in SecurityContext.

5) Notes & production advice
- NEVER store secrets in plaintext; use Vault or environment variables.
- Prefer OAuth2/OpenID Connect for production (Keycloak, Auth0).
- Add refresh tokens if needed.

Lab exercise extension
- Add roles (ADMIN/USER) and protect DELETE endpoints for ADMIN only.
- Implement login via a simple JDBC user repository.
