package at.zieserl.astrodiscordbot.i18n;

import java.util.ResourceBundle;

public final class ResourceBundleMessageStore implements MessageStore {
    private final ResourceBundle resourceBundle;

    private ResourceBundleMessageStore(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    @Override
    public String provide(final String key) {
        return resourceBundle.getString(key);
    }

    public static ResourceBundleMessageStore create(final ResourceBundle resourceBundle) {
        return new ResourceBundleMessageStore(resourceBundle);
    }
}
