package at.zieserl.astrodiscordbot.feature.info;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.Channels;
import at.zieserl.astrodiscordbot.constant.Roles;
import at.zieserl.astrodiscordbot.employee.Education;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.employee.Rank;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class InfoListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final Map<String, BiConsumer<ButtonClickEvent, Employee>> actions = new HashMap<String, BiConsumer<ButtonClickEvent, Employee>>() {{
        put("promote", InfoListener.this::performPromote);
        put("demote", InfoListener.this::performDemote);
    }};
    private final Map<String, Employee> employeeCache = new HashMap<>();

    private InfoListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
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
                builder.addField("Dienstnummer", employee.getServiceNumber().toString(), false);
                builder.addField("Name", employee.getName(), false);
                builder.addField("Dienstgrad", employee.getRank().getName(), false);
                String educations = convertEducationsToString(employee.getEducationList());
                if (!educations.isEmpty()) {
                    builder.addField("Ausbildungen", educations, false);
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
            event.reply("Dieser Mitarbeiter hat bereits den höchsten Rang!").queue();
            return;
        }

        Member member = Objects.requireNonNull(event.getGuild()).retrieveMemberById(employee.getDiscordId()).complete();
        Roles.removeRole(member, String.valueOf(currentRank.getDiscordId()));
        Rank newRank = discordBot.getInformationGrabber().getNextHigherRank(currentRank);
        Roles.grantRole(member, String.valueOf(newRank.getDiscordId()));
        employee.setRank(newRank);
        event.reply(String.format("%s wurde erfolgreich zu %s befördert!", member.getEffectiveName(), newRank.getName())).queue();
    }

    private void performDemote(ButtonClickEvent event, Employee employee) {
        int currentRankId = employee.getRank().getId();
        Rank currentRank = discordBot.getInformationGrabber().getRankById(currentRankId);
        if (currentRank == discordBot.getInformationGrabber().getLowestRank()) {
            event.reply("Dieser Mitarbeiter hat bereits den niedrigsten Rang!").queue();
            return;
        }

        Member member = Objects.requireNonNull(event.getGuild()).retrieveMemberById(employee.getDiscordId()).complete();
        Roles.removeRole(member, String.valueOf(currentRank.getDiscordId()));
        Rank newRank = discordBot.getInformationGrabber().getNextLowerRank(currentRank);
        Roles.grantRole(member, String.valueOf(newRank.getDiscordId()));
        employee.setRank(newRank);
        event.reply(String.format("%s wurde erfolgreich zu %s degradiert!", member.getEffectiveName(), newRank.getName())).queue();
    }

    private String convertEducationsToString(List<Education> educations) {
        StringBuilder educationsAsString = new StringBuilder();
        educations.forEach(education -> educationsAsString.append(education.getName()).append("\n"));
        return educationsAsString.toString();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(Channels.VERWALTUNG_COMMANDS_CHANNEL_ID);
    }

    public static InfoListener forBot(DiscordBot discordBot) {
        return new InfoListener(discordBot);
    }
}
