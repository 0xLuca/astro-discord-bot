package at.zieserl.astrodiscordbot.feature.greeter;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public final class GreetListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String greetChannelId;

    private GreetListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.greetChannelId = discordBot.getBotConfig().retrieveValue("greet-channel");
    }

    @Override
    public void onGuildMemberJoin(@NotNull final GuildMemberJoinEvent event) {
        if (!discordBot.shouldHandleEvent(event)) {
            return;
        }
        final TextChannel channel = event.getGuild().getTextChannelById(greetChannelId);
        final EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(discordBot.getMessageStore().provide("welcome-title"));
        builder.setColor(Color.GREEN);

        builder.setDescription(discordBot.getMessageStore().provide("welcome-message").replace("%mention%", event.getMember().getAsMention()));
        builder.setThumbnail(event.getMember().getUser().getEffectiveAvatarUrl());
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        assert channel != null : "Could not find Greet channel";
        channel.sendMessageEmbeds(builder.build()).queue();
    }

    @Override
    public void onGuildMemberRemove(@NotNull final GuildMemberRemoveEvent event) {
        if (!discordBot.shouldHandleEvent(event)) {
            return;
        }
        final TextChannel channel = event.getGuild().getTextChannelById(greetChannelId);

        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("leave-title"));
        builder.setColor(Color.RED);
        builder.setDescription(discordBot.getMessageStore().provide("leave-message").replace("%mention%", event.getUser().getAsMention()));
        builder.setThumbnail(event.getUser().getEffectiveAvatarUrl());
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        assert channel != null : "Could not find Greet channel";
        channel.sendMessageEmbeds(builder.build()).queue();
    }

    public static GreetListener forBot(final DiscordBot bot) {
        return new GreetListener(bot);
    }
}