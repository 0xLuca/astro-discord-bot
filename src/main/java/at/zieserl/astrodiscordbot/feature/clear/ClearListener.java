package at.zieserl.astrodiscordbot.feature.clear;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ClearListener extends ListenerAdapter {
    private final DiscordBot discordBot;

    private ClearListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onSlashCommand(@NotNull final SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase("clear")) {
            return;
        }
        if (!event.getMember().getRoles().contains(event.getGuild().getRoleById(discordBot.getBotConfig().retrieveValue("admin-role")))) {
            event.reply("Du hast keine Berechtigung diesen Befehl zu nutzen.").queue();
            return;
        }
        final TextChannel channel = event.getTextChannel();
        List<Message> messages = channel.getHistory().retrievePast(50).complete();

        event.reply("Alle Nachrichten aus diesem Kanal werden gelöscht.").setEphemeral(true).queue();

        while (!messages.isEmpty()) {
            channel.deleteMessages(messages).complete();
            messages = channel.getHistory().retrievePast(50).complete();
        }
    }

    public static ClearListener forBot(final DiscordBot discordBot) {
        return new ClearListener(discordBot);
    }
}
