package at.zieserl.astrodiscordbot.i18n;

import java.util.ResourceBundle;

public final class ResourceBundleMessageStore implements MessageStore {
    private final ResourceBundle resourceBundle;

    private ResourceBundleMessageStore(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    @Override
    public String provide(String key) {
        return resourceBundle.getString(key);
    }

    public static ResourceBundleMessageStore create(ResourceBundle resourceBundle) {
        return new ResourceBundleMessageStore(resourceBundle);
    }
}
