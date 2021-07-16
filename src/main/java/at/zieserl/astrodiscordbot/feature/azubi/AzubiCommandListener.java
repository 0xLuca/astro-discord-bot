package at.zieserl.astrodiscordbot.feature.azubi;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.Channels;
import at.zieserl.astrodiscordbot.constant.Roles;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class AzubiCommandListener extends ListenerAdapter {
    private final DiscordBot discordBot;

    private AzubiCommandListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase("azubi")) {
            return;
        }
        final Member member = event.getMember();
        assert member != null : "Unknown member used azubi command";
        grantAzubiRoles(member);
        event.reply(discordBot.getMessageStore().provide("azubi-command-success")).queue();
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getMessage().getContentRaw().trim().equalsIgnoreCase("!azubi")) {
            return;
        }
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        grantAzubiRoles(member);
        event.getChannel().sendMessage(discordBot.getMessageStore().provide("azubi-command-success")).queue();
    }

    private void grantAzubiRoles(Member member) {
        Roles.grantRole(member, Roles.AUSSER_DIENST_ID);
        Roles.grantRole(member, Roles.AZUBI_ID);
        Roles.grantRole(member, Roles.ABTEILUNGEN_ID);
        Roles.grantRole(member, Roles.RETTUNGSMEDIZIN_ID);
        Roles.grantRole(member, Roles.SONSTIGES_ID);
        Roles.grantRole(member, Roles.LSMD_ID);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(Channels.AZUBI_COMMAND_CHANNEL_ID);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(GenericGuildMessageEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(Channels.AZUBI_COMMAND_CHANNEL_ID);
    }

    public static AzubiCommandListener forBot(DiscordBot bot) {
        return new AzubiCommandListener(bot);
    }
}
