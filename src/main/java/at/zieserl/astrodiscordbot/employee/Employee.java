package at.zieserl.astrodiscordbot.employee;

import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Employee {
    private Integer id;
    private Integer serviceNumber;
    private final String discordId;
    private final String name;
    private Rank rank;
    private Integer warnings;
    private Long worktime;
    private String phoneNumber;
    private String birthDate;
    private final List<Education> educationList;
    private final List<SpecialUnit> specialUnitList;

    public Employee(final Integer id, final Integer serviceNumber, final String discordId, final String name, final Rank rank, final Integer warnings, final Long worktime, final String phoneNumber, final String birthDate, final Education[] educations, final SpecialUnit[] specialUnits) {
        this.id = id;
        this.serviceNumber = serviceNumber;
        this.discordId = discordId;
        this.name = name;
        this.rank = rank;
        this.warnings = warnings;
        this.worktime = worktime;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.educationList = new ArrayList<>(Arrays.asList(educations));
        this.specialUnitList = new ArrayList<>(Arrays.asList(specialUnits));
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        if (this.id != 0) {
            throw new RuntimeException("Cannot set employee id more than once!");
        }
        this.id = id;
    }

    public Integer getServiceNumber() {
        return serviceNumber;
    }

    public void setServiceNumber(final Integer serviceNumber) {
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

    public void setRank(final Rank rank) {
        this.rank = rank;
    }

    public Integer getWarnings() {
        return warnings;
    }

    public void setWarnings(final Integer warnings) {
        this.warnings = warnings;
    }

    public Long getWorktime() {
        return worktime;
    }

    public void setWorktime(final Long worktime) {
        this.worktime = worktime;
    }

    public List<Education> getEducationList() {
        return educationList;
    }

    public List<SpecialUnit> getSpecialUnitList() {
        return specialUnitList;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(final String birthDate) {
        this.birthDate = birthDate;
    }

    public void updateNickname(final Member member) {
        member.modifyNickname(String.format("[%s] %s", formatServiceNumber(getServiceNumber()), getName())).complete();
    }

    private String formatServiceNumber(final int serviceNumber) {
        return String.format("%02d", serviceNumber);
    }
}