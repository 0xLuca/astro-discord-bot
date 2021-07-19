package at.zieserl.astrodiscordbot.feature.info;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.RoleController;
import at.zieserl.astrodiscordbot.employee.Education;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.employee.Rank;
import at.zieserl.astrodiscordbot.employee.SpecialUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class InfoListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final Map<String, BiConsumer<ButtonClickEvent, Employee>> actions = new HashMap<String, BiConsumer<ButtonClickEvent, Employee>>() {{
        put("promote", InfoListener.this::performPromote);
        put("demote", InfoListener.this::performDemote);
    }};
    private final Map<String, Employee> employeeCache = new HashMap<>();
    private final String adminCommandsChannelId;

    private InfoListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.adminCommandsChannelId = discordBot.getBotConfig().retrieveValue("admin-commands-channel");
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase("info")) {
            return;
        }
        Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
        if (member == null) {
            event.reply("Dieser Member konnte nicht geladen werden.").queue();
            return;
        }

        String memberId = member.getId();
        discordBot.getInformationGrabber().findEmployeeByDiscordId(memberId).thenAccept(optionalEmployee -> {
            optionalEmployee.ifPresent(employee -> {
                employeeCache.put(memberId, employee);
                final EmbedBuilder builder = new EmbedBuilder();

                builder.setTitle("Informationen zu " + member.getEffectiveName());
                builder.setColor(Color.BLUE);
                builder.addField("Dienstnummer", formatServiceNumber(employee.getServiceNumber()), false);
                builder.addField("Name", employee.getName(), false);
                builder.addField("Dienstgrad", employee.getRank().getName(), false);
                String educations = convertEducationsToString(employee.getEducationList());
                if (!educations.isEmpty()) {
                    builder.addField("Ausbildungen", educations, false);
                }
                String specialUnits = convertSpecialUnitsToString(employee.getSpecialUnitList());
                if (!specialUnits.isEmpty()) {
                    builder.addField("Spezialeinheiten", specialUnits, false);
                }

                builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getAvatarUrl());
                event.replyEmbeds(builder.build()).addActionRow(
                        Button.success("promote:" + memberId, discordBot.getMessageStore().provide("promote-text")),
                        Button.danger("demote:" + memberId, discordBot.getMessageStore().provide("demote-text")),
                        Button.success("add_education:" + memberId, discordBot.getMessageStore().provide("add-education-text")),
                        Button.danger("warn:" + memberId, discordBot.getMessageStore().provide("warn-text")),
                        Button.primary("additional_actions:" + memberId, discordBot.getMessageStore().provide("additional-actions-text"))
                ).queue();
            });
            if (!optionalEmployee.isPresent()) {
                event.reply("Dieser User wurde nicht in der Datenbank gefunden!").queue();
            }
        });
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }

        String[] splitComponentId = event.getComponentId().split(":");
        assert splitComponentId.length == 2 : "Invalid command id!";
        String action = splitComponentId[0];
        String discordId = splitComponentId[1];
        actions.get(action).accept(event, employeeCache.get(discordId));
    }

    private void performPromote(ButtonClickEvent event, Employee employee) {
        int currentRankId = employee.getRank().getId();
        Rank currentRank = discordBot.getInformationGrabber().getRankById(currentRankId);
        if (currentRank == discordBot.getInformationGrabber().getHighestRank()) {
            event.reply("Dieser Mitarbeiter hat bereits den höchsten Dienstgrad!").queue();
            return;
        }

        Rank newRank = discordBot.getInformationGrabber().getNextHigherRank(currentRank);
        if (discordBot.getInformationGrabber().countEmployeesWithRank(newRank) >= newRank.getMaxMembers()) {
            event.reply("Der neue Dienstgrad hat bereits die maximale Anzahl an Mitarbeitern erreicht!").queue();
            return;
        }

        int newServiceNumber = discordBot.getInformationGrabber().findNextFreeServiceNumber(newRank);
        String newServiceNumberFormatted = formatServiceNumber(newServiceNumber);
        Member member = Objects.requireNonNull(event.getGuild()).retrieveMemberById(employee.getDiscordId()).complete();
        RoleController.removeRole(member, String.valueOf(currentRank.getDiscordId()));
        RoleController.grantRole(member, String.valueOf(newRank.getDiscordId()));
        employee.setRank(newRank);
        employee.setServiceNumber(newServiceNumber);
        event.reply(String.format("%s wurde erfolgreich zu %s befördert. Seine neue Dienstnummer lautet %s.", member.getEffectiveName(), newRank.getName(), newServiceNumberFormatted)).queue();
        discordBot.getLogController().postRankChange(employee);
        employee.updateNickname(member);
        discordBot.getInformationGrabber().saveEmployeeData(employee);
    }

    private void performDemote(ButtonClickEvent event, Employee employee) {
        int currentRankId = employee.getRank().getId();
        Rank currentRank = discordBot.getInformationGrabber().getRankById(currentRankId);
        if (currentRank == discordBot.getInformationGrabber().getLowestRank()) {
            event.reply("Dieser Mitarbeiter hat bereits den niedrigsten Dienstgrad!").queue();
            return;
        }

        Rank newRank = discordBot.getInformationGrabber().getNextLowerRank(currentRank);
        if (discordBot.getInformationGrabber().countEmployeesWithRank(newRank) >= newRank.getMaxMembers()) {
            event.reply("Der neue Dienstgrad hat bereits die maximale Anzahl an Mitarbeitern erreicht!").queue();
            return;
        }

        int newServiceNumber = discordBot.getInformationGrabber().findNextFreeServiceNumber(newRank);
        String newServiceNumberFormatted = formatServiceNumber(newServiceNumber);
        Member member = Objects.requireNonNull(event.getGuild()).retrieveMemberById(employee.getDiscordId()).complete();
        RoleController.removeRole(member, String.valueOf(currentRank.getDiscordId()));
        RoleController.grantRole(member, String.valueOf(newRank.getDiscordId()));
        employee.setRank(newRank);
        employee.setServiceNumber(newServiceNumber);
        event.reply(String.format("%s wurde erfolgreich zu %s degradiert. Seine neue Dienstnummer lautet %s.", member.getEffectiveName(), newRank.getName(), newServiceNumberFormatted)).queue();
        discordBot.getLogController().postRankChange(employee);
        employee.updateNickname(member);
        discordBot.getInformationGrabber().saveEmployeeData(employee);
    }

    private String formatServiceNumber(int serviceNumber) {
        return String.format("%02d", serviceNumber);
    }

    private String convertEducationsToString(List<Education> educations) {
        StringBuilder educationsAsString = new StringBuilder();
        educations.forEach(education -> educationsAsString.append(education.getName()).append("\n"));
        return educationsAsString.toString();
    }

    private String convertSpecialUnitsToString(List<SpecialUnit> specialUnits) {
        StringBuilder educationsAsString = new StringBuilder();
        specialUnits.forEach(specialUnit -> educationsAsString.append(specialUnit.getName()).append("\n"));
        return educationsAsString.toString();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(adminCommandsChannelId);
    }

    public static InfoListener forBot(DiscordBot discordBot) {
        return new InfoListener(discordBot);
    }
}
