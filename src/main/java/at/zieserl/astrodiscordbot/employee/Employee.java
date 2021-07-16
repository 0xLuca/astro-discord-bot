package at.zieserl.astrodiscordbot.employee;

import java.util.Arrays;
import java.util.List;

public final class Employee {
    private final Integer serviceNumber;
    private final String name;
    private final Rank rank;
    private final Integer warnings;
    private final Integer worktime;
    private final List<Education> educationList;

    public Employee(Integer serviceNumber, String name, Rank rank, Integer warnings, Integer worktime, Education... educations) {
        this.serviceNumber = serviceNumber;
        this.name = name;
        this.rank = rank;
        this.warnings = warnings;
        this.worktime = worktime;
        this.educationList = Arrays.asList(educations);
    }

    public Integer getServiceNumber() {
        return serviceNumber;
    }

    public String getName() {
        return name;
    }

    public Rank getRank() {
        return rank;
    }

    public Integer getWarnings() {
        return warnings;
    }

    public Integer getWorktime() {
        return worktime;
    }

    public List<Education> getEducationList() {
        return educationList;
    }
}