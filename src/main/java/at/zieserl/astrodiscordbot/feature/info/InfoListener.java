package at.zieserl.astrodiscordbot.feature.info;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.constant.Channels;
import at.zieserl.astrodiscordbot.employee.Education;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class InfoListener extends ListenerAdapter {
    private final DiscordBot discordBot;

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

        discordBot.getInformationGrabber().findEmployeeByDiscordId(member.getId()).thenAccept(optionalEmployee -> {
            optionalEmployee.ifPresent(employee -> {
                final TextChannel channel = event.getTextChannel();
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
                        Button.success("promote:" + member.getId(), discordBot.getMessageStore().provide("promote-text")),
                        Button.danger("demote:" + member.getId(), discordBot.getMessageStore().provide("demote-text")),
                        Button.success("add_education:" + member.getId(), discordBot.getMessageStore().provide("add-education-text")),
                        Button.danger("warn:" + member.getId(), discordBot.getMessageStore().provide("warn-text")),
                        Button.primary("additional_actions:" + member.getId(), discordBot.getMessageStore().provide("additional-actions-text"))
                ).queue();
            });
            if (!optionalEmployee.isPresent()) {
                event.reply("Dieser User wurde nicht in der Datenbank gefunden!");
            }
        });

        /*event.reply("Informationen zu " + member.getEffectiveName() + " werden geladen...").queue(interactionHook -> {

        });*/
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (event.getComponentId().equals("promote")) {

        } else if (event.getComponentId().equals("demote")) {

        } else if (event.getComponentId().equals("add_education")) {

        } else if (event.getComponentId().equals("warn")) {

        } else if (event.getComponentId().equals("additional_actions")) {

        }

        event.reply("Action: " + event.getComponentId()).queue();
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
