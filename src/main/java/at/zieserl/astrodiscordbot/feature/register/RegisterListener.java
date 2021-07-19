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

    private RegisterListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.adminCommandsChannelId = discordBot.getBotConfig().retrieveValue("admin-commands-channel");
    }

    @Override
    public void onSlashCommand(@NotNull final SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase("register")) {
            return;
        }
        final Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
        assert member != null;
        if (discordBot.getInformationGrabber().isRegistered(member)) {
            event.reply(member.getAsMention() + " ist bereits registriert.").queue();
            return;
        }

        event.reply(member.getAsMention() + " wird registriert...").queue(interactionHook -> {
            final String name = Objects.requireNonNull(event.getOption("name")).getAsString();
            final String educationsAsString = Objects.requireNonNull(event.getOption("educations")).getAsString();
            final int[] educationIds = Arrays.stream(educationsAsString.split(",")).mapToInt(Integer::parseInt).toArray();
            final Rank rank = findRankByRoles(member);
            final int serviceNumber = discordBot.getInformationGrabber().findNextFreeServiceNumber(rank);
            final List<Education> educations = new ArrayList<>();
            Arrays.stream(educationIds).forEach(educationId -> educations.add(discordBot.getInformationGrabber().getEducationById(educationId)));
            final Employee employee = new Employee(0, serviceNumber, member.getId(), name, rank, 0, 0L, educations.toArray(new Education[0]), new SpecialUnit[0]);
            discordBot.getInformationGrabber().registerEmployeeData(employee);
            discordBot.getInformationGrabber().saveEmployeeEducations(employee);
            employee.updateNickname(member);
            interactionHook.editOriginal(member.getAsMention() + " wurde erfolgreich mit Dienstnummer " + formatServiceNumber(employee.getServiceNumber()) + " registriert!").queue();
        });
    }

    private Rank findRankByRoles(final Member member) {
        for (final Role role : member.getRoles()) {
            final Optional<Rank> optionalRank = discordBot.getInformationGrabber().getRankForRole(role);
            if (optionalRank.isPresent()) {
                return optionalRank.get();
            }
        }
        throw new IllegalArgumentException("Could not find rank in roles for " + member.getEffectiveName());
    }

    private String formatServiceNumber(final int serviceNumber) {
        return String.format("%02d", serviceNumber);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(final GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(adminCommandsChannelId);
    }

    public static RegisterListener forBot(final DiscordBot discordBot) {
        return new RegisterListener(discordBot);
    }
}
