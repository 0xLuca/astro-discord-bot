package at.zieserl.astrodiscordbot.feature.worktime;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.Channels;
import at.zieserl.astrodiscordbot.constant.Roles;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public final class WorktimeListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String reactionEmote;

    private WorktimeListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.reactionEmote = discordBot.getBotConfig().retrieveValue("dienstmeldung-reaction-emoji");
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
        Roles.removeRole(member, Roles.AUSSER_DIENST_ID);

        final TextChannel channel = event.getGuild().getTextChannelById(Channels.LOGS_CHANNEL_ID);
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
        Roles.grantRole(member, Roles.AUSSER_DIENST_ID);

        final TextChannel channel = event.getGuild().getTextChannelById(Channels.LOGS_CHANNEL_ID);

        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("no-longer-at-work-title"));
        builder.setColor(Color.RED);

        builder.setDescription(discordBot.getMessageStore().provide("no-longer-at-work-message").replace("%mention%", member.getAsMention()));
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
        return event.getTextChannel().getId().equals(Channels.DIENSTMELDUNGEN_CHANNEL_ID);
    }

    public static WorktimeListener forBot(DiscordBot discordBot) {
        return new WorktimeListener(discordBot);
    }
}
