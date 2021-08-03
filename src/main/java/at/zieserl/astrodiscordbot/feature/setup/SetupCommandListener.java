package at.zieserl.astrodiscordbot.feature.setup;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public final class SetupCommandListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String dienstmeldungenChannelId;
    private final String patrolChannelId;
    private final String adminCommandsChannelId;

    private SetupCommandListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.dienstmeldungenChannelId = discordBot.getBotConfig().retrieveValue("dienstmeldung-channel");
        this.patrolChannelId = discordBot.getBotConfig().retrieveValue("patrol-channel");
        this.adminCommandsChannelId = discordBot.getBotConfig().retrieveValue("admin-commands-channel");
    }

    @Override
    public void onGuildMessageReceived(@NotNull final GuildMessageReceivedEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getMessage().getContentRaw().equalsIgnoreCase("!setup")) {
            return;
        }
        final TextChannel dienstmeldungenChannel = event.getGuild().getTextChannelById(dienstmeldungenChannelId);
        final TextChannel patrolChannel = event.getGuild().getTextChannelById(patrolChannelId);

        assert dienstmeldungenChannel != null : "Could not find Dienstmeldungen channel";
        assert patrolChannel != null : "Could not find Patrol channel";

        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("work-info-title"));
        builder.setColor(Color.BLUE);

        builder.addField(discordBot.getMessageStore().provide("start-work-title"), discordBot.getMessageStore().provide("start-work"), false);
        builder.addField(discordBot.getMessageStore().provide("end-work-title"), discordBot.getMessageStore().provide("end-work"), false);

        final String avatarUrl = event.getJDA().getSelfUser().getEffectiveAvatarUrl();
        builder.setThumbnail(avatarUrl);
        builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        dienstmeldungenChannel.sendMessageEmbeds(builder.build()).queue(message -> message.addReaction(discordBot.getBotConfig().retrieveValue("dienstmeldung-reaction-emoji")).queue());
        discordBot.getPatrolController().getPatrolMap().forEach((id, patrol) -> {
            patrolChannel.sendMessageEmbeds(discordBot.getPatrolController().buildPatrolEmbed(patrol)).queue(message -> {
                message.editMessageComponents(discordBot.getPatrolController().buildActionRow(patrol)).queue();
            });
        });
    }

    private boolean shouldHandleEvent(final GenericGuildMessageEvent event) {
        return event.getChannel().getId().equals(adminCommandsChannelId);
    }

    public static SetupCommandListener forBot(final DiscordBot discordBot) {
        return new SetupCommandListener(discordBot);
    }
}
