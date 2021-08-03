package at.zieserl.astrodiscordbot.feature.patrol;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.log.LogController;
import at.zieserl.astrodiscordbot.patrol.Patrol;
import at.zieserl.astrodiscordbot.patrol.PatrolStatus;
import at.zieserl.astrodiscordbot.patrol.PatrolUnit;
import at.zieserl.astrodiscordbot.patrol.PatrolVehicle;
import at.zieserl.astrodiscordbot.util.TriConsumer;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class PatrolListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final Map<String, TriConsumer<ButtonClickEvent, Patrol, Employee>> buttonActions = new HashMap<String, TriConsumer<ButtonClickEvent, Patrol, Employee>>() {{
        put("join-patrol", PatrolListener.this::performJoinPatrol);
        put("leave-patrol", PatrolListener.this::performLeavePatrol);
        put("show-patrol-vehicle-selection", PatrolListener.this::showSetPatrolVehicleSelection);
        put("show-set-patrol-unit-selection", PatrolListener.this::showSetPatrolUnitSelection);
        put("show-set-patrol-status-selection", PatrolListener.this::showSetPatrolStatusSelection);
    }};
    private final Map<String, BiConsumer<SelectionMenuEvent, Patrol>> selectionMenuActions = new HashMap<String, BiConsumer<SelectionMenuEvent, Patrol>>() {{
        put("patrol-vehicle-selection", PatrolListener.this::performChangePatrolVehicle);
        put("patrol-unit-selection", PatrolListener.this::performChangePatrolUnit);
        put("patrol-status-selection", PatrolListener.this::performChangePatrolStatus);
    }};
    private final String patrolChannelId;
    private final String adminLogsChannelId;

    private PatrolListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.patrolChannelId = discordBot.getBotConfig().retrieveValue("patrol-channel");
        this.adminLogsChannelId = discordBot.getBotConfig().retrieveValue("admin-logs-channel");
    }

    @Override
    public void onButtonClick(@NotNull final ButtonClickEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        final String[] splitComponentId = event.getComponentId().split(":");
        assert splitComponentId.length == 2 : "Invalid component id!";
        final String actionName = splitComponentId[0];
        final int patrolId = Integer.parseInt(splitComponentId[1]);
        final Patrol patrol = discordBot.getPatrolController().getPatrol(patrolId);
        assert patrol != null : "Patrol of clicked button could not be found!";
        assert event.getMember() != null : "Member of ButtonClickEvent was null!";
        final String discordId = event.getMember().getId();
        discordBot.getInformationGrabber().findEmployeeByDiscordId(discordId).thenAccept(optionalEmployee -> optionalEmployee.ifPresent(employee -> {
            final TriConsumer<ButtonClickEvent, Patrol, Employee> action = buttonActions.get(actionName);
            action.accept(event, patrol, employee);
            if (!event.isAcknowledged()) {
                event.deferEdit().setEmbeds(discordBot.getPatrolController().buildPatrolEmbed(patrol)).queue();
            }
        }));
    }

    @Override
    public void onSelectionMenu(@NotNull final SelectionMenuEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (event.getSelectedOptions() == null) {
            return;
        }
        final String[] splitComponentId = event.getComponentId().split(":");
        assert splitComponentId.length == 2 : "Invalid selection menu id!";
        final String actionName = splitComponentId[0];
        final Patrol patrol = discordBot.getPatrolController().getPatrol(Integer.parseInt(splitComponentId[1]));
        selectionMenuActions.get(actionName).accept(event, patrol);
        event.deferEdit().setEmbeds(discordBot.getPatrolController().buildPatrolEmbed(patrol)).setActionRows(discordBot.getPatrolController().buildActionRow(patrol)).queue();
    }

    private void performJoinPatrol(final ButtonClickEvent event, final Patrol patrol, final Employee employee) {
        if (patrol.isFull()) {
            event.reply("Diese Streife ist bereits voll!").setEphemeral(true).queue();
            return;
        } else if (discordBot.getPatrolController().isEmployeeInAnyPatrol(employee)) {
            event.reply("Du bist bereits in einer Streife!").setEphemeral(true).queue();
            return;
        }
        patrol.addMember(employee);
        discordBot.getLogController().postPatrolJoin(discordBot.getActiveGuild().retrieveMemberById(employee.getDiscordId()).complete(), patrol);
    }

    private void performLeavePatrol(final ButtonClickEvent event, final Patrol patrol, final Employee employee) {
        if (!patrol.getMembers().contains(employee)) {
            event.reply("Du bist nicht in dieser Streife!").setEphemeral(true).queue();
            return;
        }
        patrol.removeMember(employee);
        discordBot.getLogController().postPatrolLeave(discordBot.getActiveGuild().retrieveMemberById(employee.getDiscordId()).complete(), patrol);
        if (patrol.isEmpty()) {
            patrol.setVehicle(null);
            patrol.setUnit(null);
            patrol.setStatus(null);
        }
    }

    private void showSetPatrolVehicleSelection(final ButtonClickEvent event, final Patrol patrol, final Employee employee) {
        final SelectionMenu.Builder menu = SelectionMenu.create(String.format("patrol-vehicle-selection:%d", patrol.getId()));

        menu.addOptions(discordBot.getInformationGrabber().getPatrolVehicles().stream().map(patrolVehicle -> SelectOption.of(
                patrolVehicle.getName(),
                patrolVehicle.getId().toString()
        )).collect(Collectors.toList()));

        if (patrol.getVehicle() != null) {
            final PatrolVehicle vehicle = patrol.getVehicle();
            menu.setDefaultOptions(Collections.singletonList(SelectOption.of(vehicle.getName(), vehicle.getId().toString())));
        }

        menu.setMinValues(0);
        menu.setMaxValues(1);
        menu.setPlaceholder("Wähle ein Fahrzeug aus");

        event.deferEdit().setActionRow(menu.build()).queue();
    }

    private void showSetPatrolUnitSelection(final ButtonClickEvent event, final Patrol patrol, final Employee employee) {
        final SelectionMenu.Builder menu = SelectionMenu.create(String.format("patrol-unit-selection:%d", patrol.getId()));

        menu.addOptions(discordBot.getInformationGrabber().getPatrolUnits().stream().map(patrolUnit -> SelectOption.of(
                patrolUnit.getName(),
                patrolUnit.getId().toString()
        )).collect(Collectors.toList()));

        if (patrol.getUnit() != null) {
            final PatrolUnit unit = patrol.getUnit();
            menu.setDefaultOptions(Collections.singletonList(SelectOption.of(unit.getName(), unit.getId().toString())));
        }

        menu.setMinValues(0);
        menu.setMaxValues(1);
        menu.setPlaceholder("Wähle eine Einheit aus");

        event.deferEdit().setActionRow(menu.build()).queue();
    }

    private void showSetPatrolStatusSelection(final ButtonClickEvent event, final Patrol patrol, final Employee employee) {
        final SelectionMenu.Builder menu = SelectionMenu.create(String.format("patrol-status-selection:%d", patrol.getId()));

        menu.addOptions(discordBot.getInformationGrabber().getPatrolStatuses().stream().map(patrolStatus -> SelectOption.of(
                patrolStatus.getName(),
                patrolStatus.getId().toString()
        )).collect(Collectors.toList()));

        if (patrol.getStatus() != null) {
            final PatrolStatus status = patrol.getStatus();
            menu.setDefaultOptions(Collections.singletonList(SelectOption.of(status.getName(), status.getId().toString())));
        }

        menu.setMinValues(0);
        menu.setMaxValues(1);
        menu.setPlaceholder("Wähle einen Status aus");

        event.deferEdit().setActionRow(menu.build()).queue();
    }

    public void performChangePatrolVehicle(final SelectionMenuEvent event, final Patrol patrol) {
        Objects.requireNonNull(event.getSelectedOptions()).stream().map(SelectOption::getValue).map(Integer::parseInt).forEach(vehicleId -> patrol.setVehicle(discordBot.getInformationGrabber().getPatrolVehicleById(vehicleId)));
    }

    public void performChangePatrolUnit(final SelectionMenuEvent event, final Patrol patrol) {
        Objects.requireNonNull(event.getSelectedOptions()).stream().map(SelectOption::getValue).map(Integer::parseInt).forEach(unitId -> patrol.setUnit(discordBot.getInformationGrabber().getPatrolUnitById(unitId)));
    }

    public void performChangePatrolStatus(final SelectionMenuEvent event, final Patrol patrol) {
        Objects.requireNonNull(event.getSelectedOptions()).stream().map(SelectOption::getValue).map(Integer::parseInt).forEach(statusId -> patrol.setStatus(discordBot.getInformationGrabber().getPatrolStatusById(statusId)));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(final GenericComponentInteractionCreateEvent event) {
        return event.getChannel().getId().equals(patrolChannelId);
    }

    public static PatrolListener forBot(final DiscordBot discordBot) {
        return new PatrolListener(discordBot);
    }
}
