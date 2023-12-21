package dev.softawii.controller;

import com.softawii.curupira.annotations.IButton;
import com.softawii.curupira.annotations.ICommand;
import com.softawii.curupira.annotations.IGroup;
import com.softawii.curupira.annotations.IModal;
import com.softawii.curupira.properties.Environment;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

/**
 * This controller is responsible for generating tokens for users to verify their email addresses.
 *
 * Will listen to the button click in Discord and generate a token for the user.
 */

@IGroup(name = "token", description = "Token generator", hidden = false)
public class TokenGeneratorController {

    private static final String TOKEN_GENERATOR_MESSAGE = "dev-sa-token-generator-message";
    private static final String MODAL_RESPONSE_CALLBACK = "dev-sa-modal-generator-message";

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

    @IModal(id=MODAL_RESPONSE_CALLBACK)
    public static void ProcessModalCallback(ModalInteractionEvent event) {
        String email = event.getValue("email").getAsString();
        String token = "hahahahahaha";
        event.reply("Your token is: " + token).queue();
    }


}
