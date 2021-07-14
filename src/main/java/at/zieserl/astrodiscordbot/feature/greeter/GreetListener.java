package at.zieserl.astrodiscordbot.feature.greeter;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.Channels;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public final class GreetListener extends ListenerAdapter {
    private final DiscordBot discordBot;

    private GreetListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if (!discordBot.shouldHandleEvent(event)) {
            return;
        }
        final TextChannel channel = event.getGuild().getTextChannelById(Channels.GREET_CHANNEL_ID);

        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("welcome-title"));
        builder.setColor(Color.GREEN);

        builder.setDescription(discordBot.getMessageStore().provide("welcome-message").replace("%mention%", event.getMember().getAsMention()));
        String avatarUrl = event.getMember().getUser().getAvatarUrl();
        if (avatarUrl == null) {
            avatarUrl = event.getMember().getUser().getDefaultAvatarUrl();
        }
        builder.setThumbnail(avatarUrl);
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getAvatarUrl());

        assert channel != null : "Could not find Greet channel";
        channel.sendMessage(builder.build()).queue();
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        if (!discordBot.shouldHandleEvent(event)) {
            return;
        }
        final TextChannel channel = event.getGuild().getTextChannelById(Channels.GREET_CHANNEL_ID);

        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("leave-title"));
        builder.setColor(Color.RED);
        builder.setDescription(discordBot.getMessageStore().provide("leave-message").replace("%mention%", event.getUser().getAsMention()));
        String avatarUrl = event.getUser().getAvatarUrl();
        if (avatarUrl == null) {
            avatarUrl = event.getUser().getDefaultAvatarUrl();
        }
        builder.setThumbnail(avatarUrl);
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getAvatarUrl());

        assert channel != null : "Could not find Greet channel";
        channel.sendMessage(builder.build()).queue();
    }

    public static GreetListener forBot(DiscordBot bot) {
        return new GreetListener(bot);
    }
}