package at.zieserl.astrodiscordbot.feature.vacation;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.Channels;
import at.zieserl.astrodiscordbot.constant.Strings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Date;
import java.time.Instant;

public final class VacationListener extends ListenerAdapter {
    private final DiscordBot discordBot;

    private VacationListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }

        final TextChannel channel = event.getGuild().getTextChannelById(Channels.ABMELDEN_CHANNEL_ID);
        final EmbedBuilder builder = new EmbedBuilder();

        String name;
        if (event.getMember() != null) {
            name = event.getMember().getEffectiveName();
        } else {
            name = event.getAuthor().getName();
        }

        builder.setTitle(discordBot.getMessageStore().provide("vacation-title").replace("%name%", name));
        builder.setColor(Color.RED);
        builder.setDescription(event.getMessage().getContentRaw());
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getAvatarUrl());

        assert channel != null : "Could not find Abmelden channel";
        channel.sendMessage(builder.build()).queue(message -> {
            message.addReaction(Strings.VACATION_ACCEPT_EMOJI).queue();
            message.addReaction(Strings.VACATION_DECLINE_EMOJI).queue();
            event.getMessage().delete().queue();
        });
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }

        final TextChannel channel = event.getGuild().getTextChannelById(Channels.ABMELDEN_CHANNEL_ID);
        assert channel != null : "Could not find Abmelden channel";
        channel.retrieveMessageById(event.getMessageId()).queue(message -> message.clearReactions().queue(unused -> {
            if (event.getReactionEmote().isEmoji()) {
                String emoji = event.getReactionEmote().getEmoji();
                message.addReaction(emoji).queue();
            }
        }));

    }

    @Override
    public void onGuildMessageReactionRemove(@NotNull GuildMessageReactionRemoveEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !event.getChannel().getId().equals(Channels.ABMELDEN_CHANNEL_ID)) {
            return;
        }
        Member member = event.getMember();
        assert member != null : "Unknown member removed a reaction";
        System.out.println("[" + Date.from(Instant.now()) +  "] " + member.getEffectiveName() + " removed reaction " + event.getReactionEmote() + " from message id " + event.getMessageId());
    }

    private boolean shouldHandleEvent(GuildMessageReceivedEvent event) {
        return event.getChannel().getId().equals(Channels.ABMELDEN_CHANNEL_ID) && !event.getGuild().getSelfMember().equals(event.getMember());
    }

    private boolean shouldHandleEvent(GenericGuildMessageReactionEvent event) {
        return event.getChannel().getId().equals(Channels.ABMELDEN_CHANNEL_ID) && !event.getGuild().getSelfMember().equals(event.getMember());
    }

    public static VacationListener forBot(DiscordBot discordBot) {
        return new VacationListener(discordBot);
    }
}
