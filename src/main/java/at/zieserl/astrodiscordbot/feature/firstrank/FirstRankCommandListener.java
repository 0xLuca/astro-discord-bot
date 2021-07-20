package at.zieserl.astrodiscordbot.feature.firstrank;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.RoleController;
import at.zieserl.astrodiscordbot.employee.Education;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.employee.Rank;
import at.zieserl.astrodiscordbot.employee.SpecialUnit;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class FirstRankCommandListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String firstRankCommandName;
    private final List<String> firstRankCommandRoleIds;
    private final String firstRankCommandChannelId;
    private final List<Education> firstRankEducations;
    private final Rank startingRank;

    private FirstRankCommandListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.firstRankCommandName = discordBot.getBotConfig().retrieveValue("first-rank-command-name");
        this.firstRankCommandRoleIds = Arrays.asList(retrieveFirstRankCommandRoles(discordBot));
        this.firstRankCommandChannelId = discordBot.getBotConfig().retrieveValue("first-rank-command-channel");
        this.firstRankEducations = Arrays.asList(retrieveFirstRankEducationIds(discordBot));
        this.startingRank = discordBot.getInformationGrabber().getRankById(Integer.parseInt(discordBot.getBotConfig().retrieveValue("first-rank-command-rank")));
    }

    @Override
    public void onSlashCommand(@NotNull final SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase(firstRankCommandName)) {
            return;
        }
        final Member member = event.getMember();
        assert member != null : "Unknown member used first rank command";
        if (discordBot.getInformationGrabber().isRegistered(member)) {
            event.reply(discordBot.getMessageStore().provide("first-rank-command-already-registered")).queue();
            return;
        }
        final String name = Objects.requireNonNull(event.getOption("name")).getAsString();
        final String phoneNumber = Objects.requireNonNull(event.getOption("phone_number")).getAsString();
        final String birthDate = Objects.requireNonNull(event.getOption("birth_date")).getAsString();
        grantFirstRankRoles(member);
        event.reply(discordBot.getMessageStore().provide("first-rank-command-success")).queue();
        saveEmployee(member, name, phoneNumber, birthDate);
    }

    @Override
    public void onGuildMessageReceived(@NotNull final GuildMessageReceivedEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        final String commandFormat = "!" + firstRankCommandName;
        if (!event.getMessage().getContentRaw().toLowerCase().startsWith(commandFormat)) {
            return;
        }
        event.getMessage().delete().queue();
        final Member member = event.getMember();
        assert member != null : "Unknown member used first rank command";
        if (discordBot.getInformationGrabber().isRegistered(member)) {
            event.getChannel().sendMessage(discordBot.getMessageStore().provide("first-rank-command-already-registered")).queue();
            return;
        }
        final String name = event.getMessage().getContentRaw().substring(commandFormat.length());
        if (name.trim().isEmpty()) {
            event.getChannel().sendMessage("Du musst deinen IC Namen angeben!").queue();
            return;
        }
        grantFirstRankRoles(member);
        event.getChannel().sendMessage(discordBot.getMessageStore().provide("first-rank-command-success")).queue();
        saveEmployee(member, name, "", "");
    }

    private void saveEmployee(final Member member, final String name, final String phoneNumber, final String birthDate) {
        final int newServiceNumber = discordBot.getInformationGrabber().findNextFreeServiceNumber(startingRank);
        final Employee employee = new Employee(0, newServiceNumber, member.getId(), name, startingRank, 0, 0L, phoneNumber, birthDate, firstRankEducations.toArray(new Education[0]), new SpecialUnit[0]);
        employee.updateNickname(member);
        discordBot.getLogController().postNewEmployee(employee);
        discordBot.getInformationGrabber().registerEmployeeData(employee);
        discordBot.getInformationGrabber().saveEmployeeEducations(employee);
    }

    private String[] retrieveFirstRankCommandRoles(final DiscordBot discordBot) {
        return Arrays.stream(discordBot.getBotConfig().retrieveValue("first-rank-command-roles").split(",")).map(String::trim).toArray(String[]::new);
    }

    private Education[] retrieveFirstRankEducationIds(final DiscordBot discordBot) {
        return Arrays.stream(discordBot.getBotConfig().retrieveValue("first-rank-command-educations").split(","))
                .filter(s -> !s.trim().isEmpty()).map(s -> discordBot.getInformationGrabber().getEducationById(Integer.parseInt(s))).toArray(Education[]::new);
    }

    private void grantFirstRankRoles(final Member member) {
        firstRankCommandRoleIds.forEach(roleId -> RoleController.grantRole(member, roleId));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(final GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(firstRankCommandChannelId);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(final GenericGuildMessageEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(firstRankCommandChannelId);
    }

    public static FirstRankCommandListener forBot(final DiscordBot bot) {
        return new FirstRankCommandListener(bot);
    }
}
