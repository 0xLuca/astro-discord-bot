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

    private FirstRankCommandListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.firstRankCommandName = discordBot.getBotConfig().retrieveValue("first-rank-command-name");
        this.firstRankCommandRoleIds = Arrays.asList(retrieveFirstRankCommandRoles(discordBot));
        this.firstRankCommandChannelId = discordBot.getBotConfig().retrieveValue("first-rank-command-channel");
        this.firstRankEducations = Arrays.asList(retrieveFirstRankEducationIds(discordBot));
        this.startingRank = discordBot.getInformationGrabber().getRankById(Integer.parseInt(discordBot.getBotConfig().retrieveValue("first-rank-command-rank")));
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
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
        String name = Objects.requireNonNull(event.getOption("name")).getAsString();
        grantFirstRankRoles(member);
        event.reply(discordBot.getMessageStore().provide("first-rank-command-success")).queue();
        saveEmployee(member, name);
    }

    private void saveEmployee(Member member, String name) {
        int newServiceNumber = discordBot.getInformationGrabber().findNextFreeServiceNumber(startingRank);
        Employee employee = new Employee(0, newServiceNumber, member.getId(), name, startingRank, 0, 0, firstRankEducations.toArray(new Education[0]), new SpecialUnit[0]);
        employee.updateNickname(member);
        discordBot.getLogController().postNewEmployee(employee);
        discordBot.getInformationGrabber().registerEmployeeData(employee);
        discordBot.getInformationGrabber().saveEmployeeEducations(employee);
    }

    private String[] retrieveFirstRankCommandRoles(DiscordBot discordBot) {
        return Arrays.stream(discordBot.getBotConfig().retrieveValue("first-rank-command-roles").split(",")).map(String::trim).toArray(String[]::new);
    }

    private Education[] retrieveFirstRankEducationIds(DiscordBot discordBot) {
        return Arrays.stream(discordBot.getBotConfig().retrieveValue("first-rank-command-educations").split(","))
                .map(s -> discordBot.getInformationGrabber().getEducationById(Integer.parseInt(s))).toArray(Education[]::new);
    }

    private void grantFirstRankRoles(Member member) {
        firstRankCommandRoleIds.forEach(roleId -> RoleController.grantRole(member, roleId));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(firstRankCommandChannelId);
    }

    public static FirstRankCommandListener forBot(DiscordBot bot) {
        return new FirstRankCommandListener(bot);
    }
}
