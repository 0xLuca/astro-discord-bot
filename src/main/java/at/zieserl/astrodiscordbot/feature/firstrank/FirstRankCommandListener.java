package at.zieserl.astrodiscordbot.feature.firstrank;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.RoleController;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class FirstRankCommandListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String firstRankCommandName;
    private final List<String> firstRankCommandRoleIds;
    private final String firstRankCommandChannelId;

    private FirstRankCommandListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.firstRankCommandName = discordBot.getBotConfig().retrieveValue("first-rank-command-name");
        this.firstRankCommandRoleIds = Arrays.asList(retrieveFirstRankCommandRoles(discordBot));
        this.firstRankCommandChannelId = discordBot.getBotConfig().retrieveValue("first-rank-command-channel");
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase(firstRankCommandName)) {
            return;
        }
        final Member member = event.getMember();
        assert member != null : "Unknown member used first rank command";
        grantFirstRankRoles(member);
        event.reply(discordBot.getMessageStore().provide("first-rank-command-success")).queue();
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getMessage().getContentRaw().trim().equalsIgnoreCase("!" + firstRankCommandName)) {
            return;
        }
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        grantFirstRankRoles(member);
        event.getChannel().sendMessage(discordBot.getMessageStore().provide("first-rank-command-success")).queue();
    }

    private String[] retrieveFirstRankCommandRoles(DiscordBot discordBot) {
        return Arrays.stream(discordBot.getBotConfig().retrieveValue("first-rank-command-roles").split(",")).map(String::trim).toArray(String[]::new);
    }

    private void grantFirstRankRoles(Member member) {
        firstRankCommandRoleIds.forEach(roleId -> RoleController.grantRole(member, roleId));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(firstRankCommandChannelId);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(GenericGuildMessageEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(firstRankCommandChannelId);
    }

    public static FirstRankCommandListener forBot(DiscordBot bot) {
        return new FirstRankCommandListener(bot);
    }
}
