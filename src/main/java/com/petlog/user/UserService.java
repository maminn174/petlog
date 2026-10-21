package com.petlog.user;

import com.petlog.auth.exception.EmailAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            log.warn("User registration rejected: email already is use");
            throw new EmailAlreadyExistsException("This email is already in use");
        }

        String  passwordHash = passwordEncoder.encode(password);
        User user = new User(name, email, passwordHash);

        User savedUser = userRepository.save(user);

        log.info("User registered successfully: userId={}", savedUser.getId());

        return savedUser;
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                "User not found: " + id
            ));
    }

}
