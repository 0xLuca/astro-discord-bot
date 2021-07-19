package at.zieserl.astrodiscordbot.database;

import at.zieserl.astrodiscordbot.employee.Education;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.employee.Rank;
import at.zieserl.astrodiscordbot.employee.SpecialUnit;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class InformationGrabber {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MysqlConnection connection;
    private final Map<String, Employee> employeeCache = new HashMap<>();
    private final Map<Integer, Rank> ranks = new HashMap<>();
    private final Map<Integer, Education> educations = new HashMap<>();
    private final Map<Integer, SpecialUnit> specialUnits = new HashMap<>();

    private InformationGrabber(MysqlConnection connection) {
        this.connection = connection;
    }

    public void reloadConstantsCache() {
        connection.executeQuery("SELECT id, discord_id, name, starting_service_number, max_members FROM rank").ifPresent(ranksResultSet -> {
            ranks.clear();
            try {
                while (ranksResultSet.next()) {
                    int rankId = ranksResultSet.getInt("id");
                    ranks.put(rankId,
                            new Rank(
                                    rankId,
                                    ranksResultSet.getLong("discord_id"),
                                    ranksResultSet.getString("name"),
                                    ranksResultSet.getInt("starting_service_number"),
                                    ranksResultSet.getInt("max_members")
                            )
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        connection.executeQuery("SELECT id, discord_id, name FROM education").ifPresent(educationsResultSet -> {
            educations.clear();
            try {
                while (educationsResultSet.next()) {
                    int educationId = educationsResultSet.getInt("id");
                    educations.put(educationId,
                            new Education(
                                    educationId,
                                    educationsResultSet.getLong("discord_id"),
                                    educationsResultSet.getString("name")
                            )
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        connection.executeQuery("SELECT id, discord_id, name FROM special_unit").ifPresent(specialUnitsResultSet -> {
            specialUnits.clear();
            try {
                while (specialUnitsResultSet.next()) {
                    int specialUnitId = specialUnitsResultSet.getInt("id");
                    specialUnits.put(specialUnitId,
                            new SpecialUnit(
                                    specialUnitId,
                                    specialUnitsResultSet.getLong("discord_id"),
                                    specialUnitsResultSet.getString("name")
                            )
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Optional<Rank> getRankForRole(Role role) {
        return ranks.values().stream().filter(rank -> rank.getDiscordId().toString().equals(role.getId())).findFirst();
    }

    public Rank getHighestRank() {
        return ranks.values().stream().filter(rank -> rank.getId() == 1).findFirst().orElseThrow(() -> new RuntimeException("Could not get highest rank by id 1!"));
    }

    public Rank getLowestRank() {
        return getRankById(ranks.values().stream().mapToInt(Rank::getId).max().orElseThrow(() -> new RuntimeException("Could not find highest rank!")));
    }

    public Rank getNextHigherRank(Rank rank) {
        return getRankById(ranks.values().stream().mapToInt(Rank::getId).filter(id -> id < rank.getId()).max().orElseThrow(() -> new RuntimeException("Could not find higher rank!")));
    }

    public Rank getNextLowerRank(Rank rank) {
        return getRankById(ranks.values().stream().mapToInt(Rank::getId).filter(id -> id > rank.getId()).min().orElseThrow(() -> new RuntimeException("Could not find lower rank!")));
    }

    public Rank getRankById(int rankId) {
        return ranks.get(rankId);
    }

    public Education getEducationById(int educationId) {
        return educations.get(educationId);
    }

    public SpecialUnit getSpecialUnitById(int specialUnitId) {
        return specialUnits.get(specialUnitId);
    }

    public int countEmployeesWithRank(Rank rank) {
        Optional<ResultSet> optionalEmployeeCountResult = connection.executeQuery("SELECT COUNT(*) FROM employee WHERE rank_id = ?", rank.getId().toString());
        if (optionalEmployeeCountResult.isPresent()) {
            ResultSet employeeCountResult = optionalEmployeeCountResult.get();
            try {
                if (employeeCountResult.next()) {
                    return employeeCountResult.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("Could not find employees with rank " + rank.getName() + "!");
    }

    public int findNextFreeServiceNumber(Rank rank) {
        Optional<ResultSet> optionalEmployeeResult = connection.executeQuery("SELECT service_number FROM employee WHERE rank_id = ?", rank.getId().toString());
        if (!optionalEmployeeResult.isPresent()) {
            throw new RuntimeException("Could not get employees for rank " + rank.getName() + "!");
        }

        ResultSet employeeResult = optionalEmployeeResult.get();
        List<Integer> employeesWithRank = new ArrayList<>();
        try {
            while (employeeResult.next()) {
                employeesWithRank.add(employeeResult.getInt("service_number"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (employeesWithRank.size() >= rank.getMaxMembers()) {
            throw new RuntimeException("Cannot find free service number when rank has reached its max member count!");
        }

        int startingServiceNumber = rank.getStartingServiceNumber();
        int endingServiceNumber = startingServiceNumber + rank.getMaxMembers();
        for (int i = startingServiceNumber; i < endingServiceNumber; i++) {
            if (!(employeesWithRank.contains(i))) {
                return i;
            }
        }

        throw new RuntimeException("Cannot find free service number for rank " + rank.getName() + "!");
    }

    public boolean isRegistered(Member member) {
        Optional<ResultSet> optionalEmployeeNameResult = connection.executeQuery("SELECT name FROM employee WHERE discord_id = ?", member.getId());
        if (optionalEmployeeNameResult.isPresent()) {
            try {
                return optionalEmployeeNameResult.get().next();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public void registerEmployeeData(Employee employee) {
        executor.submit(() -> {
            int newId = connection.executeInsertWithReturnNewID("id", "INSERT INTO employee VALUES(0, ?, ?, ?, ?, ?, ?)",
                employee.getServiceNumber().toString(),
                employee.getName(),
                employee.getDiscordId(),
                employee.getRank().getId().toString(),
                employee.getWarnings().toString(),
                employee.getWorktime().toString());
            employee.setId(newId);
        });
    }

    public void saveEmployeeData(Employee employee) {
        executor.execute(() -> connection.executeQuery("UPDATE employee SET service_number = ?, rank_id = ?, warnings = ?, worktime = ? WHERE discord_id = ?",
                employee.getServiceNumber().toString(),
                employee.getRank().getId().toString(),
                employee.getWarnings().toString(),
                employee.getWorktime().toString(),
                employee.getDiscordId()));
    }

    public void saveEmployeeEducations(Employee employee) {
        executor.execute(() -> {
            connection.executeQuery("DELETE FROM employee_education WHERE employee_id = ?", employee.getId().toString());
            employee.getEducationList().forEach(education -> connection.executeQuery("INSERT INTO employee_education VALUES(?, ?)",
                    employee.getId().toString(),
                    education.getId().toString()
            ));
        });
    }

    public void saveEmployeeSpecialUnits(Employee employee) {
        executor.execute(() -> {
            connection.executeQuery("DELETE FROM employee_special_unit WHERE employee_id = ?", employee.getId().toString());
            employee.getSpecialUnitList().forEach(specialUnit -> connection.executeQuery("INSERT INTO employee_special_unit VALUES(?, ?)",
                    employee.getId().toString(),
                    specialUnit.getId().toString()
            ));
        });
    }

    public void saveEmployee(Employee employee) {
        saveEmployeeData(employee);
        saveEmployeeEducations(employee);
        saveEmployeeSpecialUnits(employee);
    }

    public Education[] getEducationsForEmployee(Integer id) {
        List<Education> educationList = new ArrayList<>();
        connection.executeQuery("SELECT education_id FROM employee_education WHERE employee_id = ?", id.toString()).ifPresent(educationsResultSet -> {
            try {
                while (educationsResultSet.next()) {
                    final int educationId = educationsResultSet.getInt("education_id");
                    educationList.add(getEducationById(educationId));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return educationList.toArray(new Education[0]);
    }

    public SpecialUnit[] getSpecialUnitsForEmployee(Integer id) {
        List<SpecialUnit> specialUnitList = new ArrayList<>();
        connection.executeQuery("SELECT special_unit_id FROM employee_special_unit WHERE employee_id = ?", id.toString()).ifPresent(specialUnitsResultSet -> {
            try {
                while (specialUnitsResultSet.next()) {
                    final int specialUnitId = specialUnitsResultSet.getInt("special_unit_id");
                    specialUnitList.add(getSpecialUnitById(specialUnitId));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return specialUnitList.toArray(new SpecialUnit[0]);
    }

    public CompletableFuture<Optional<Employee>> findEmployeeByDiscordId(String discordId) {
        if (employeeCache.containsKey(discordId)) {
            return CompletableFuture.completedFuture(Optional.of(employeeCache.get(discordId)));
        }
        Optional<ResultSet> optionalEmployeeResult = connection.executeQuery("SELECT id, service_number, name, rank_id, warnings, worktime FROM employee WHERE discord_id = ?", discordId);
        return optionalEmployeeResult.map(resultSet -> CompletableFuture.supplyAsync(() -> {
            Employee employee;
            try {
                if (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    employee = new Employee(
                            id,
                            resultSet.getInt("service_number"),
                            discordId,
                            resultSet.getString("name"),
                            getRankById(resultSet.getInt("rank_id")),
                            resultSet.getInt("warnings"),
                            resultSet.getLong("worktime"),
                            getEducationsForEmployee(id),
                            getSpecialUnitsForEmployee(id)
                    );
                    employeeCache.put(discordId, employee);
                } else {
                    throw new RuntimeException("Employee result set had no values in it!");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return Optional.of(employee);
        }, executor)).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
    }

    public CompletableFuture<Optional<Employee>> findEmployeeByDiscordId(long discordId) {
        return findEmployeeByDiscordId(String.valueOf(discordId));
    }

    public void removeEmployeeFromCache(String discordId) {
        employeeCache.remove(discordId);
    }

    public List<Rank> getRanks() {
        return new ArrayList<>(ranks.values());
    }

    public List<Education> getEducations() {
        return new ArrayList<>(educations.values());
    }

    public List<SpecialUnit> getSpecialUnits() {
        return new ArrayList<>(specialUnits.values());
    }

    public static InformationGrabber forConnection(MysqlConnection connection) {
        return new InformationGrabber(connection);
    }
}
