package at.zieserl.astrodiscordbot.patrol;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.employee.Employee;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

import java.util.HashMap;
import java.util.Map;

public final class PatrolController {
    private final DiscordBot discordBot;
    private final Map<Integer, Patrol> patrolMap = new HashMap<>();

    private PatrolController(final DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    public int addPatrolAndSetId(final Patrol patrol) {
        final int id = patrolMap.size();
        patrol.setId(id);
        patrolMap.put(id, patrol);
        return id;
    }

    public Patrol getPatrol(final int id) {
        return patrolMap.get(id);
    }

    public synchronized MessageEmbed buildPatrolEmbed(final Patrol patrol) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(String.format("Streife %d (%d/%d)", patrol.getDisplayId(), patrol.getMembers().size(), patrol.getMaxMembers()));
        builder.addField("Mitglieder", formatPatrolMembers(patrol), false);
        if (patrol.getVehicle() != null) {
            builder.addField("Fahrzeug", patrol.getVehicle().getName(), true);
        }
        if (patrol.getUnit() != null) {
            builder.addField("Einheit", patrol.getUnit().getName(), true);
        }
        if (patrol.getStatus() != null) {
            builder.addField("Status", patrol.getStatus().getName(), true);
        }
        return builder.build();
    }

    public ActionRow buildActionRow(final Patrol patrol) {
        return ActionRow.of(
                Button.success("join-patrol:" + patrol.getId(), discordBot.getMessageStore().provide("join-patrol-text")),
                Button.danger("leave-patrol:" + patrol.getId(), discordBot.getMessageStore().provide("leave-patrol-text")),
                Button.primary("show-patrol-vehicle-selection:" + patrol.getId(), discordBot.getMessageStore().provide("set-patrol-vehicle-text")),
                Button.primary("show-set-patrol-unit-selection:" + patrol.getId(), discordBot.getMessageStore().provide("set-patrol-unit-text")),
                Button.primary("show-set-patrol-status-selection:" + patrol.getId(), discordBot.getMessageStore().provide("set-patrol-status-text"))
        );
    }

    private String formatPatrolMembers(final Patrol patrol) {
        final StringBuilder builder = new StringBuilder();
        patrol.getMembers().forEach(employee -> {
            builder.append(discordBot.getActiveGuild().retrieveMemberById(employee.getDiscordId()).complete().getAsMention()).append('\n');
        });
        for (int i = patrol.getMembers().size(); i < patrol.getMaxMembers(); i++) {
            builder.append("-\n");
        }
        return builder.toString();
    }

    public boolean isEmployeeInAnyPatrol(final Employee employee) {
        return patrolMap.values().stream().anyMatch(patrol -> patrol.getMembers().contains(employee));
    }

    public Map<Integer, Patrol> getPatrolMap() {
        return patrolMap;
    }

    public static PatrolController forBot(final DiscordBot discordBot) {
        return new PatrolController(discordBot);
    }
}
