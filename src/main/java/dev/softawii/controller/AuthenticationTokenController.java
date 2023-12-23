package dev.softawii.controller;

import com.softawii.curupira.annotations.IButton;
import com.softawii.curupira.annotations.ICommand;
import com.softawii.curupira.annotations.IGroup;
import com.softawii.curupira.annotations.IModal;
import dev.softawii.exceptions.*;
import dev.softawii.service.AuthenticationTokenService;
import io.micronaut.context.annotation.Context;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.Objects;

/**
 * This controller is responsible for generating tokens for users to verify their email addresses.
 * Will listen to the button click in Discord and generate a token for the user.
 */

@IGroup(name = "token", description = "Token generator", hidden = false)
@Context
public class AuthenticationTokenController {

    private static final String                     TOKEN_REGISTRATION_BUTTON   = "token.token-registration-button";
    private static final String                     TOKEN_AUTHENTICATION_BUTTON = "token.token-authentication-button";
    private static final String                     REGISTRATION_MODAL          = "token.registration-modal";
    private static final String                     AUTHENTICATION_MODAL        = "token.authentication-modal";
    private static       AuthenticationTokenService authenticationTokenService;

    public AuthenticationTokenController(AuthenticationTokenService authenticationTokenService) {
        AuthenticationTokenController.authenticationTokenService = authenticationTokenService;
    }

    @ICommand(name = "setup", description = "Setup the token generator message",
              permissions = {Permission.ADMINISTRATOR})
    public static void setup(SlashCommandInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        Message message = channel.sendMessage("Click the button below to generate a token.")
                .addActionRow(Button.primary(TOKEN_REGISTRATION_BUTTON, "Register"), Button.secondary(TOKEN_AUTHENTICATION_BUTTON, "Authenticate"))
                .complete();
        event.reply("Setup finished.").setEphemeral(true).queue();
    }

    @IButton(id = TOKEN_REGISTRATION_BUTTON)
    public static void generateRegistrationModal(ButtonInteractionEvent event) {
        long idLong = event.getUser().getIdLong();
        if (authenticationTokenService.checkExistingDiscordId(idLong)) {
            event.reply("You are already registered.").setEphemeral(true).queue();
            return;
        }

        if (authenticationTokenService.checkTokenAlreadyGenerated(idLong)) {
            event.reply("You already have a token generated. Check your email.").setEphemeral(true).queue();
            return;
        }

        Modal.Builder builder = Modal.create(REGISTRATION_MODAL, "Authentication");
        // Add Email Input
        builder.addActionRow(TextInput.create("email", "Email", TextInputStyle.SHORT)
                                     .setPlaceholder("Enter your email address")
                                     .setMaxLength(100)
                                     .setMinLength(5)
                                     .setRequired(true)
                                     .build());
        event.replyModal(builder.build()).queue();
    }

    @IButton(id = TOKEN_AUTHENTICATION_BUTTON)
    public static void generateAuthenticationModal(ButtonInteractionEvent event) {
        long idLong = event.getUser().getIdLong();
        if (authenticationTokenService.checkExistingDiscordId(idLong)) {
            event.reply("You are already registered.").setEphemeral(true).queue();
            return;
        }

        if (!authenticationTokenService.checkTokenAlreadyGenerated(idLong)) {
            event.reply("You don't have a token generated. Click the register button to generate one").setEphemeral(true).queue();
            return;
        }

        Modal.Builder builder = Modal.create(AUTHENTICATION_MODAL, "Authentication");
        // Add Email Input
        builder.addActionRow(TextInput.create("token", "Token", TextInputStyle.SHORT)
                                     .setPlaceholder("Enter your token")
                                     .setMaxLength(50)
                                     .setMinLength(1)
                                     .setRequired(true)
                                     .build());
        event.replyModal(builder.build()).queue();
    }

    /**
     * This method will be called when the user confirms the modal interaction. <br>
     * <br>
     * Needs to: <br>
     * 1. Check if the email is valid (@ufrrj.br) <br>
     * 2. Check if the email is already registered <br>
     * 3. Generate a token <br>
     * 4. Send the token to the user's email <br>
     * 5. Send a message to the user saying that the token was sent to the email. <br>
     */
    @IModal(id = REGISTRATION_MODAL)
    public static void processRegistrationModal(ModalInteractionEvent event) {
        event.deferReply(true).queue();
        long   discordUserId = event.getUser().getIdLong();
        String email         = Objects.requireNonNull(event.getValue("email")).getAsString();
        User   user          = event.getUser();

        InteractionHook hook = event.getHook().setEphemeral(true);
        try {
            authenticationTokenService.generateToken(user, discordUserId, email);
            hook.sendMessage("Email sent to: " + email).queue();
        } catch (InvalidEmailException e) {
            hook.sendMessage("Invalid email").queue();
        } catch (AlreadyVerifiedException | EmailAlreadyInUseException e) {
            hook.sendMessage(e.getMessage()).queue();
        } catch (RateLimitException e) {
            hook.sendMessage("Rate limited. Try again in a few minutes").queue();
        } catch (FailedToSendEmailException e) {
            hook.sendMessage("Failed to send email: " + email).queue();
        }
    }

    @IModal(id = AUTHENTICATION_MODAL)
    public static void processAuthenticationModal(ModalInteractionEvent event) {
        event.deferReply(true).queue();
        long   discordUserId = event.getUser().getIdLong();
        String token         = Objects.requireNonNull(event.getValue("token")).getAsString();
        try {
            authenticationTokenService.validateToken(discordUserId, token);
            event.getHook().setEphemeral(true).sendMessage("User verified").queue();
        } catch (TokenNotFoundException e) {
            event.getHook().setEphemeral(true).sendMessage("Invalid token").queue();
        }
    }

}
