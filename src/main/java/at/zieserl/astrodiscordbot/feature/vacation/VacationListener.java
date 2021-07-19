package at.zieserl.astrodiscordbot.feature.vacation;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.Strings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class VacationListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String vacationChannelId;
    private final List<Role> allowedVacationReactRoles;

    private VacationListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.vacationChannelId = discordBot.getBotConfig().retrieveValue("vacation-channel");
        this.allowedVacationReactRoles = Arrays.stream(discordBot.getBotConfig().retrieveValue("vacation-react-roles").split(",")).map(roleId -> discordBot.getActiveGuild().getRoleById(roleId)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void onGuildMessageReceived(@NotNull final GuildMessageReceivedEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }

        final TextChannel channel = event.getGuild().getTextChannelById(vacationChannelId);
        final EmbedBuilder builder = new EmbedBuilder();

        final String name;
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
        channel.sendMessageEmbeds(builder.build()).queue(message -> {
            message.addReaction(Strings.VACATION_ACCEPT_EMOJI).queue();
            message.addReaction(Strings.VACATION_DECLINE_EMOJI).queue();
            event.getMessage().delete().queue();
        });
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull final GuildMessageReactionAddEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }

        final TextChannel channel = event.getChannel();
        final Member member = event.getGuild().retrieveMemberById(event.getUserId()).complete();
        if (!isMemberAllowedToReact(member)) {
            channel.retrieveMessageById(event.getMessageId()).queue(message -> {
                if (event.getReactionEmote().isEmoji()) {
                    final String emoji = event.getReactionEmote().getEmoji();
                    message.removeReaction(emoji, member.getUser()).queue();
                }
            });
            return;
        }
        channel.retrieveMessageById(event.getMessageId()).queue(message -> message.clearReactions().queue(unused -> {
            if (event.getReactionEmote().isEmoji()) {
                final String emoji = event.getReactionEmote().getEmoji();
                message.addReaction(emoji).queue();
            }
        }));
    }

    @Override
    public void onGuildMessageReactionRemove(@NotNull final GuildMessageReactionRemoveEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !event.getChannel().getId().equals(vacationChannelId)) {
            return;
        }
        final Member member = event.getMember();
        assert member != null : "Unknown member removed a reaction";
        System.out.println("[" + Date.from(Instant.now()) +  "] " + member.getEffectiveName() + " removed reaction " + event.getReactionEmote() + " from message id " + event.getMessageId());
    }

    private boolean isMemberAllowedToReact(final Member member) {
        for (final Role role : member.getRoles()) {
            if (allowedVacationReactRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldHandleEvent(final GuildMessageReceivedEvent event) {
        return event.getChannel().getId().equals(vacationChannelId) && !event.getGuild().getSelfMember().equals(event.getMember());
    }

    private boolean shouldHandleEvent(final GenericGuildMessageReactionEvent event) {
        return event.getChannel().getId().equals(vacationChannelId) && !event.getGuild().getSelfMember().equals(event.getMember());
    }

    public static VacationListener forBot(final DiscordBot discordBot) {
        return new VacationListener(discordBot);
    }
}
