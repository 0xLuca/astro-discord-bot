package at.zieserl.astrodiscordbot.feature.setup;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public final class SetupCommandListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String dienstmeldungenChannelId;

    private SetupCommandListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.dienstmeldungenChannelId = discordBot.getBotConfig().retrieveValue("dienstmeldung-channel");
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getMessage().getContentRaw().equalsIgnoreCase("!setup")) {
            return;
        }
        final TextChannel channel = event.getGuild().getTextChannelById(dienstmeldungenChannelId);

        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("work-info-title"));
        builder.setColor(Color.BLUE);

        builder.addField(discordBot.getMessageStore().provide("start-work-title"), discordBot.getMessageStore().provide("start-work"), false);
        builder.addField(discordBot.getMessageStore().provide("end-work-title"), discordBot.getMessageStore().provide("end-work"), false);

        String avatarUrl = event.getJDA().getSelfUser().getAvatarUrl();
        if (avatarUrl == null) {
            avatarUrl = event.getJDA().getSelfUser().getDefaultAvatarUrl();
        }
        builder.setThumbnail(avatarUrl);
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getAvatarUrl());

        assert channel != null : "Could not find Dienstmeldungen channel";
        channel.sendMessageEmbeds(builder.build()).queue(message -> message.addReaction(discordBot.getBotConfig().retrieveValue("dienstmeldung-reaction-emoji")).queue());

    }

    private boolean shouldHandleEvent(GenericGuildMessageEvent event) {
        return event.getChannel().getId().equals(dienstmeldungenChannelId);
    }

    public static SetupCommandListener forBot(DiscordBot discordBot) {
        return new SetupCommandListener(discordBot);
    }
}
