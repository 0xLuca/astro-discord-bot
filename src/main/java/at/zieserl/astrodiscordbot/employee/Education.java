package at.zieserl.astrodiscordbot.employee;

public final class Education {
    private final int id;
    private final long discordId;
    private final String name;

    public Education(int id, long discordId, String name) {
        this.id = id;
        this.discordId = discordId;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public long getDiscordId() {
        return discordId;
    }

    public String getName() {
        return name;
    }
}
