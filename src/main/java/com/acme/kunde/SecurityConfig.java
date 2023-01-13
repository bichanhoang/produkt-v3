/*
 * Copyright (C) 2022 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.acme.kunde;

import java.util.Map;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import static com.acme.kunde.rest.KundeGetController.NACHNAME_PATH;
import static com.acme.kunde.rest.KundeGetController.REST_PATH;
import static com.acme.kunde.security.AuthController.AUTH_PATH;
import static com.acme.kunde.security.Rolle.ACTUATOR;
import static com.acme.kunde.security.Rolle.ADMIN;
import static com.acme.kunde.security.Rolle.KUNDE;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

// https://github.com/spring-projects/spring-security/tree/master/samples
/**
 * Security-Konfiguration.
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
@SuppressWarnings({"TrailingComment", "MagicNumber", "RedundantSuppression"})
interface SecurityConfig {
    // https://foojay.io/today/how-to-do-password-hashing-in-java-applications-the-right-way
    int SALT_LENGTH = 32; // default: 16
    int HASH_LENGTH = 64; // default: 32
    int PARALLELISM = 1; // default: 1 (Bouncy Castle kann keine Parallelitaet)
    int MEMORY_CONSUMPTION_KBYTES = 1 << 14; // default: 2^14 KByte = 16 MiB  ("Memory Cost Parameter")
    int ITERATIONS = 3; // default: 3

    /**
     * Bean-Definition, um den Zugriffsschutz an der REST-Schnittstelle zu konfigurieren,
     * d.h. vor Anwendung von @PreAuthorize.
     *
     * @param http Injiziertes Objekt von HttpSecurity als Ausgangspunkt für die Konfiguration.
     * @return Objekt von SecurityFilterChain
     * @throws Exception Wegen HttpSecurity.authorizeHttpRequests()
     */
    @Bean
    @SuppressWarnings("LambdaBodyLength")
    default SecurityFilterChain securityFilterChainFn(final HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(authorize -> {
                // https://spring.io/blog/2019/11/21/spring-security-lambda-dsl
                final var restPathKundeId = REST_PATH + "/*";
                authorize
                    .requestMatchers(GET, REST_PATH).hasRole(ADMIN)
                    .requestMatchers(GET, restPathKundeId).hasAnyRole(ADMIN, KUNDE)
                    .requestMatchers(PUT, restPathKundeId).hasRole(ADMIN)
                    .requestMatchers(PATCH, restPathKundeId).hasRole(ADMIN)
                    .requestMatchers(DELETE, restPathKundeId).hasRole(ADMIN)
                    .requestMatchers(GET, "/swagger-ui.html").hasRole(ADMIN)
                    .requestMatchers(GET, AUTH_PATH + "/rollen", REST_PATH + NACHNAME_PATH + "/*").hasRole(KUNDE)
                    // Actuator: Health mit Liveness und Readiness wird von Kubernetes genutzt
                    .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                    // ggf. PrometheusScrapeEndpoint
                    .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole(ACTUATOR)

                    .requestMatchers(POST, REST_PATH).permitAll()
                    .requestMatchers(POST, "/graphql").permitAll()
                    .requestMatchers(GET, "/v3/api-docs.yaml").permitAll()
                    .requestMatchers(GET, "/v3/api-docs").permitAll()
                    .requestMatchers(GET, "/graphiql").permitAll()
                    .requestMatchers("/h2-console", "/h2-console/*").permitAll()
                    .requestMatchers(POST, AUTH_PATH + "/login").permitAll()
                    .requestMatchers("/error").permitAll()

                    .anyRequest().authenticated();
            })
            .httpBasic()
            .and()
            .formLogin().disable()
            .csrf().disable()
            .headers().frameOptions().sameOrigin()
            .and()
            .build();
    }

    /**
     * Bean-Definition, um den Verschlüsselungsalgorithmus für Passwörter bereitzustellen.
     * Es wird Argon2id statt bcrypt (Default-Algorithmus von Spring Security) verwendet.
     *
     * @return Objekt für die Verschlüsselung von Passwörtern.
     */
    @Bean
    default PasswordEncoder passwordEncoder() {
        // https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
        // https://github.com/OWASP/CheatSheetSeries/blob/master/cheatsheets/Password_Storage_Cheat_Sheet.md
        // https://www.rfc-editor.org/rfc/rfc9106.html
        final var idForEncode = "argon2id";
        final Map<String, PasswordEncoder> encoders = Map.of(
            idForEncode,
            new Argon2PasswordEncoder(
                SALT_LENGTH,
                HASH_LENGTH,
                PARALLELISM,
                MEMORY_CONSUMPTION_KBYTES,
                ITERATIONS
            )
        );
        return new DelegatingPasswordEncoder(idForEncode, encoders);
    }
}
