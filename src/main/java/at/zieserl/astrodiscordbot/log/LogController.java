package at.zieserl.astrodiscordbot.log;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.employee.Employee;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

public final class LogController {
    private static final DateFormat timeStampFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm");
    private final DiscordBot discordBot;
    private final Role employeeRole;
    private final TextChannel logsChannel;

    private LogController(DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.employeeRole = discordBot.getActiveGuild().getRoleById(discordBot.getBotConfig().retrieveValue("employee-role"));
        this.logsChannel = discordBot.getActiveGuild().getTextChannelById(discordBot.getBotConfig().retrieveValue("public-logs-channel"));
    }

    public void postRankChange(Employee... employees) {
        if (employees.length == 0) {
            throw new IllegalArgumentException("Need to pass at least one employee to post rank change!");
        }
        final EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(discordBot.getMessageStore().provide("log-title").replace("%employee-role%", employeeRole.getAsMention()).replace("%timestamp%", timeStampFormat.format(Date.from(Instant.now()))));
        builder.setColor(Color.RED);

        Arrays.stream(employees).forEach(employee -> {
            Member member = discordBot.getActiveGuild().getMemberById(employee.getDiscordId());
            Role role = discordBot.getActiveGuild().getRoleById(employee.getRank().getDiscordId());
            assert member != null && role != null : "Could not find member for employee id / role for rank id!";
            builder.addField("Neuer Dienstgrad", discordBot.getMessageStore().provide("new-rank-log").replace("%mention%", member.getAsMention()).replace("%role%", role.getAsMention()).replace("%service-number%", employee.getServiceNumber().toString()), false);
        });

        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getAvatarUrl());
        logsChannel.sendMessage(builder.build()).queue();
    }

    public static LogController forBot(DiscordBot discordBot) {
        return new LogController(discordBot);
    }
}
