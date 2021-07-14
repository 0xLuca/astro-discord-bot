package at.zieserl.astrodiscordbot;

import at.zieserl.astrodiscordbot.bot.DiscordBot;
import at.zieserl.astrodiscordbot.config.BotConfig;
import at.zieserl.astrodiscordbot.config.PropertiesBotConfig;
import at.zieserl.astrodiscordbot.i18n.MessageStore;
import at.zieserl.astrodiscordbot.i18n.ResourceBundleMessageStore;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Main {
    /**
     * Program entry point
     *
     * @param args first param is the Discord api token, second param the department type to load messages from, and the third param is the server id to run this instance for
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Use the following argument structure: <Token> <Department> <Server ID>");
        }
        final Locale department = new Locale(args[1]);
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle("messages", department);
        } catch (MissingResourceException e) {
            throw new IllegalArgumentException("Could not find message bundle with given department type!", e);
        }
        final BotConfig config = PropertiesBotConfig.loadFromPath(new File("config.properties"), "config.properties");
        final MessageStore messageStore = ResourceBundleMessageStore.create(bundle);
        final DiscordBot bot = DiscordBot.create(messageStore, config, args[2]);
        try {
            bot.start(args[0]);
        } catch (LoginException e) {
            throw new IllegalArgumentException("Could not login with given token!", e);
        }
    }
}