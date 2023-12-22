package dev.softawii.controller;

import com.softawii.curupira.annotations.IButton;
import com.softawii.curupira.annotations.ICommand;
import com.softawii.curupira.annotations.IGroup;
import com.softawii.curupira.annotations.IModal;
import dev.softawii.exceptions.AlreadyVerifiedException;
import dev.softawii.exceptions.EmailAlreadyInUseException;
import dev.softawii.exceptions.InvalidEmailException;
import dev.softawii.exceptions.RateLimitException;
import dev.softawii.service.StudentService;
import dev.softawii.service.TokenGeneratorService;
import io.micronaut.context.annotation.Context;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.Objects;

/**
 * This controller is responsible for generating tokens for users to verify their email addresses.
 *
 * Will listen to the button click in Discord and generate a token for the user.
 */

@IGroup(name = "token", description = "Token generator", hidden = false)
@Context
public class TokenGeneratorController {

    private static final String TOKEN_GENERATOR_MESSAGE = "dev-sa-token-generator-message";
    private static final String MODAL_RESPONSE_CALLBACK = "dev-sa-modal-generator-message";
    private static StudentService studentService;
    private static TokenGeneratorService tokenGeneratorService;

    public TokenGeneratorController(
            StudentService studentService,
            TokenGeneratorService tokenGeneratorService
    ) {
        TokenGeneratorController.studentService = studentService;
        TokenGeneratorController.tokenGeneratorService = tokenGeneratorService;
    }

    @ICommand(name="setup", description = "Setup the token generator message", permissions = {Permission.ADMINISTRATOR})
    public static void SendMessage(SlashCommandInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        Message message = channel.sendMessage("Click the button below to generate a token.")
                .addActionRow(Button.primary(TOKEN_GENERATOR_MESSAGE, "Authentication"))
                .complete();
        event.reply("Setup finished.").setEphemeral(true).queue();
    }

    @IButton(id=TOKEN_GENERATOR_MESSAGE)
    public static void GenerateModal(ButtonInteractionEvent event) {
        Modal.Builder builder = Modal.create(MODAL_RESPONSE_CALLBACK, "Authentication");
        // Add Email Input
        builder.addActionRow(TextInput.create("email", "Email", TextInputStyle.SHORT)
                .setPlaceholder("Enter your email address")
                .setMaxLength(100)
                .setMinLength(5)
                .setRequired(true)
                .build());
        event.replyModal(builder.build()).queue();
    }

    /**
     * This method will be called when the user confirms the modal interaction.
     *
     * Needs to:
     * 1. Check if the email is valid (@ufrrj.br)
     * 2. Check if the email is already registered
     * 3. Generate a token
     * 4. Send the token to the user's email
     * 5. Send a message to the user saying that the token was sent to the email.
     *
     */
    @IModal(id=MODAL_RESPONSE_CALLBACK)
    public static void ProcessModalCallback(ModalInteractionEvent event) {
        event.deferReply(true).queue();
        long   discordUserId = event.getUser().getIdLong();
        String email = Objects.requireNonNull(event.getValue("email")).getAsString();
        try {
            tokenGeneratorService.generateToken(discordUserId, email);
            event.getHook().setEphemeral(true).sendMessage("Email sent to: " + email).queue();
        } catch (InvalidEmailException e) {
            event.getHook().setEphemeral(true).sendMessage("Invalid email").queue();
        } catch (AlreadyVerifiedException e) {
            event.getHook().setEphemeral(true).sendMessage("Email already verified").queue();
        } catch (EmailAlreadyInUseException e) {
            event.getHook().setEphemeral(true).sendMessage("Email already in use").queue();
        } catch (RateLimitException e) {
            event.getHook().setEphemeral(true).sendMessage("Rate limited. Try again in a few minutes").queue();
        }
    }

}
