package at.zieserl.astrodiscordbot.patrol;

public class PatrolStatus {
    private final Integer id;
    private final String name;

    public PatrolStatus(final Integer id, final String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
