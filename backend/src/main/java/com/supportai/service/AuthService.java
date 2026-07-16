package com.supportai.service;

import com.supportai.dto.AuthResponse;
import com.supportai.dto.LoginRequest;
import com.supportai.dto.RegisterRequest;
import com.supportai.dto.UserProfileResponse;
import com.supportai.entity.Company;
import com.supportai.entity.User;
import com.supportai.enums.RoleType;
import com.supportai.exception.BadRequestException;
import com.supportai.exception.UnauthorizedException;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.UserRepository;
import com.supportai.security.JwtTokenProvider;
import com.supportai.util.InputNormalizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            CompanyService companyService,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companyService = companyService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = InputNormalizer.normalizeEmail(request.email());
        String firstName = InputNormalizer.trimToNull(request.firstName());
        String lastName = InputNormalizer.trimToNull(request.lastName());
        String companyName = InputNormalizer.trimToNull(request.companyName());

        if (firstName == null || lastName == null || companyName == null) {
            throw new BadRequestException("First name, last name, and company name are required");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        String slug = companyName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isEmpty()) {
            throw new BadRequestException("Company name must contain at least one letter or number");
        }

        if (companyRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Company company = new Company();
        company.setName(companyName);
        company.setSlug(slug);
        company.setAiSystemPrompt("You are a helpful customer support agent. Answer questions using the company documentation.");
        companyRepository.save(company);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(RoleType.ADMIN);
        user.setCompany(company);
        userRepository.save(user);

        companyService.createMembership(user, company, RoleType.ADMIN);

        return issueToken(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = InputNormalizer.normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (BadCredentialsException | DisabledException ex) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is disabled");
        }

        return issueToken(user);
    }

    public UserProfileResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is disabled");
        }

        return toUserProfile(user);
    }

    private AuthResponse issueToken(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);
        return toAuthResponse(token, user);
    }

    private AuthResponse toAuthResponse(String token, User user) {
        RoleType role = companyService.resolveRole(user);
        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role.name(),
                user.getCompany() != null ? user.getCompany().getId() : null
        );
    }

    private UserProfileResponse toUserProfile(User user) {
        Company company = user.getCompany();
        RoleType role = companyService.resolveRole(user);
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role.name(),
                company != null ? company.getId() : null,
                company != null ? company.getName() : null,
                user.isEmailVerified()
        );
    }
}
