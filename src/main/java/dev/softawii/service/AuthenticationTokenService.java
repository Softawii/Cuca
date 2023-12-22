package dev.softawii.service;

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

        Student student = getStudent(userDiscordId, email);
        if (student.isVerified()) throw new AlreadyVerifiedException("User is already verified");
        if (this.authenticationTokenRepository.validTokenExists(userDiscordId)) throw new RateLimitException();

        String token = generateRandomToken();
        saveToken(student, token);
        sendEmail(email, token);
    }

    @Transactional
    public void validateToken(Long userDiscordId, String token) throws TokenNotFoundException {
        Optional<AuthenticationToken> authTokenOptional = authenticationTokenRepository.findValidToken(token, userDiscordId);
        if (authTokenOptional.isEmpty()) {
            throw new TokenNotFoundException();
        }
        AuthenticationToken authToken = authTokenOptional.get();
        authToken.setUsed(true);
        authenticationTokenRepository.saveAndFlush(authToken);
        studentService.verifyStudent(userDiscordId);
    }

    private void saveToken(Student student, String token) {
        ZonedDateTime createdAt = ZonedDateTime.now();
        ZonedDateTime expiresAt = createdAt.plusMinutes(5);
        this.authenticationTokenRepository.saveAndFlush(new AuthenticationToken(student, token, createdAt, expiresAt));
    }

    private Student getStudent(Long userDiscordId, String email) throws EmailAlreadyInUseException {
        Optional<Student> optionalEmailStudent = this.studentService.findByEmail(email);
        Student           student;
        if (optionalEmailStudent.isPresent()) {
            if (!optionalEmailStudent.get().getDiscordUserId().equals(userDiscordId))
                throw new EmailAlreadyInUseException();

            student = optionalEmailStudent.get();
        } else {
            student = this.studentService.createStudent(userDiscordId, email);
        }
        return student;
    }

    private void sendEmail(String to, String token) throws FailedToSendEmailException {
        emailService.send(to, "Authentication Token", token);
    }

}
