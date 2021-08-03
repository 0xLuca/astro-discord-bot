package at.zieserl.astrodiscordbot.patrol;

public final class PatrolVehicle {
    private final Integer id;
    private final String name;

    public PatrolVehicle(final Integer id, final String name) {
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