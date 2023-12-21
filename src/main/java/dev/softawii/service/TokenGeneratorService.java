package dev.softawii.service;

import dev.softawii.entity.AuthenticationToken;
import dev.softawii.entity.Student;
import dev.softawii.exceptions.AlreadyVerifiedException;
import dev.softawii.exceptions.EmailAlreadyInUseException;
import dev.softawii.exceptions.InvalidDomainEmailException;
import dev.softawii.exceptions.InvalidEmailException;
import dev.softawii.exceptions.RateLimitException;
import dev.softawii.repository.TokenGeneratorRepository;
import io.micronaut.context.annotation.Value;
import io.micronaut.email.Email;
import io.micronaut.email.MultipartBody;
import jakarta.inject.Singleton;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import net.dv8tion.jda.api.entities.User;

import java.io.UnsupportedEncodingException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class TokenGeneratorService {

    private final StudentService studentService;
    private final TokenGeneratorRepository tokenGeneratorRepository;
    private final Pattern ruralPattern;
    private final Pattern gmailPattern;
    private final String gmailRegex;
    private final int tokenLength;
    private final EmailService emailService;

    public TokenGeneratorService(
            @Value("${email_domain}") String emailDomain,
            @Value("${token_length:6}") int tokenLength,
            StudentService studentService,
            TokenGeneratorRepository tokenGeneratorRepository,
            EmailService emailService
    ) {
        this.gmailRegex     = "\\+(.*?)@";
        this.gmailPattern   = Pattern.compile(this.gmailRegex);
        this.ruralPattern   = Pattern.compile("^[a-zA-Z0-9._%+-]+@ufrrj\\.br$");
        this.tokenLength    = tokenLength;
        this.studentService = studentService;
        this.tokenGeneratorRepository = tokenGeneratorRepository;
        this.emailService = emailService;
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
    public void generateToken(Long userDiscordId, String email) throws InvalidEmailException, AlreadyVerifiedException, EmailAlreadyInUseException, RateLimitException {
        email = processEmail(email);
        if(!isValidEmail(email)) throw new InvalidEmailException("Invalid email");

        Student student = getStudent(userDiscordId, email);
        if(student.isVerified()) throw new AlreadyVerifiedException("User is already verified");
        if (this.tokenGeneratorRepository.validTokenExists(userDiscordId)) throw new RateLimitException();

        String token = generateRandomToken();
        saveToken(student, token);
        sendEmail(email, token);
    }

    private void saveToken(Student student, String token) {
        ZonedDateTime createdAt = ZonedDateTime.now();
        ZonedDateTime expiresAt = createdAt.plusMinutes(5);
        this.tokenGeneratorRepository.saveAndFlush(new AuthenticationToken(student, token, createdAt, expiresAt));
    }

    private Student getStudent(Long userDiscordId, String email) throws EmailAlreadyInUseException {
        Optional<Student> optionalEmailStudent = this.studentService.findByEmail(email);
        Student student;
        if (optionalEmailStudent.isPresent()) {
            if (!optionalEmailStudent.get().getDiscordUserId().equals(userDiscordId)) throw new EmailAlreadyInUseException();

            student = optionalEmailStudent.get();
        } else {
            student = this.studentService.createStudent(userDiscordId, email);
        }
        return student;
    }

    private void sendEmail(String email, String token) {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("admin@example.com", "Example.com Admin"));
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress("user@example.com", "Mr. User"));
            msg.setSubject("Your Example.com account has been activated");
            msg.setText("This is a test");
            Transport.send(msg);
        } catch (MessagingException | UnsupportedEncodingException e) {
            // ...
        }
    }

    private void send(String email, String token) {
        emailService.send(email, token);
    }

}
