package at.zieserl.astrodiscordbot.patrol;

public final class PatrolUnit {
    private final Integer id;
    private final String name;

    public PatrolUnit(final Integer id, final String name) {
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
