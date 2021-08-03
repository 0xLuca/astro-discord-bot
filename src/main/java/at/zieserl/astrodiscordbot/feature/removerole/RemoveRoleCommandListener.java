package at.zieserl.astrodiscordbot.feature.removerole;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.RoleController;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RemoveRoleCommandListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String adminCommandsChannelId;

    private RemoveRoleCommandListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.adminCommandsChannelId = discordBot.getBotConfig().retrieveValue("admin-commands-channel");
    }

    @Override
    public void onSlashCommand(@NotNull final SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase("removerole")) {
            return;
        }

        final Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
        final Role role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        assert member != null : "Could not get member argument!";
        RoleController.removeRole(member, role.getId());
        event.reply("Die Rolle wurde entfernt.").setEphemeral(true).queue();
    }

    private boolean shouldHandleEvent(final GenericInteractionCreateEvent event) {
        return event.getGuildChannel().getId().equals(adminCommandsChannelId);
    }

    public static RemoveRoleCommandListener forBot(final DiscordBot discordBot) {
        return new RemoveRoleCommandListener(discordBot);
    }
}
