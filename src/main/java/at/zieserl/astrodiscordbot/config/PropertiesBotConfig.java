package at.zieserl.astrodiscordbot.config;

import java.io.*;
import java.util.Properties;

public final class PropertiesBotConfig implements BotConfig {
    private final Properties properties;

    private PropertiesBotConfig(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String retrieveValue(String key) {
        return properties.getProperty(key);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static PropertiesBotConfig loadFromPath(File file, String fallbackResource) throws IOException {
        Properties properties = new Properties();
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Could not create bot config");
            }
            InputStream defaultConfig = PropertiesBotConfig.class.getClassLoader().getResourceAsStream(fallbackResource);
            assert defaultConfig != null;
            byte[] defaultConfigBytes = new byte[defaultConfig.available()];
            defaultConfig.read(defaultConfigBytes);
            defaultConfig.close();
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(defaultConfigBytes);
            stream.close();
        }
        properties.load(new FileReader(file));
        return new PropertiesBotConfig(properties);
    }
}