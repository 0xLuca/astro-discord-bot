package at.zieserl.astrodiscordbot.feature.worktime;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.RoleController;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public final class WorktimeListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String reactionEmote;
    private final String outOfServiceRoleId;
    private final String dienstmeldungChannelId;
    private final String adminLogsChannelId;
    private final Map<Long, Long> lastSessions = new HashMap<>();

    private WorktimeListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.reactionEmote = discordBot.getBotConfig().retrieveValue("dienstmeldung-reaction-emoji");
        this.outOfServiceRoleId = discordBot.getBotConfig().retrieveValue("out-of-service-role");
        this.dienstmeldungChannelId = discordBot.getBotConfig().retrieveValue("dienstmeldung-channel");
        this.adminLogsChannelId = discordBot.getBotConfig().retrieveValue("admin-logs-channel");
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getReaction().getReactionEmote().getName().equals(reactionEmote)) {
            return;
        }
        Member member = event.retrieveMember().complete();
        if (member.equals(event.getGuild().getSelfMember())) {
            return;
        }
        lastSessions.put(member.getUser().getIdLong(), System.currentTimeMillis());
        RoleController.removeRole(member, outOfServiceRoleId);

        final TextChannel channel = event.getGuild().getTextChannelById(adminLogsChannelId);
        final EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(discordBot.getMessageStore().provide("now-at-work-title"));
        builder.setColor(Color.GREEN);
        builder.setDescription(discordBot.getMessageStore().provide("now-at-work-message").replace("%mention%", member.getAsMention()));
        String avatarUrl = member.getUser().getAvatarUrl();
        if (avatarUrl == null) {
            avatarUrl = member.getUser().getDefaultAvatarUrl();
        }
        builder.setThumbnail(avatarUrl);
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getAvatarUrl());

        assert channel != null : "Could not find logs channel";
        channel.sendMessage(builder.build()).queue();
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getReaction().getReactionEmote().getName().equals(reactionEmote)) {
            return;
        }
        Member member = event.retrieveMember().complete();
        if (member.equals(event.getGuild().getSelfMember())) {
            return;
        }
        RoleController.grantRole(member, outOfServiceRoleId);

        final TextChannel channel = event.getGuild().getTextChannelById(adminLogsChannelId);
        final EmbedBuilder builder = new EmbedBuilder();
        final long sessionStartTime = lastSessions.getOrDefault(member.getUser().getIdLong(), 0L);
        final long sessionTime = System.currentTimeMillis() - sessionStartTime;
        if (sessionStartTime != 0 && sessionTime != 0) {
            
        }
        final long seconds = sessionTime / 1000;
        final String formattedSessionTime =
                sessionStartTime > 0 ?
                        String.format("%d Stunde(n), %d Minute(n), %d Sekunde(n)", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
                        : "Unbekannt";

        builder.setTitle(discordBot.getMessageStore().provide("no-longer-at-work-title"));
        builder.setColor(Color.RED);
        builder.setDescription(discordBot.getMessageStore().provide("no-longer-at-work-message").replace("%mention%", member.getAsMention()).replace("%time%", formattedSessionTime));
        String avatarUrl = member.getUser().getAvatarUrl();
        if (avatarUrl == null) {
            avatarUrl = member.getUser().getDefaultAvatarUrl();
        }
        builder.setThumbnail(avatarUrl);
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getAvatarUrl());

        assert channel != null : "Could not find logs channel";
        channel.sendMessage(builder.build()).queue();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(GenericMessageEvent event) {
        return event.getTextChannel().getId().equals(dienstmeldungChannelId);
    }

    public static WorktimeListener forBot(DiscordBot discordBot) {
        return new WorktimeListener(discordBot);
    }
}
