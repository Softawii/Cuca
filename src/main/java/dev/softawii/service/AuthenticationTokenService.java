package dev.softawii.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.softawii.entity.AuthenticationToken;
import dev.softawii.entity.Student;
import dev.softawii.exceptions.*;
import dev.softawii.repository.AuthenticationTokenRepository;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class AuthenticationTokenService {

    private final StudentService                studentService;
    private final AuthenticationTokenRepository authenticationTokenRepository;
    private final Pattern                       ruralPattern;
    private final Pattern                       gmailPattern;
    private final String                        gmailRegex;
    private final int                           tokenLength;
    private final EmailService                  emailService;
//    LoadingCache<String, String> graphs = Caffeine.newBuilder()
//            .expireAfterAccess(5, TimeUnit.MINUTES)
//            .build();

    public AuthenticationTokenService(
            @Value("${email_domain:ufrrj.br}") String emailDomain,
            @Value("${token_length:6}") int tokenLength,
            StudentService studentService,
            AuthenticationTokenRepository authenticationTokenRepository,
            EmailService emailService) {
        this.emailService = emailService;
        this.gmailRegex = "\\+(.*?)@";
        this.gmailPattern = Pattern.compile(this.gmailRegex);
        this.ruralPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@ufrrj\\.br$");
        this.tokenLength = tokenLength;
        this.studentService = studentService;
        this.authenticationTokenRepository = authenticationTokenRepository;
    }

    /**
     * Check if the email is from the specified domain
     *
     * @param email the email to check
     * @return result of the check
     */
    private boolean isValidEmail(String email) {
        Matcher matcher = this.ruralPattern.matcher(email);
        return matcher.matches();
    }

    /**
     * Process the email to remove the + and everything after it
     *
     * @param email the email to process
     * @return the processed email
     */
    private String processEmail(String email) {
        Matcher matcher = gmailPattern.matcher(email);

        if (matcher.find())
            return email.replaceAll(this.gmailRegex, "");
        return email;
    }

    private String generateRandomToken() {
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        return token.substring(0, Math.min(this.tokenLength, token.length()));
    }

    /**
     * Checks:
     * 1. Is valid email
     * 1. If the email is already used by another user
     * 2. If the user is already verified
     * 3. If the user has a valid token (not used or expired)
     */
    public void generateToken(Long userDiscordId, String email) throws InvalidEmailException, AlreadyVerifiedException, EmailAlreadyInUseException, RateLimitException, FailedToSendEmailException {
        email = processEmail(email);
        if (!isValidEmail(email)) throw new InvalidEmailException("Invalid email");
        if (checkExistingDiscordId(userDiscordId)) throw new AlreadyVerifiedException("You are already verified");
        if (checkExistingEmail(email)) throw new EmailAlreadyInUseException("Email already in use");

        String token = generateRandomToken();

        if(sendEmail(email, token)) {
            saveToken(userDiscordId, email, token);
        } else {
            throw new FailedToSendEmailException("Failed to send email");
        }
    }

    public boolean checkExistingDiscordId(Long userDiscordId) {
        return getStudentByDiscordId(userDiscordId).isPresent();
    }

    public boolean checkExistingEmail(String email) {
        return getStudentByEmail(email).isPresent();
    }

    public boolean checkTokenAlreadyGenerated(Long discordId) {
        Optional<AuthenticationToken> optionalAuthenticationToken = this.authenticationTokenRepository.validTokenExists(discordId);
        return optionalAuthenticationToken.isPresent();
    }

    @Transactional
    public void validateToken(Long userDiscordId, String token) throws TokenNotFoundException {

        Optional<AuthenticationToken> authTokenOptional = authenticationTokenRepository.findValidToken(token, userDiscordId);

        if (authTokenOptional.isEmpty()) throw new TokenNotFoundException();

        AuthenticationToken authToken = authTokenOptional.get();
        authToken.setUsed(true);
        authenticationTokenRepository.saveAndFlush(authToken);
        studentService.createStudent(authToken.getDiscordUserId(), authToken.getEmail());
    }

    private void saveToken(Long userDiscordId, String email, String token) {
        ZonedDateTime createdAt = ZonedDateTime.now();
        ZonedDateTime expiresAt = createdAt.plusMinutes(5);
        this.authenticationTokenRepository.saveAndFlush(new AuthenticationToken(userDiscordId, email, token, createdAt, expiresAt));
    }

    private Optional<Student> getStudentByEmail(String email) {
        return this.studentService.findByEmail(email);
    }

    private  Optional<Student> getStudentByDiscordId(Long discordUserId) {
        return this.studentService.findByDiscordId(discordUserId);
    }

    private boolean sendEmail(String to, String token) throws FailedToSendEmailException {
        return emailService.enqueue(to, "Authentication Token", token);
    }

}
