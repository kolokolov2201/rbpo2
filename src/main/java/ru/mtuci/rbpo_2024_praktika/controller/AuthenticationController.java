package ru.mtuci.rbpo_2024_praktika.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.rbpo_2024_praktika.configuration.JwtTokenProvider;
import ru.mtuci.rbpo_2024_praktika.model.ApplicationUser;
import ru.mtuci.rbpo_2024_praktika.request.AuthenticationRequest;
import ru.mtuci.rbpo_2024_praktika.model.AuthenticationResponse;
import ru.mtuci.rbpo_2024_praktika.repository.UserRepository;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated

public class AuthenticationController {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        try {
            String email = request.getEmail();

            ApplicationUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            String token = jwtTokenProvider.createToken(email, user.getRole().getGrantedAuthorities());

            return ResponseEntity.ok(new AuthenticationResponse(email, token));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }


    @PostMapping("/reg")
    public ResponseEntity<?> reg(@RequestBody AuthenticationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User with this email already exists");
        }

        ApplicationUser newUser = new ApplicationUser();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(hashPassword(request.getPassword())); // Хеширование пароля
        newUser.setRole(request.getRole());
        newUser.setUsername(request.getUsername());

        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    private String hashPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    @PostMapping("/token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);

                if (jwtTokenProvider.validateToken(token)) {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && authentication.isAuthenticated()) {
                        return ResponseEntity.ok("Token is valid");
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid");
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Authorization header is missing or invalid");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while validating token");
        }
    }

}


