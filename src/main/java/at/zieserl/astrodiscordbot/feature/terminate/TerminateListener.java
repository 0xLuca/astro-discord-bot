package at.zieserl.astrodiscordbot.feature.terminate;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class TerminateListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String terminateCommandName;
    private final String adminCommandsChannelId;

    private TerminateListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.terminateCommandName = discordBot.getBotConfig().retrieveValue("terminate-command-name");
        this.adminCommandsChannelId = discordBot.getBotConfig().retrieveValue("admin-commands-channel");
    }

    @Override
    public void onSlashCommand(@NotNull final SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase(terminateCommandName)) {
            return;
        }
        final Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
        final String reason = Objects.requireNonNull(event.getOption("reason")).getAsString();
        assert member != null;
        discordBot.getInformationGrabber().findEmployeeByDiscordId(member.getId()).thenAccept(optionalEmployee -> optionalEmployee.ifPresent(employee -> {
            discordBot.getInformationGrabber().deleteEmployee(employee);
            discordBot.getActiveGuild().kick(member).queue();
            discordBot.getLogController().postTermination(member, reason);
        }));
    }

    @Override
    public void onGuildMemberRemove(@NotNull final GuildMemberRemoveEvent event) {
        if (!discordBot.shouldHandleEvent(event)) {
            return;
        }
        final User user = event.getUser();
        discordBot.getInformationGrabber().findEmployeeByDiscordId(user.getId()).thenAccept(optionalEmployee -> optionalEmployee.ifPresent(employee -> {
            if (discordBot.getInformationGrabber().isRegistered(user.getId())) {
                discordBot.getInformationGrabber().deleteEmployee(employee);
                discordBot.getLogController().postSelfTermination(user);
            }
        }));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(final GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(adminCommandsChannelId);
    }

    public static TerminateListener forBot(final DiscordBot discordBot) {
        return new TerminateListener(discordBot);
    }
}