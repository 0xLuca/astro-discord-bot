package at.zieserl.astrodiscordbot.employee;

import net.dv8tion.jda.api.entities.Member;

import java.util.Arrays;
import java.util.List;

public final class Employee {
    private Integer id;
    private Integer serviceNumber;
    private final String discordId;
    private final String name;
    private Rank rank;
    private Integer warnings;
    private Integer worktime;
    private final List<Education> educationList;
    private final List<SpecialUnit> specialUnitList;

    public Employee(Integer id, Integer serviceNumber, String discordId, String name, Rank rank, Integer warnings, Integer worktime, Education[] educations, SpecialUnit[] specialUnits) {
        this.id = id;
        this.serviceNumber = serviceNumber;
        this.discordId = discordId;
        this.name = name;
        this.rank = rank;
        this.warnings = warnings;
        this.worktime = worktime;
        this.educationList = Arrays.asList(educations);
        this.specialUnitList = Arrays.asList(specialUnits);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        if (id != -1) {
            throw new RuntimeException("Cannot set employee id more than once!");
        }
        this.id = id;
    }

    public Integer getServiceNumber() {
        return serviceNumber;
    }

    public void setServiceNumber(Integer serviceNumber) {
        this.serviceNumber = serviceNumber;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getName() {
        return name;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public Integer getWarnings() {
        return warnings;
    }

    public void setWarnings(Integer warnings) {
        this.warnings = warnings;
    }

    public Integer getWorktime() {
        return worktime;
    }

    public void setWorktime(Integer worktime) {
        this.worktime = worktime;
    }

    public List<Education> getEducationList() {
        return educationList;
    }

    public List<SpecialUnit> getSpecialUnitList() {
        return specialUnitList;
    }

    public void updateNickname(Member member) {
        member.modifyNickname(String.format("[%s] %s", formatServiceNumber(getServiceNumber()), getName())).queue();
    }

    private String formatServiceNumber(int serviceNumber) {
        return String.format("%02d", serviceNumber);
    }
}