package at.zieserl.astrodiscordbot.employee;

public final class Rank {
    private final Integer id;
    private final Long discordId;
    private final String name;
    private final int startingServiceNumber;
    private final int maxMembers;

    public Rank(final Integer id, final Long discordId, final String name, final int startingServiceNumber, final int maxMembers) {
        this.id = id;
        this.discordId = discordId;
        this.name = name;
        this.startingServiceNumber = startingServiceNumber;
        this.maxMembers = maxMembers;
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

    public int getStartingServiceNumber() {
        return startingServiceNumber;
    }

    public int getMaxMembers() {
        return maxMembers;
    }
}
