package dev.softawii.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.softawii.entity.AuthenticationToken;
import dev.softawii.exceptions.*;
import dev.softawii.repository.AuthenticationTokenRepository;
import dev.softawii.util.EmailUtil;
import dev.softawii.util.EmbedUtil;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Singleton
public class AuthenticationTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationTokenService.class);

    private final StudentService                studentService;
    private final AuthenticationTokenRepository authenticationTokenRepository;
    private final EmailTemplateService          emailTemplateService;
    private final EmailService                  emailService;
    private final EventService                  eventService;

    private final EmailUtil emailUtil;
    private final EmbedUtil embedUtil;

    private final int tokenLength;
    private final int maxValidationTentatives;

    private final Cache<Long, Long>    tokenValidationTentativesCache; // userDiscordId -> count
    private final Cache<Long, Boolean> tokenValidationBanCache; // userDiscordId -> unused

    public AuthenticationTokenService(
            @Value("${email_domain:ufrrj.br}") String emailDomain,
            @Value("${token_length:6}") int tokenLength,
            StudentService studentService,
            AuthenticationTokenRepository authenticationTokenRepository,
            EmailTemplateService emailTemplateService, EmailService emailService,
            EventService eventService, EmailUtil emailUtil, EmbedUtil embedUtil
    ) {
        this.emailTemplateService = emailTemplateService;
        this.emailService = emailService;
        this.tokenLength = tokenLength;
        this.studentService = studentService;
        this.authenticationTokenRepository = authenticationTokenRepository;
        this.eventService = eventService;
        this.emailUtil = emailUtil;
        this.embedUtil = embedUtil;
        this.tokenValidationTentativesCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        this.tokenValidationBanCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        this.maxValidationTentatives = 3;
    }

    private String generateRandomToken() {
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            long token = random.nextLong((long) Math.pow(10, tokenLength - 1), (long) Math.pow(10, tokenLength) - 1);
            return Long.toUnsignedString(token);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to generate random token", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks:
     * 1. Is valid email
     * 1. If the email is already used by another user
     * 2. If the user is already verified
     * 3. If the user has a valid token (not used or expired)
     */
    public void generateToken(User user, GuildMessageChannelUnion channel, Long userDiscordId, String email) throws InvalidEmailException, AlreadyVerifiedException, EmailAlreadyInUseException, RateLimitException, FailedToSendEmailException {
        email = this.emailUtil.processEmail(email);
        if (!this.emailUtil.isValidEmail(email)) throw new InvalidEmailException("Invalid email");
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
        return this.studentService.findByDiscordId(userDiscordId).isPresent();
    }

    public boolean checkExistingEmail(String email) {
        return this.studentService.findByEmail(email).isPresent();
    }

    public boolean checkTokenAlreadyGenerated(Long discordId) {
        Optional<AuthenticationToken> optionalAuthenticationToken = this.authenticationTokenRepository.validTokenExists(discordId);
        return optionalAuthenticationToken.isPresent();
    }

    @Transactional
    public void validateToken(User user, Long userDiscordId, String token) throws TokenNotFoundException {
        Optional<AuthenticationToken> authTokenOptional = authenticationTokenRepository.findValidToken(token, userDiscordId);

        if (authTokenOptional.isEmpty()) {
            computeValidationTentative(user, userDiscordId);
            throw new TokenNotFoundException();
        }

        AuthenticationToken authToken = authTokenOptional.get();
        authToken.setUsed(true);
        authenticationTokenRepository.saveAndFlush(authToken);
        studentService.createStudent(authToken.getDiscordUserId(), authToken.getEmail());
        this.eventService.dispatch(this.embedUtil.generate(EmbedUtil.EmbedLevel.SUCCESS, "User Authenticated", "User <@!%s> authenticated".formatted(userDiscordId), user));
    }

    private void computeValidationTentative(User user, Long userDiscordId) {
        Long tentatives    = this.tokenValidationTentativesCache.get(userDiscordId, key -> 1L);
        Long newTentatives = tentatives + 1;
        this.tokenValidationTentativesCache.put(userDiscordId, newTentatives);
        if (newTentatives >= maxValidationTentatives) {
            this.tokenValidationBanCache.put(userDiscordId, Boolean.TRUE);
            this.tokenValidationTentativesCache.invalidate(userDiscordId);
            LOGGER.info(String.format("User '%d' is rate limited", userDiscordId));
            this.eventService.dispatch(this.embedUtil.generate(EmbedUtil.EmbedLevel.WARNING, "Rate Limit", "User <@!%s> is rate limited".formatted(userDiscordId), user));

        }
    }

    private void saveToken(Long userDiscordId, String email, String token) {
        ZonedDateTime createdAt = ZonedDateTime.now();
        ZonedDateTime expiresAt = createdAt.plusMinutes(5);
        this.authenticationTokenRepository.saveAndFlush(new AuthenticationToken(userDiscordId, email, token, createdAt, expiresAt));
    }

    private boolean sendEmail(User user, GuildMessageChannelUnion channel, String to, String token) throws FailedToSendEmailException {
        String name      = user.getName();
        String avatarUrl = user.getEffectiveAvatarUrl();

        Invite invite = channel.asTextChannel()
                .createInvite()
                .setUnique(Boolean.TRUE)
                .deadline(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(60))
                .complete();

        ZonedDateTime     now       = ZonedDateTime.now(ZoneId.of("GMT-3"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss O");

        String template = emailTemplateService.parseTemplate("send-token", Map.of(
                ":link-user-avatar:", avatarUrl,
                ":link-name:", name,
                ":link-discord-invite:", invite.getUrl(),
                ":link-token:", token,
                ":link-date:", now.format(formatter)
        ));

        return emailService.enqueue(to, "Token de Autenticação - DCC Discord Server", template);
    }

    public boolean isRateLimited(Long discordUserId) {
        return this.tokenValidationBanCache.getIfPresent(discordUserId) != null;
    }
}
