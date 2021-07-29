package at.zieserl.astrodiscordbot.feature.removerole;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.RoleController;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RemoveRoleCommandListener extends ListenerAdapter {
    private final DiscordBot discordBot;

    private RemoveRoleCommandListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onSlashCommand(@NotNull final SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase("removerole")) {
            return;
        }
        final Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
        final Role role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        assert member != null : "Could not get member argument!";
        RoleController.removeRole(member, role.getId());
        event.reply("Die Rolle wurde entfernt.").queue();
    }

    public static RemoveRoleCommandListener forBot(final DiscordBot discordBot) {
        return new RemoveRoleCommandListener(discordBot);
    }
}
