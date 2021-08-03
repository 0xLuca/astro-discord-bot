package at.zieserl.astrodiscordbot.log;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.patrol.Patrol;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

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
    private final TextChannel adminLogsChannel;

    private LogController(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.employeeRole = discordBot.getActiveGuild().getRoleById(discordBot.getBotConfig().retrieveValue("employee-role"));
        this.logsChannel = discordBot.getActiveGuild().getTextChannelById(discordBot.getBotConfig().retrieveValue("public-logs-channel"));
        this.adminLogsChannel = discordBot.getActiveGuild().getTextChannelById(discordBot.getBotConfig().retrieveValue("admin-logs-channel"));
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

        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getEffectiveAvatarUrl());
        logsChannel.sendMessageEmbeds(builder.build()).queue();
    }

    public void postUprank(final Employee... employees) {
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
            String message = discordBot.getMessageStore().provide("uprank-log");
            message = message.replace("%mention%", member.getAsMention());
            message = message.replace("%role%", role.getAsMention());
            message = message.replace("%service-number%", employee.getServiceNumber().toString());
            builder.addField("Neuer Dienstgrad", message, false);
        });

        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getEffectiveAvatarUrl());
        logsChannel.sendMessageEmbeds(builder.build()).queue();
    }

    public void postDownrank(final Employee... employees) {
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
            String message = discordBot.getMessageStore().provide("downrank-log");
            message = message.replace("%mention%", member.getAsMention());
            message = message.replace("%role%", role.getAsMention());
            message = message.replace("%service-number%", employee.getServiceNumber().toString());
            builder.addField("Neuer Dienstgrad", message, false);
        });

        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getEffectiveAvatarUrl());
        logsChannel.sendMessageEmbeds(builder.build()).queue();
    }

    public void postTermination(final Member member, final String reason) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("log-title").replace("%timestamp%", timeStampFormat.format(Date.from(Instant.now()))));
        builder.setColor(Color.RED);
        builder.setDescription(discordBot.getMessageStore().provide("log-description").replace("%employee-role%", employeeRole.getAsMention()));
        builder.addField("Kündigung", discordBot.getMessageStore().provide("terminated-text").replace("%mention%", member.getAsMention()).replace("%reason%", reason), false);
        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getEffectiveAvatarUrl());
        logsChannel.sendMessageEmbeds(builder.build()).queue();
    }

    public void postSelfTermination(final User user) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("log-title").replace("%timestamp%", timeStampFormat.format(Date.from(Instant.now()))));
        builder.setColor(Color.RED);
        builder.setDescription(discordBot.getMessageStore().provide("log-description").replace("%employee-role%", employeeRole.getAsMention()));
        builder.addField("Kündigung", discordBot.getMessageStore().provide("self-terminated-text").replace("%mention%", user.getAsMention()), false);
        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getEffectiveAvatarUrl());
        logsChannel.sendMessageEmbeds(builder.build()).queue();
    }

    public void postPatrolJoin(final Member member, final Patrol patrol) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("join-patrol-log-title"));
        builder.setColor(Color.GREEN);
        builder.setDescription(discordBot.getMessageStore().provide("join-patrol-log-text").replace("%mention%", member.getAsMention()).replace("%display-id%", patrol.getDisplayId().toString()));
        builder.setThumbnail(member.getUser().getEffectiveAvatarUrl());
        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getEffectiveAvatarUrl());
        adminLogsChannel.sendMessageEmbeds(builder.build()).queue();
    }

    public void postPatrolLeave(final Member member, final Patrol patrol) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(discordBot.getMessageStore().provide("leave-patrol-log-title"));
        builder.setColor(Color.RED);
        builder.setDescription(discordBot.getMessageStore().provide("leave-patrol-log-text").replace("%mention%", member.getAsMention()).replace("%display-id%", patrol.getDisplayId().toString()));
        builder.setThumbnail(member.getUser().getEffectiveAvatarUrl());
        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getSelfMember().getUser().getEffectiveAvatarUrl());
        adminLogsChannel.sendMessageEmbeds(builder.build()).queue();
    }

    public void postWarn(final Employee employee, final Member member) {

    }

    public static LogController forBot(final DiscordBot discordBot) {
        return new LogController(discordBot);
    }
}