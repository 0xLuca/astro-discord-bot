package at.zieserl.astrodiscordbot.feature.register;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.employee.Education;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.employee.Rank;
import at.zieserl.astrodiscordbot.employee.SpecialUnit;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class RegisterListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String adminCommandsChannelId;

    private RegisterListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.adminCommandsChannelId = discordBot.getBotConfig().retrieveValue("admin-commands-channel");
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase("register")) {
            return;
        }
        Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
        assert member != null;
        if (discordBot.getInformationGrabber().isRegistered(member)) {
            event.reply(member.getAsMention() + " ist bereits registriert.").queue();
            return;
        }

        event.reply(member.getAsMention() + " wird registriert...").queue(interactionHook -> {
            String name = Objects.requireNonNull(event.getOption("name")).getAsString();
            String educationsAsString = Objects.requireNonNull(event.getOption("educations")).getAsString();
            int[] educationIds = Arrays.stream(educationsAsString.split(",")).mapToInt(Integer::parseInt).toArray();
            Rank rank = findRankByRoles(member);
            int serviceNumber = discordBot.getInformationGrabber().findNextFreeServiceNumber(rank);
            List<Education> educations = new ArrayList<>();
            Arrays.stream(educationIds).forEach(educationId -> educations.add(discordBot.getInformationGrabber().getEducationById(educationId)));
            Employee employee = new Employee(0, serviceNumber, member.getId(), name, rank, 0, 0, educations.toArray(new Education[0]), new SpecialUnit[0]);
            discordBot.getInformationGrabber().registerEmployeeData(employee);
            discordBot.getInformationGrabber().saveEmployeeEducations(employee);
            employee.updateNickname(member);
            interactionHook.editOriginal(member.getAsMention() + " wurde erfolgreich mit Dienstnummer " + formatServiceNumber(employee.getServiceNumber()) + " registriert!").queue();
        });
    }

    private Rank findRankByRoles(Member member) {
        for (Role role : member.getRoles()) {
            Optional<Rank> optionalRank = discordBot.getInformationGrabber().getRankForRole(role);
            if (optionalRank.isPresent()) {
                return optionalRank.get();
            }
        }
        throw new IllegalArgumentException("Could not find rank in roles for " + member.getEffectiveName());
    }

    private String formatServiceNumber(int serviceNumber) {
        return String.format("%02d", serviceNumber);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(adminCommandsChannelId);
    }

    public static RegisterListener forBot(DiscordBot discordBot) {
        return new RegisterListener(discordBot);
    }
}
