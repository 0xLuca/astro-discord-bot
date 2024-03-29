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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public final class WorktimeListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String reactionEmote;
    private final String outOfServiceRoleId;
    private final String inServiceRoleId;
    private final String dienstmeldungChannelId;
    private final String adminLogsChannelId;
    private final Map<Long, Long> lastSessions = new HashMap<>();

    private WorktimeListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.reactionEmote = discordBot.getBotConfig().retrieveValue("dienstmeldung-reaction-emoji");
        this.outOfServiceRoleId = discordBot.getBotConfig().retrieveValue("out-of-service-role");
        this.inServiceRoleId = discordBot.getBotConfig().retrieveValue("in-service-role");
        this.dienstmeldungChannelId = discordBot.getBotConfig().retrieveValue("dienstmeldung-channel");
        this.adminLogsChannelId = discordBot.getBotConfig().retrieveValue("admin-logs-channel");
    }

    @Override
    public void onMessageReactionAdd(@NotNull final MessageReactionAddEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getReaction().getReactionEmote().getName().equals(reactionEmote)) {
            return;
        }
        final Member member = event.retrieveMember().complete();
        if (member.equals(event.getGuild().getSelfMember())) {
            return;
        }
        lastSessions.put(member.getUser().getIdLong(), System.currentTimeMillis());
        RoleController.removeRole(member, outOfServiceRoleId);
        RoleController.grantRole(member, inServiceRoleId);

        final TextChannel channel = event.getGuild().getTextChannelById(adminLogsChannelId);
        final EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(discordBot.getMessageStore().provide("now-at-work-title"));
        builder.setColor(Color.GREEN);
        builder.setDescription(discordBot.getMessageStore().provide("now-at-work-message").replace("%mention%", member.getAsMention()));
        builder.setThumbnail(member.getUser().getEffectiveAvatarUrl());
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        assert channel != null : "Could not find logs channel";
        channel.sendMessageEmbeds(builder.build()).queue();
    }

    @Override
    public void onMessageReactionRemove(@NotNull final MessageReactionRemoveEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getReaction().getReactionEmote().getName().equals(reactionEmote)) {
            return;
        }
        final Member member = event.retrieveMember().complete();
        if (member.equals(event.getGuild().getSelfMember())) {
            return;
        }
        RoleController.grantRole(member, outOfServiceRoleId);
        RoleController.removeRole(member, inServiceRoleId);

        final TextChannel channel = event.getGuild().getTextChannelById(adminLogsChannelId);
        final EmbedBuilder builder = new EmbedBuilder();
        final long sessionStartTime = lastSessions.getOrDefault(member.getUser().getIdLong(), 0L);
        final long sessionTime = System.currentTimeMillis() - sessionStartTime;
        if (sessionStartTime != 0 && sessionTime != 0) {
            discordBot.getInformationGrabber().findEmployeeByDiscordId(member.getId()).thenAccept(optionalEmployee -> {
                optionalEmployee.ifPresent(employee -> {
                    employee.setWorktime(employee.getWorktime() + sessionTime);
                    discordBot.getInformationGrabber().saveEmployeeData(employee);
                });
            });
        }
        final long seconds = sessionTime / 1000;
        final String formattedSessionTime =
                sessionStartTime > 0 ?
                        String.format("%d Stunde(n), %d Minute(n), %d Sekunde(n)", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
                        : "Unbekannt";

        builder.setTitle(discordBot.getMessageStore().provide("no-longer-at-work-title"));
        builder.setColor(Color.RED);
        builder.setDescription(discordBot.getMessageStore().provide("no-longer-at-work-message").replace("%mention%", member.getAsMention()).replace("%time%", formattedSessionTime));
        builder.setThumbnail(member.getUser().getEffectiveAvatarUrl());
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        assert channel != null : "Could not find logs channel";
        channel.sendMessageEmbeds(builder.build()).queue();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(final GenericMessageEvent event) {
        return event.getTextChannel().getId().equals(dienstmeldungChannelId);
    }

    public static WorktimeListener forBot(final DiscordBot discordBot) {
        return new WorktimeListener(discordBot);
    }
}
