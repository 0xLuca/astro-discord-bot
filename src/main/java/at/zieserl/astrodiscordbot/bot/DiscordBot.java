package at.zieserl.astrodiscordbot.bot;

import at.zieserl.astrodiscordbot.config.BotConfig;
import at.zieserl.astrodiscordbot.feature.azubi.AzubiCommandListener;
import at.zieserl.astrodiscordbot.feature.greeter.GreetListener;
import at.zieserl.astrodiscordbot.feature.setup.SetupCommandListener;
import at.zieserl.astrodiscordbot.feature.vacation.VacationListener;
import at.zieserl.astrodiscordbot.feature.worktime.WorktimeListener;
import at.zieserl.astrodiscordbot.i18n.MessageStore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.Objects;

public final class DiscordBot {
    private final MessageStore messageStore;
    private final BotConfig botConfig;
    private final String guildId;

    public DiscordBot(MessageStore messageStore, BotConfig botConfig, String guildId) {
        this.messageStore = messageStore;
        this.botConfig = botConfig;
        this.guildId = guildId;
    }

    public void start(String token) throws LoginException {
        final JDABuilder builder = JDABuilder.createDefault(token);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS);

        builder.addEventListeners(GreetListener.forBot(this));
        builder.addEventListeners(AzubiCommandListener.forBot(this));
        builder.addEventListeners(WorktimeListener.forBot(this));
        builder.addEventListeners(SetupCommandListener.forBot(this));
        builder.addEventListeners(VacationListener.forBot(this));

        final JDA jda = builder.build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        registerCommands(jda);
        changeNicknameIfNeeded(jda);
    }

    private void registerCommands(JDA jda) {
        Guild guild = jda.getGuildById(guildId);
        assert guild != null : "Could not find guild by given guild id";
        registerCommand(guild, "azubi", getMessageStore().provide("azubi-command-description"));
    }

    private void registerCommand(Guild guild, String name, String description) {
        unregisterCommand(guild, name);
        guild.upsertCommand(name, description).queue();
    }

    private void unregisterCommand(Guild guild, String name) {
        guild.retrieveCommands().queue(commands -> commands.forEach(command -> {
            if (command.getName().equals(name)) {
                guild.deleteCommandById(command.getId()).queue();
            }
        }));
    }

    private void changeNicknameIfNeeded(JDA jda) {
        final Member self = Objects.requireNonNull(jda.getGuildById(guildId)).getSelfMember();
        final String requiredNickname = messageStore.provide("type");
        if (!requiredNickname.equals(self.getNickname())) {
            self.modifyNickname(requiredNickname).queue();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean shouldHandleEvent(GenericGuildEvent event) {
        return event.getGuild().getId().equals(guildId);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean shouldHandleEvent(GenericMessageEvent event) {
        return event.getGuild().getId().equals(guildId);
    }

    public boolean shouldHandleEvent(GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getGuild()).getId().equals(guildId);
    }

    public MessageStore getMessageStore() {
        return messageStore;
    }

    public BotConfig getBotConfig() {
        return botConfig;
    }

    public static DiscordBot create(MessageStore messageStore, BotConfig botConfig, String guildId) {
        return new DiscordBot(messageStore, botConfig, guildId);
    }
}
