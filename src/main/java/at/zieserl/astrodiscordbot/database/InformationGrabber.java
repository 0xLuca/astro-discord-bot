package at.zieserl.astrodiscordbot.database;

import at.zieserl.astrodiscordbot.employee.Education;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.employee.Rank;
import at.zieserl.astrodiscordbot.employee.SpecialUnit;
import at.zieserl.astrodiscordbot.patrol.PatrolStatus;
import at.zieserl.astrodiscordbot.patrol.PatrolUnit;
import at.zieserl.astrodiscordbot.patrol.PatrolVehicle;
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
    private final Map<Integer, PatrolUnit> patrolUnits = new HashMap<>();
    private final Map<Integer, PatrolVehicle> patrolVehicles = new HashMap<>();
    private final Map<Integer, PatrolStatus> patrolStatuses = new HashMap<>();

    private InformationGrabber(final MysqlConnection connection) {
        this.connection = connection;
    }

    public void reloadConstantsCache(final boolean reloadPatrolCache) {
        connection.executeQuery("SELECT * FROM rank").ifPresent(ranksResultSet -> {
            ranks.clear();
            try {
                while (ranksResultSet.next()) {
                    final int rankId = ranksResultSet.getInt("id");
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
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });

        connection.executeQuery("SELECT * FROM education").ifPresent(educationsResultSet -> {
            educations.clear();
            try {
                while (educationsResultSet.next()) {
                    final int educationId = educationsResultSet.getInt("id");
                    educations.put(educationId,
                            new Education(
                                    educationId,
                                    educationsResultSet.getLong("discord_id"),
                                    educationsResultSet.getString("name")
                            )
                    );
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });

        connection.executeQuery("SELECT * FROM special_unit").ifPresent(specialUnitsResultSet -> {
            specialUnits.clear();
            try {
                while (specialUnitsResultSet.next()) {
                    final int specialUnitId = specialUnitsResultSet.getInt("id");
                    specialUnits.put(specialUnitId,
                            new SpecialUnit(
                                    specialUnitId,
                                    specialUnitsResultSet.getLong("discord_id"),
                                    specialUnitsResultSet.getString("name")
                            )
                    );
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });

        if (reloadPatrolCache) {
            connection.executeQuery("SELECT * FROM patrol_unit").ifPresent(patrolUnitsResultSet -> {
                patrolUnits.clear();
                try {
                    while (patrolUnitsResultSet.next()) {
                        final int patrolUnitId = patrolUnitsResultSet.getInt("id");
                        patrolUnits.put(patrolUnitId,
                                new PatrolUnit(
                                        patrolUnitId,
                                        patrolUnitsResultSet.getString("name")
                                )
                        );
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });

            connection.executeQuery("SELECT * FROM patrol_vehicle").ifPresent(patrolVehiclesResultSet -> {
                patrolVehicles.clear();
                try {
                    while (patrolVehiclesResultSet.next()) {
                        final int patrolVehicleId = patrolVehiclesResultSet.getInt("id");
                        patrolVehicles.put(patrolVehicleId,
                                new PatrolVehicle(
                                        patrolVehicleId,
                                        patrolVehiclesResultSet.getString("name")
                                )
                        );
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });

            connection.executeQuery("SELECT * FROM patrol_status").ifPresent(patrolStatusesResultSet -> {
                patrolStatuses.clear();
                try {
                    while (patrolStatusesResultSet.next()) {
                        final int patrolStatusId = patrolStatusesResultSet.getInt("id");
                        patrolStatuses.put(patrolStatusId,
                                new PatrolStatus(
                                        patrolStatusId,
                                        patrolStatusesResultSet.getString("name")
                                )
                        );
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public Optional<Rank> getRankForRole(final Role role) {
        return ranks.values().stream().filter(rank -> rank.getDiscordId().toString().equals(role.getId())).findFirst();
    }

    public Rank getHighestRank() {
        return ranks.values().stream().filter(rank -> rank.getId() == 1).findFirst().orElseThrow(() -> new RuntimeException("Could not get highest rank by id 1!"));
    }

    public Rank getLowestRank() {
        return getRankById(ranks.values().stream().mapToInt(Rank::getId).max().orElseThrow(() -> new RuntimeException("Could not find highest rank!")));
    }

    public Rank getNextHigherRank(final Rank rank) {
        return getRankById(ranks.values().stream().mapToInt(Rank::getId).filter(id -> id < rank.getId()).max().orElseThrow(() -> new RuntimeException("Could not find higher rank!")));
    }

    public Rank getNextLowerRank(final Rank rank) {
        return getRankById(ranks.values().stream().mapToInt(Rank::getId).filter(id -> id > rank.getId()).min().orElseThrow(() -> new RuntimeException("Could not find lower rank!")));
    }

    public Rank getRankById(final int rankId) {
        return ranks.get(rankId);
    }

    public Education getEducationById(final int educationId) {
        return educations.get(educationId);
    }

    public SpecialUnit getSpecialUnitById(final int specialUnitId) {
        return specialUnits.get(specialUnitId);
    }

    public PatrolUnit getPatrolUnitById(final int patrolUnitId) {
        return patrolUnits.get(patrolUnitId);
    }

    public PatrolVehicle getPatrolVehicleById(final int patrolVehicleId) {
        return patrolVehicles.get(patrolVehicleId);
    }

    public PatrolStatus getPatrolStatusById(final int patrolStatusId) {
        return patrolStatuses.get(patrolStatusId);
    }

    public int countEmployeesWithRank(final Rank rank) {
        final Optional<ResultSet> optionalEmployeeCountResult = connection.executeQuery("SELECT COUNT(*) FROM employee WHERE rank_id = ?", rank.getId().toString());
        if (optionalEmployeeCountResult.isPresent()) {
            final ResultSet employeeCountResult = optionalEmployeeCountResult.get();
            try {
                if (employeeCountResult.next()) {
                    return employeeCountResult.getInt(1);
                }
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("Could not find employees with rank " + rank.getName() + "!");
    }

    public int findNextFreeServiceNumber(final Rank rank) {
        final Optional<ResultSet> optionalEmployeeResult = connection.executeQuery("SELECT service_number FROM employee WHERE rank_id = ?", rank.getId().toString());
        if (!optionalEmployeeResult.isPresent()) {
            throw new RuntimeException("Could not get employees for rank " + rank.getName() + "!");
        }

        final ResultSet employeeResult = optionalEmployeeResult.get();
        final List<Integer> employeesWithRank = new ArrayList<>();
        try {
            while (employeeResult.next()) {
                employeesWithRank.add(employeeResult.getInt("service_number"));
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        if (employeesWithRank.size() >= rank.getMaxMembers()) {
            throw new RuntimeException("Cannot find free service number when rank has reached its max member count!");
        }

        final int startingServiceNumber = rank.getStartingServiceNumber();
        final int endingServiceNumber = startingServiceNumber + rank.getMaxMembers();
        for (int i = startingServiceNumber; i < endingServiceNumber; i++) {
            if (!(employeesWithRank.contains(i))) {
                return i;
            }
        }

        throw new RuntimeException("Cannot find free service number for rank " + rank.getName() + "!");
    }

    public boolean isRegistered(final String discordId) {
        final Optional<ResultSet> optionalEmployeeNameResult = connection.executeQuery("SELECT name FROM employee WHERE discord_id = ?", discordId);
        if (optionalEmployeeNameResult.isPresent()) {
            try {
                return optionalEmployeeNameResult.get().next();
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public boolean isRegistered(final Member member) {
        return isRegistered(member.getId());
    }

    public void registerEmployeeData(final Employee employee) {
        executor.submit(() -> {
            final int newId = connection.executeInsertWithReturnNewID("id", "INSERT INTO employee VALUES(0, ?, ?, ?, ?, ?, ?, ?, ?)",
                    employee.getServiceNumber().toString(),
                    employee.getName(),
                    employee.getDiscordId(),
                    employee.getRank().getId().toString(),
                    employee.getWarnings().toString(),
                    employee.getWorktime().toString(),
                    employee.getPhoneNumber(),
                    employee.getBirthDate()
            );
            employee.setId(newId);
        });
    }

    public void deleteEmployee(final Employee employee) {
        employeeCache.remove(employee.getDiscordId());
        //executor.execute(() -> {
        final String employeeId = employee.getId().toString();
        connection.executeQuery("DELETE FROM employee WHERE id = ?", employeeId);
        connection.executeQuery("DELETE FROM employee_education WHERE employee_id = ?", employeeId);
        connection.executeQuery("DELETE FROM employee_special_unit WHERE employee_id = ?", employeeId);
        //});
    }

    public void saveEmployeeData(final Employee employee) {
        executor.execute(() -> connection.executeQuery("UPDATE employee SET service_number = ?, rank_id = ?, warnings = ?, worktime = ?, phone_number = ?, birth_date = ? WHERE discord_id = ?",
                employee.getServiceNumber().toString(),
                employee.getRank().getId().toString(),
                employee.getWarnings().toString(),
                employee.getWorktime().toString(),
                employee.getPhoneNumber(),
                employee.getBirthDate(),
                employee.getDiscordId()
        ));
    }

    public void saveEmployeeEducations(final Employee employee) {
        executor.execute(() -> {
            connection.executeQuery("DELETE FROM employee_education WHERE employee_id = ?", employee.getId().toString());
            employee.getEducationList().forEach(education -> connection.executeQuery("INSERT INTO employee_education VALUES(?, ?)",
                    employee.getId().toString(),
                    education.getId().toString()
            ));
        });
    }

    public void saveEmployeeSpecialUnits(final Employee employee) {
        executor.execute(() -> {
            connection.executeQuery("DELETE FROM employee_special_unit WHERE employee_id = ?", employee.getId().toString());
            employee.getSpecialUnitList().forEach(specialUnit -> connection.executeQuery("INSERT INTO employee_special_unit VALUES(?, ?)",
                    employee.getId().toString(),
                    specialUnit.getId().toString()
            ));
        });
    }

    public void saveEmployee(final Employee employee) {
        saveEmployeeData(employee);
        saveEmployeeEducations(employee);
        saveEmployeeSpecialUnits(employee);
    }

    public Education[] getEducationsForEmployee(final Integer id) {
        final List<Education> educationList = new ArrayList<>();
        connection.executeQuery("SELECT education_id FROM employee_education WHERE employee_id = ?", id.toString()).ifPresent(educationsResultSet -> {
            try {
                while (educationsResultSet.next()) {
                    final int educationId = educationsResultSet.getInt("education_id");
                    educationList.add(getEducationById(educationId));
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
        return educationList.toArray(new Education[0]);
    }

    public SpecialUnit[] getSpecialUnitsForEmployee(final Integer id) {
        final List<SpecialUnit> specialUnitList = new ArrayList<>();
        connection.executeQuery("SELECT special_unit_id FROM employee_special_unit WHERE employee_id = ?", id.toString()).ifPresent(specialUnitsResultSet -> {
            try {
                while (specialUnitsResultSet.next()) {
                    final int specialUnitId = specialUnitsResultSet.getInt("special_unit_id");
                    specialUnitList.add(getSpecialUnitById(specialUnitId));
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
        return specialUnitList.toArray(new SpecialUnit[0]);
    }

    public List<String> findEmployeeDiscordIdsWithRank(final Rank rank) {
        final Optional<ResultSet> optionalEmployeeResult = connection.executeQuery("SELECT discord_id FROM employee WHERE rank_id = ?", rank.getId().toString());
        final List<String> discordIds = new ArrayList<>();
        if (optionalEmployeeResult.isPresent()) {
            final ResultSet employeeResult = optionalEmployeeResult.get();
            try {
                while (employeeResult.next()) {
                    discordIds.add(String.valueOf(employeeResult.getLong("discord_id")));
                }
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return discordIds;
    }

    public List<Employee> retrieveAllEmployees() {
        final Optional<ResultSet> optionalEmployeeResult = connection.executeQuery("SELECT * FROM employee");
        final List<Employee> employees = new ArrayList<>();
        if (optionalEmployeeResult.isPresent()) {
            final ResultSet employeeResult = optionalEmployeeResult.get();
            try {
                while (employeeResult.next()) {
                    final int id = employeeResult.getInt("id");
                    final Employee employee = new Employee(
                            id,
                            employeeResult.getInt("service_number"),
                            String.valueOf(employeeResult.getLong("discord_id")),
                            employeeResult.getString("name"),
                            getRankById(employeeResult.getInt("rank_id")),
                            employeeResult.getInt("warnings"),
                            employeeResult.getLong("worktime"),
                            employeeResult.getString("phone_number"),
                            employeeResult.getString("birth_date"),
                            getEducationsForEmployee(id),
                            getSpecialUnitsForEmployee(id)
                    );
                    employees.add(employee);
                }
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return employees;
    }

    public CompletableFuture<Optional<Employee>> findEmployeeByDiscordId(final String discordId) {
        if (employeeCache.containsKey(discordId)) {
            return CompletableFuture.completedFuture(Optional.of(employeeCache.get(discordId)));
        }
        final Optional<ResultSet> optionalEmployeeResult = connection.executeQuery("SELECT id, service_number, name, rank_id, warnings, worktime, phone_number, birth_date FROM employee WHERE discord_id = ?", discordId);
        return optionalEmployeeResult.map(resultSet -> CompletableFuture.supplyAsync(() -> {
            Employee employee = null;
            try {
                if (resultSet.next()) {
                    final int id = resultSet.getInt("id");
                    employee = new Employee(
                            id,
                            resultSet.getInt("service_number"),
                            discordId,
                            resultSet.getString("name"),
                            getRankById(resultSet.getInt("rank_id")),
                            resultSet.getInt("warnings"),
                            resultSet.getLong("worktime"),
                            resultSet.getString("phone_number"),
                            resultSet.getString("birth_date"),
                            getEducationsForEmployee(id),
                            getSpecialUnitsForEmployee(id)
                    );
                    employeeCache.put(discordId, employee);
                }
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
            return Optional.ofNullable(employee);
        }, executor)).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
    }

    public CompletableFuture<Optional<Employee>> findEmployeeByDiscordId(final long discordId) {
        return findEmployeeByDiscordId(String.valueOf(discordId));
    }

    public void removeEmployeeFromCache(final String discordId) {
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

    public List<PatrolUnit> getPatrolUnits() {
        return new ArrayList<>(patrolUnits.values());
    }

    public List<PatrolVehicle> getPatrolVehicles() {
        return new ArrayList<>(patrolVehicles.values());
    }

    public List<PatrolStatus> getPatrolStatuses() {
        return new ArrayList<>(patrolStatuses.values());
    }

    public static InformationGrabber forConnection(final MysqlConnection connection) {
        return new InformationGrabber(connection);
    }
}
