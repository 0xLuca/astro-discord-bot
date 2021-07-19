package at.zieserl.astrodiscordbot.log;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.employee.Employee;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.Color;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

public final class LogController {
    private static final DateFormat timeStampFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private final DiscordBot discordBot;
    private final Role employeeRole;
    private final TextChannel logsChannel;

    private LogController(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.employeeRole = discordBot.getActiveGuild().getRoleById(discordBot.getBotConfig().retrieveValue("employee-role"));
        this.logsChannel = discordBot.getActiveGuild().getTextChannelById(discordBot.getBotConfig().retrieveValue("public-logs-channel"));
    }

    public void postNewEmployee(final Employee employee) {
        final EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(discordBot.getMessageStore().provide("log-title").replace("%timestamp%", timeStampFormat.format(Date.from(Instant.now()))));
        builder.setColor(Color.RED);

        builder.setDescription(discordBot.getMessageStore().provide("log-description").replace("%employee-role%", employeeRole.getAsMention()));

        final Member member = discordBot.getActiveGuild().retrieveMemberById(employee.getDiscordId()).complete();
        final Role role = discordBot.getActiveGuild().getRoleById(employee.getRank().getDiscordId());
        assert member != null && role != null : "Could not find member for employee id / role for rank id!";
        String message = discordBot.getMessageStore().provide("new-employee-log");
        message = message.replace("%mention%", member.getAsMention());
        message = message.replace("%role%", role.getAsMention());
        message = message.replace("%service-number%", employee.getServiceNumber().toString());
        builder.addField("Neuer Mitarbeiter", message, false);

        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getAvatarUrl());
        logsChannel.sendMessageEmbeds(builder.build()).queue();
    }

    public void postRankChange(final Employee... employees) {
        if (employees.length == 0) {
            throw new IllegalArgumentException("Need to pass at least one employee to post rank change!");
        }
        final EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(discordBot.getMessageStore().provide("log-title").replace("%timestamp%", timeStampFormat.format(Date.from(Instant.now()))));
        builder.setColor(Color.RED);

        builder.setDescription(discordBot.getMessageStore().provide("log-description").replace("%employee-role%", employeeRole.getAsMention()));

        Arrays.stream(employees).forEach(employee -> {
            final Member member = discordBot.getActiveGuild().retrieveMemberById(employee.getDiscordId()).complete();
            final Role role = discordBot.getActiveGuild().getRoleById(employee.getRank().getDiscordId());
            assert member != null && role != null : "Could not find member for employee id / role for rank id!";
            String message = discordBot.getMessageStore().provide("new-rank-log");
            message = message.replace("%mention%", member.getAsMention());
            message = message.replace("%role%", role.getAsMention());
            message = message.replace("%service-number%", employee.getServiceNumber().toString());
            builder.addField("Neuer Dienstgrad", message, false);
        });

        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getAvatarUrl());
        logsChannel.sendMessageEmbeds(builder.build()).queue();
    }

    public static LogController forBot(final DiscordBot discordBot) {
        return new LogController(discordBot);
    }
}
