package at.zieserl.astrodiscordbot.patrol;

import at.zieserl.astrodiscordbot.employee.Employee;

import java.util.ArrayList;
import java.util.List;

public class Patrol {
    private int id;
    private final int maxMembers;
    private final List<Employee> members = new ArrayList<>();
    private PatrolVehicle vehicle;
    private PatrolUnit unit;
    private PatrolStatus status;

    public Patrol(final int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public int getId() {
        return id;
    }

    public Integer getDisplayId() {
        return id + 1;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public PatrolVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(final PatrolVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public PatrolUnit getUnit() {
        return unit;
    }

    public void setUnit(final PatrolUnit unit) {
        this.unit = unit;
    }

    public PatrolStatus getStatus() {
        return status;
    }

    public void setStatus(final PatrolStatus status) {
        this.status = status;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public List<Employee> getMembers() {
        return members;
    }

    public void addMember(final Employee employee) {
        members.add(employee);
    }

    public void removeMember(final Employee employee) {
        members.remove(employee);
    }

    public boolean isFull() {
        return getMembers().size() >= getMaxMembers();
    }

    public boolean isEmpty() {
        return getMembers().size() == 0;
    }
}
