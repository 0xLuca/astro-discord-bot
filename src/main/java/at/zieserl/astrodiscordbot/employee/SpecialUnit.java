package at.zieserl.astrodiscordbot.employee;

public final class SpecialUnit {
    private final Integer id;
    private final Long discordId;
    private final String name;

    public SpecialUnit(final Integer id, final Long discordId, final String name) {
        this.id = id;
        this.discordId = discordId;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public Long getDiscordId() {
        return discordId;
    }

    public String getName() {
        return name;
    }
}
