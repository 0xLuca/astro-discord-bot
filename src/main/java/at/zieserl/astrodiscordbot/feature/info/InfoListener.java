package at.zieserl.astrodiscordbot.feature.info;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.RoleController;
import at.zieserl.astrodiscordbot.employee.Education;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.employee.Rank;
import at.zieserl.astrodiscordbot.employee.SpecialUnit;
import at.zieserl.astrodiscordbot.util.TriConsumer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.dv8tion.jda.api.requests.restaction.interactions.UpdateInteractionAction;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class InfoListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final Map<String, BiConsumer<ButtonClickEvent, Employee>> buttonActions = new HashMap<String, BiConsumer<ButtonClickEvent, Employee>>() {{
        put("promote", InfoListener.this::performPromote);
        put("demote", InfoListener.this::performDemote);
        put("add-education", InfoListener.this::showAddEducationSelection);
        put("add-special-unit", InfoListener.this::showAddSpecialUnitSelection);
        put("warn", InfoListener.this::performWarn);
    }};
    private final Map<String, TriConsumer<SelectionMenuEvent, Employee, List<String>>> selectionMenuActions = new HashMap<String, TriConsumer<SelectionMenuEvent, Employee, List<String>>>() {{
        put("education-selection", InfoListener.this::performChangeEducations);
        put("special-unit-selection", InfoListener.this::performChangeSpecialUnits);
    }};
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
            event.reply("Dieser Member konnte nicht geladen werden.").setEphemeral(true).queue();
            return;
        }

        String memberId = member.getId();
        discordBot.getInformationGrabber().findEmployeeByDiscordId(memberId).thenAccept(optionalEmployee -> {
            optionalEmployee.ifPresent(employee -> {
                addControlActionRow(event.replyEmbeds(buildInformationEmbed(member, event.getJDA(), employee)), memberId).queue();
            });
            if (!optionalEmployee.isPresent()) {
                event.reply("Dieser User wurde nicht in der Datenbank gefunden!").setEphemeral(true).queue();
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
        String actionName = splitComponentId[0];
        String discordId = splitComponentId[1];
        discordBot.getInformationGrabber().findEmployeeByDiscordId(discordId).thenAccept(optionalEmployee -> {
            optionalEmployee.ifPresent(employee -> {
                BiConsumer<ButtonClickEvent, Employee> action = buttonActions.get(actionName);
                action.accept(event, employee);
                Member member = Objects.requireNonNull(event.getGuild()).retrieveMemberById(discordId).complete();
                if (!(actionName.equals("add-education") || actionName.equals("add-special-unit"))) {
                    event.deferEdit().setEmbeds(buildInformationEmbed(member, event.getJDA(), employee)).queue();
                }
            });
        });
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (event.getSelectedOptions() == null) {
            return;
        }
        String[] splitComponentId = event.getComponentId().split(":");
        assert splitComponentId.length == 2 : "Invalid selection menu id!";
        String actionName = splitComponentId[0];
        String discordId = splitComponentId[1];
        List<String> newIds = new ArrayList<>();
        event.getSelectedOptions().forEach(selectOption -> newIds.add(selectOption.getValue()));
        discordBot.getInformationGrabber().findEmployeeByDiscordId(discordId).thenAccept(optionalEmployee -> {
            optionalEmployee.ifPresent(employee -> {
                selectionMenuActions.get(actionName).accept(event, employee, newIds);
                Member member = Objects.requireNonNull(event.getGuild()).retrieveMemberById(discordId).complete();
                addControlActionRow(event.deferEdit().setEmbeds(buildInformationEmbed(member, event.getJDA(), employee)), member.getId()).queue();
            });
        });
    }

    private MessageEmbed buildInformationEmbed(Member member, JDA jda, Employee employee) {
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
        Integer warns = employee.getWarnings();
        if (employee.getWarnings() > 0) {
            builder.addField("Verwarnungen", warns.toString(), false);
        }

        builder.setFooter(discordBot.getMessageStore().provide("type"), jda.getSelfUser().getAvatarUrl());
        return builder.build();
    }

    private ReplyAction addControlActionRow(ReplyAction action, String memberId) {
        return action.addActionRow(
                Button.success("promote:" + memberId, discordBot.getMessageStore().provide("promote-text")),
                Button.danger("demote:" + memberId, discordBot.getMessageStore().provide("demote-text")),
                Button.primary("add-education:" + memberId, discordBot.getMessageStore().provide("add-education-text")),
                Button.primary("add-special-unit:" + memberId, discordBot.getMessageStore().provide("add-special-unit-text")),
                Button.danger("warn:" + memberId, discordBot.getMessageStore().provide("warn-text"))
                //Button.primary("additional_actions:" + memberId, discordBot.getMessageStore().provide("additional-actions-text"))
        );
    }

    private UpdateInteractionAction addControlActionRow(UpdateInteractionAction action, String memberId) {
        return action.setActionRow(
                Button.success("promote:" + memberId, discordBot.getMessageStore().provide("promote-text")),
                Button.danger("demote:" + memberId, discordBot.getMessageStore().provide("demote-text")),
                Button.primary("add-education:" + memberId, discordBot.getMessageStore().provide("add-education-text")),
                Button.primary("add-special-unit:" + memberId, discordBot.getMessageStore().provide("add-special-unit-text")),
                Button.danger("warn:" + memberId, discordBot.getMessageStore().provide("warn-text"))
                //Button.primary("additional_actions:" + memberId, discordBot.getMessageStore().provide("additional-actions-text"))
        );
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
        Member member = Objects.requireNonNull(event.getGuild()).retrieveMemberById(employee.getDiscordId()).complete();
        RoleController.removeRole(member, String.valueOf(currentRank.getDiscordId()));
        RoleController.grantRole(member, String.valueOf(newRank.getDiscordId()));
        employee.setRank(newRank);
        employee.setServiceNumber(newServiceNumber);
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
        Member member = Objects.requireNonNull(event.getGuild()).retrieveMemberById(employee.getDiscordId()).complete();
        RoleController.removeRole(member, String.valueOf(currentRank.getDiscordId()));
        RoleController.grantRole(member, String.valueOf(newRank.getDiscordId()));
        employee.setRank(newRank);
        employee.setServiceNumber(newServiceNumber);
        //event.reply(String.format("%s wurde erfolgreich zu %s degradiert. Seine neue Dienstnummer lautet %s.", member.getEffectiveName(), newRank.getName(), newServiceNumberFormatted)).queue();
        discordBot.getLogController().postRankChange(employee);
        employee.updateNickname(member);
        discordBot.getInformationGrabber().saveEmployeeData(employee);
    }

    private void performWarn(ButtonClickEvent event, Employee employee) {
        employee.setWarnings(employee.getWarnings() + 1);
        RoleController.grantRole(Objects.requireNonNull(
                discordBot.getActiveGuild().retrieveMemberById(employee.getDiscordId()).complete()),
                discordBot.getBotConfig().retrieveValue("warn-role")
        );
        discordBot.getInformationGrabber().saveEmployeeData(employee);
    }

    private void showAddEducationSelection(ButtonClickEvent event, Employee employee) {
        SelectionMenu.Builder menu = SelectionMenu.create(String.format("education-selection:%s", employee.getDiscordId()));

        menu.addOptions(discordBot.getInformationGrabber().getEducations().stream().map(education -> SelectOption.of(
                education.getName(),
                education.getId().toString()
        )).collect(Collectors.toList()));

        menu.setDefaultOptions(employee.getEducationList().stream().map(education -> SelectOption.of(
                education.getName(),
                education.getId().toString()
        )).collect(Collectors.toList()));

        menu.setMinValues(0);
        menu.setMaxValues(menu.getOptions().size());

        menu.setPlaceholder("Wähle eine Ausbildung aus");
        event.deferEdit().setActionRow(menu.build()).queue();
    }

    private void showAddSpecialUnitSelection(ButtonClickEvent event, Employee employee) {
        SelectionMenu.Builder menu = SelectionMenu.create(String.format("special-unit-selection:%s", employee.getDiscordId()));
        menu.addOptions(discordBot.getInformationGrabber().getSpecialUnits().stream().map(specialUnit -> SelectOption.of(
                specialUnit.getName(),
                specialUnit.getId().toString()
        )).collect(Collectors.toList()));

        menu.setDefaultOptions(employee.getSpecialUnitList().stream().map(specialUnit -> SelectOption.of(
                specialUnit.getName(),
                specialUnit.getId().toString()
        )).collect(Collectors.toList()));

        menu.setMinValues(0);
        menu.setMaxValues(menu.getOptions().size());
        menu.setPlaceholder("Wähle eine Spezialeinheit aus");
        event.deferEdit().setActionRow(menu.build()).queue();
    }

    private void performChangeEducations(SelectionMenuEvent event, Employee employee, List<String> educationIds) {
        employee.getEducationList().clear();
        educationIds.stream().map(educationId -> discordBot.getInformationGrabber().getEducationById(Integer.parseInt(educationId)))
                .forEach(employee.getEducationList()::add);
        discordBot.getInformationGrabber().saveEmployeeEducations(employee);
    }

    private void performChangeSpecialUnits(SelectionMenuEvent event, Employee employee, List<String> specialUnitIds) {
        employee.getSpecialUnitList().clear();
        specialUnitIds.stream().map(specialUnitId -> discordBot.getInformationGrabber().getSpecialUnitById(Integer.parseInt(specialUnitId)))
                .forEach(employee.getSpecialUnitList()::add);
        discordBot.getInformationGrabber().saveEmployeeSpecialUnits(employee);
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
