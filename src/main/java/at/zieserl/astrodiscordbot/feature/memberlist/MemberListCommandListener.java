package at.zieserl.astrodiscordbot.feature.memberlist;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.employee.Rank;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class MemberListCommandListener extends ListenerAdapter {
    private final DiscordBot discordBot;
    private final String adminCommandsChannelId;

    private MemberListCommandListener(final DiscordBot discordBot) {
        this.discordBot = discordBot;
        this.adminCommandsChannelId = discordBot.getBotConfig().retrieveValue("admin-commands-channel");
    }

    @Override
    public void onSlashCommand(@NotNull final SlashCommandEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getName().equalsIgnoreCase("memberlist")) {
            return;
        }
        event.reply("Die Mitarbeiterliste wird geladen...").queue(interactionHook -> new Thread(() -> {
//            final EmbedBuilder builder = new EmbedBuilder();
//
//            builder.setTitle("Mitarbeiterliste ");
//            builder.setColor(Color.RED);

            final List<Employee> employees = discordBot.getInformationGrabber().retrieveAllEmployees();

            final StringBuilder message = new StringBuilder();
            discordBot.getInformationGrabber().getRanks().stream().sorted(Comparator.comparingInt(Rank::getId)).forEach(rank -> {
                final List<Employee> employeesWithRank = employees.stream().filter(employee -> employee.getRank().equals(rank)).collect(Collectors.toList());
                final String title = String.format("%s (%d/%d)", rank.getName(), employeesWithRank.size(), rank.getMaxMembers());
                message.append("**").append(title).append("**\n");

                employeesWithRank.stream().sorted(Comparator.comparingInt(Employee::getServiceNumber)).forEach(employee -> {
                    message.append(String.format("<@!%s>", employee.getDiscordId()));
                    if (employee.getWorktime() > 0) {
                        final long seconds = employee.getWorktime() / 1000;
                        final String formattedSessionTime = String.format(" (Gesamtdienstzeit: %dh, %dm, %ds)", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
                        message.append(formattedSessionTime);
                    }
                    if (employee.getPhoneNumber().isEmpty() || employee.getBirthDate().isEmpty()) {
                        message.append(" (**X**)");
                    }
                    message.append('\n');
                });
                message.append('\n');
            });

//            builder.setDescription(message.toString());
//            builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getEffectiveAvatarUrl());

            final List<MessageEmbed> embeds = provideBuildersForMemberList(message.toString()).stream().map(EmbedBuilder::build).collect(Collectors.toList());
            interactionHook.editOriginal("Die Mitarbeiterliste wurde erfolgreich geladen!").setEmbeds(embeds.get(0)).queue();
            if (embeds.size() > 1) {
                for (int i = 1; i < embeds.size(); i++) {
                    event.getTextChannel().sendMessageEmbeds(embeds.get(i)).queue();
                }
            }
        }).start());
    }

    private List<EmbedBuilder> provideBuildersForMemberList(final String message) {
        if (message.length() <= 4096) {
            return new ArrayList<EmbedBuilder>() {{
                final EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("Mitarbeiterliste");
                builder.setDescription(message.isEmpty() ? "Keine Mitarbeiter gefunden" : message);
                builder.setColor(Color.RED);
                builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getJDA().getSelfUser().getEffectiveAvatarUrl());
                add(builder);
            }};
        }

        final String cutMessage = message.substring(0, 4096);
        final String newMessage = message.substring(4096);
        final List<EmbedBuilder> builders = new ArrayList<>();
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Mitarbeiterliste");
        builder.setDescription(cutMessage);
        builders.add(builder);
        builders.addAll(provideBuildersForMemberList(newMessage));
        builder.setColor(Color.RED);
        builder.setFooter(discordBot.getMessageStore().provide("type"), discordBot.getActiveGuild().getJDA().getSelfUser().getEffectiveAvatarUrl());
        return builders;
    }

    @Override
    public void onGuildMessageReceived(@NotNull final GuildMessageReceivedEvent event) {
        if (!discordBot.shouldHandleEvent(event) || !shouldHandleEvent(event)) {
            return;
        }
        if (!event.getMessage().getContentRaw().equalsIgnoreCase("!memberlist")) {
            return;
        }

        event.getChannel().sendMessage("Die Mitarbeiterliste wird geladen...").queue(message -> new Thread(() -> {
            final EmbedBuilder builder = new EmbedBuilder();

            builder.setTitle("Mitarbeiterliste ");
            builder.setColor(Color.RED);

            final List<Employee> employees = discordBot.getInformationGrabber().retrieveAllEmployees();

            final StringBuilder mentions = new StringBuilder();
            discordBot.getInformationGrabber().getRanks().stream().sorted(Comparator.comparingInt(Rank::getId)).forEach(rank -> {
                final List<Employee> employeesWithRank = employees.stream().filter(employee -> employee.getRank().equals(rank)).collect(Collectors.toList());
                final String title = String.format("%s (%d/%d)", rank.getName(), employeesWithRank.size(), rank.getMaxMembers());
                mentions.append("**").append(title).append("**\n");
                employeesWithRank.forEach(employee -> {
                    Member member = discordBot.getActiveGuild().getMemberById(employee.getDiscordId());
                    if (member == null) {
                        member = discordBot.getActiveGuild().retrieveMemberById(employee.getDiscordId()).complete();
                    }
                    mentions.append(member.getAsMention()).append('\n');
                });
                mentions.append('\n');
            });

            builder.setDescription(mentions.toString());
            builder.setFooter(discordBot.getMessageStore().provide("type"), event.getJDA().getSelfUser().getEffectiveAvatarUrl());

            message.delete().queue();
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
        }).start());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(final GenericInteractionCreateEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(adminCommandsChannelId);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean shouldHandleEvent(final GenericGuildMessageEvent event) {
        return Objects.requireNonNull(event.getChannel()).getId().equalsIgnoreCase(adminCommandsChannelId);
    }

    public static MemberListCommandListener forBot(final DiscordBot discordBot) {
        return new MemberListCommandListener(discordBot);
    }
}
