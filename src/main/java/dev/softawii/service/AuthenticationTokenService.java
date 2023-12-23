package dev.softawii.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.softawii.entity.AuthenticationToken;
import dev.softawii.entity.Student;
import dev.softawii.exceptions.*;
import dev.softawii.repository.AuthenticationTokenRepository;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class AuthenticationTokenService {

    private static final Logger                        LOGGER = LoggerFactory.getLogger(AuthenticationTokenService.class);
    private final        StudentService                studentService;
    private final        AuthenticationTokenRepository authenticationTokenRepository;
    private final        EmailTemplateService          emailTemplateService;
    private final        Pattern                       ruralPattern;
    private final        Pattern                       gmailPattern;
    private final        String                        gmailRegex;
    private final        int                           tokenLength;
    private final        EmailService                  emailService;
    private final        Cache<Long, Long>             tokenValidationTentativesCache; // userDiscordId -> count
    private final        Cache<Long, Boolean>          tokenValidationBanCache; // userDiscordId -> unused
    private final        int                           maxValidationTentatives;

    public AuthenticationTokenService(
            @Value("${email_domain:ufrrj.br}") String emailDomain,
            @Value("${token_length:6}") int tokenLength,
            StudentService studentService,
            AuthenticationTokenRepository authenticationTokenRepository,
            EmailTemplateService emailTemplateService, EmailService emailService
    ) {
        this.emailTemplateService = emailTemplateService;
        this.emailService = emailService;
        this.gmailRegex = "\\+(.*?)@";
        this.gmailPattern = Pattern.compile(this.gmailRegex);
        this.ruralPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@ufrrj\\.br$");
        this.tokenLength = tokenLength;
        this.studentService = studentService;
        this.authenticationTokenRepository = authenticationTokenRepository;
        this.tokenValidationTentativesCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        this.tokenValidationBanCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        this.maxValidationTentatives = 3;
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
    public void generateToken(User user, GuildMessageChannelUnion channel, Long userDiscordId, String email) throws InvalidEmailException, AlreadyVerifiedException, EmailAlreadyInUseException, RateLimitException, FailedToSendEmailException {
        email = processEmail(email);
        if (!isValidEmail(email)) throw new InvalidEmailException("Invalid email");
        if (checkExistingDiscordId(userDiscordId)) throw new AlreadyVerifiedException("You are already verified");
        if (checkExistingEmail(email)) throw new EmailAlreadyInUseException("Email already in use");
        String token = generateRandomToken();

        if (sendEmail(user, channel, email, token)) {
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

        if (authTokenOptional.isEmpty()) {
            computeValidationTentative(userDiscordId);
            throw new TokenNotFoundException();
        }

        AuthenticationToken authToken = authTokenOptional.get();
        authToken.setUsed(true);
        authenticationTokenRepository.saveAndFlush(authToken);
        studentService.createStudent(authToken.getDiscordUserId(), authToken.getEmail());
    }

    private void computeValidationTentative(Long userDiscordId) {
        Long tentatives    = this.tokenValidationTentativesCache.get(userDiscordId, key -> 1L);
        Long newTentatives = tentatives + 1;
        this.tokenValidationTentativesCache.put(userDiscordId, newTentatives);
        if (newTentatives >= maxValidationTentatives) {
            this.tokenValidationBanCache.put(userDiscordId, Boolean.TRUE);
            this.tokenValidationTentativesCache.invalidate(userDiscordId);
            LOGGER.info(String.format("User '%d' is rate limited", userDiscordId));
        }
    }

    private void saveToken(Long userDiscordId, String email, String token) {
        ZonedDateTime createdAt = ZonedDateTime.now();
        ZonedDateTime expiresAt = createdAt.plusMinutes(5);
        this.authenticationTokenRepository.saveAndFlush(new AuthenticationToken(userDiscordId, email, token, createdAt, expiresAt));
    }

    private Optional<Student> getStudentByEmail(String email) {
        return this.studentService.findByEmail(email);
    }

    private Optional<Student> getStudentByDiscordId(Long discordUserId) {
        return this.studentService.findByDiscordId(discordUserId);
    }

    private boolean sendEmail(User user, GuildMessageChannelUnion channel, String to, String token) throws FailedToSendEmailException {
        String name      = user.getName();
        String avatarUrl = user.getEffectiveAvatarUrl();

        Invite invite = channel.asTextChannel()
                .createInvite()
                .setUnique(Boolean.TRUE)
                .deadline(System.currentTimeMillis() + 600000)
                .complete();

        ZonedDateTime     now       = ZonedDateTime.now(ZoneId.of("GMT-3"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss Z");

        String template = emailTemplateService.parseTemplate("send-token", Map.of(
                ":link-user-avatar:", avatarUrl,
                ":link-name:", name,
                ":link-discord-invite:", invite.getUrl(),
                ":link-token:", token,
                ":link-date:", now.format(formatter)
        ));

        return emailService.enqueue(to, "Authentication Token", template);
    }

    public boolean isRateLimited(Long discordUserId) {
        return this.tokenValidationBanCache.getIfPresent(discordUserId) != null;
    }
}
