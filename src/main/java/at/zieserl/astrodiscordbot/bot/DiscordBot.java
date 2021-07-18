package at.zieserl.astrodiscordbot.bot;

import at.zieserl.astrodiscordbot.config.BotConfig;
import at.zieserl.astrodiscordbot.database.InformationGrabber;
import at.zieserl.astrodiscordbot.database.MysqlConnection;
import at.zieserl.astrodiscordbot.feature.firstrank.FirstRankCommandListener;
import at.zieserl.astrodiscordbot.feature.clear.ClearListener;
import at.zieserl.astrodiscordbot.feature.greeter.GreetListener;
import at.zieserl.astrodiscordbot.feature.info.InfoListener;
import at.zieserl.astrodiscordbot.feature.register.RegisterListener;
import at.zieserl.astrodiscordbot.feature.setup.SetupCommandListener;
import at.zieserl.astrodiscordbot.feature.vacation.VacationListener;
import at.zieserl.astrodiscordbot.feature.worktime.WorktimeListener;
import at.zieserl.astrodiscordbot.i18n.MessageStore;
import at.zieserl.astrodiscordbot.log.LogController;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.Objects;

public final class DiscordBot {
    private final MessageStore messageStore;
    private final BotConfig botConfig;
    private MysqlConnection databaseConnection;
    private InformationGrabber informationGrabber;
    private LogController logController;
    private final String guildId;
    private Guild activeGuild;

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
        builder.addEventListeners(FirstRankCommandListener.forBot(this));
        builder.addEventListeners(WorktimeListener.forBot(this));
        builder.addEventListeners(SetupCommandListener.forBot(this));
        builder.addEventListeners(VacationListener.forBot(this));
        builder.addEventListeners(InfoListener.forBot(this));
        builder.addEventListeners(ClearListener.forBot(this));
        builder.addEventListeners(RegisterListener.forBot(this));

        final JDA jda = builder.build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        activeGuild = jda.getGuildById(guildId);

        databaseConnection = MysqlConnection.establish(
                botConfig.retrieveValue("host"),
                botConfig.retrieveValue("port"),
                botConfig.retrieveValue("database"),
                botConfig.retrieveValue("user"),
                botConfig.retrieveValue("password")
        );

        informationGrabber = InformationGrabber.forConnection(databaseConnection);
        informationGrabber.reloadConstantsCache();

        logController = LogController.forBot(this);

        registerCommands();
        changeNicknameIfNeeded();
    }

    private void registerCommands() {
        assert activeGuild != null : "Could not find guild by given guild id";
        registerCommand(activeGuild, new CommandData(getBotConfig().retrieveValue("first-rank-command-name"), getMessageStore().provide("first-rank-command-description"))
                .addOption(OptionType.STRING, "name", "Dein IC Name"));
        registerCommand(activeGuild, new CommandData("clear", "Löscht alle Nachrichten aus dem angegebenen Channel."));
        registerCommand(activeGuild, new CommandData("info", "Ruft Informationen eines bestimmten Members ab")
                .addOption(OptionType.USER, "member", "Der Member, dessen Informationen abgerufen werden sollen", true));
        registerCommand(activeGuild, new CommandData("register", "Registriert eine neue Person in die Datenbank")
                .addOption(OptionType.USER, "member", "Der Member, der registriert werden soll", true)
                .addOption(OptionType.STRING, "name", "Der IC Name der Person", true)
                .addOption(OptionType.STRING, "educations", "Die Ausbildungen mit welchen die Person registriert werden soll", true));
    }

    private void registerCommand(Guild guild, CommandData commandData) {
        unregisterCommand(guild, commandData.getName());
        guild.upsertCommand(commandData).complete();
    }

    private void unregisterCommand(Guild guild, String name) {
        guild.retrieveCommands().complete().forEach(command -> {
                    if (command.getName().equals(name)) {
                        guild.deleteCommandById(command.getId()).complete();
                    }
                }
        );
    }

    private void changeNicknameIfNeeded() {
        final Member self = activeGuild.getSelfMember();
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

    public LogController getLogController() {
        return logController;
    }

    public MysqlConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public InformationGrabber getInformationGrabber() {
        return informationGrabber;
    }

    public Guild getActiveGuild() {
        return activeGuild;
    }

    public static DiscordBot create(MessageStore messageStore, BotConfig botConfig, String guildId) {
        return new DiscordBot(messageStore, botConfig, guildId);
    }
}
