package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.LoginRequest;
import com.ecommerce.auth_service.dto.LoginResponse;
import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.dto.UserResponse;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.auth_service.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {

    	    if (userRepository.existsByUsername(request.username())) {
    	        throw new RuntimeException("Username already exists");
    	    }

    	    User user = new User();

    	    user.setUsername(request.username());

    	    user.setPassword(
    	            passwordEncoder.encode(request.password())
    	    );

    	    user.setRole(
    	            request.username().equals("admin")
    	                    ? "ADMIN"
    	                    : "USER"
    	    );

    	    User savedUser = userRepository.save(user);

    	    return new UserResponse(
    	            savedUser.getId(),
    	            savedUser.getUsername(),
    	            savedUser.getRole()
    	    );
    	}
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsername(request.username())
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or password"));

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPassword()
                );

        if (!passwordMatches) {
            throw new RuntimeException(
                    "Invalid username or password"
            );
        }

        String token =
                jwtService.generateToken(
                        user.getUsername(),
                        user.getRole()
                );

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole()
        );
    }
}