package at.zieserl.astrodiscordbot.feature.update;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class UpdateCommandListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final TextChannel botCommandsTextChannel;

    private UpdateCommandListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.botCommandsTextChannel = discordBot.getActiveGuild().getTextChannelById(discordBot.getBotConfig().retrieveValue("first-rank-command-channel"));
    }

    @Override
    public void onSlashCommand(@NotNull final SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase("update")) {
            return;
        }
        final Member member = event.getMember();
        assert member != null : "Unknown member used first rank command";
        final String phoneNumber = Objects.requireNonNull(event.getOption("phone_number")).getAsString();
        final String birthDate = Objects.requireNonNull(event.getOption("birth_date")).getAsString();
        discordBot.getInformationGrabber().findEmployeeByDiscordId(member.getId()).thenAccept(optionalEmployee -> {
            optionalEmployee.ifPresent(employee -> {
                employee.setPhoneNumber(phoneNumber);
                employee.setBirthDate(birthDate);
                discordBot.getInformationGrabber().saveEmployeeData(employee);
                event.reply("Deine Daten wurden gespeichert.").queue();
            });
        });
    }

    private boolean shouldHandleEvent(final GenericInteractionCreateEvent event) {
        return event.getTextChannel().equals(botCommandsTextChannel);
    }

    public static UpdateCommandListener forBot(final DiscordBot discordBot) {
        return new UpdateCommandListener(discordBot);
    }
}
