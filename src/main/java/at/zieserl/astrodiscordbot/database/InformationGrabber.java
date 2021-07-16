package at.zieserl.astrodiscordbot.database;

import at.zieserl.astrodiscordbot.employee.Education;
import at.zieserl.astrodiscordbot.employee.Employee;
import at.zieserl.astrodiscordbot.employee.Rank;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class InformationGrabber {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final MysqlConnection connection;
    private final Map<Integer, Rank> ranks = new HashMap<>();
    private final Map<Integer, Education> educations = new HashMap<>();

    private InformationGrabber(MysqlConnection connection) {
        this.connection = connection;
    }

    public void reloadConstantsCache() {
        connection.executeQuery("SELECT id, name FROM rank").ifPresent(ranksResultSet -> {
            ranks.clear();
            try {
                while (ranksResultSet.next()) {
                    ranks.put(ranksResultSet.getInt("id"), new Rank(ranksResultSet.getString("name")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        connection.executeQuery("SELECT id, name FROM education").ifPresent(educationsResultSet -> {
            educations.clear();
            try {
                while (educationsResultSet.next()) {
                    educations.put(educationsResultSet.getInt("id"), new Education(educationsResultSet.getString("name")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Rank getRankById(int rankId) {
        return ranks.get(rankId);
    }

    public Education getEducationById(int educationId) {
        return educations.get(educationId);
    }

    public Education[] getEducationsForEmployee(int serviceNumber) {
        List<Education> educationList = new ArrayList<>();
        connection.executeQuery("SELECT education_id FROM employee_education WHERE service_number = ?", String.valueOf(serviceNumber)).ifPresent(educationsResultSet -> {
            try {
                while (educationsResultSet.next()) {
                    int educationId = educationsResultSet.getInt("education_id");
                    educationList.add(getEducationById(educationId));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return educationList.toArray(new Education[0]);
    }

    public CompletableFuture<Optional<Employee>> findEmployeeByDiscordId(String discordId) {
        Optional<ResultSet> optionalEmployeeResult = connection.executeQuery("SELECT service_number, name, rank_id, warnings, worktime FROM employee WHERE discord_id = ?", discordId);
        if (optionalEmployeeResult.isPresent()) {
            return CompletableFuture.supplyAsync(() -> {
                ResultSet employeeResult = optionalEmployeeResult.get();
                Employee employee;
                try {
                    if (employeeResult.next()) {
                        int serviceNumber = employeeResult.getInt("service_number");
                        employee = new Employee(
                                serviceNumber,
                                employeeResult.getString("name"),
                                getRankById(employeeResult.getInt("rank_id")),
                                employeeResult.getInt("warnings"),
                                employeeResult.getInt("worktime"),
                                getEducationsForEmployee(serviceNumber)
                        );
                    } else {
                        throw new RuntimeException("Employee result set had no values in it!");
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return Optional.of(employee);
            }, executor);
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public CompletableFuture<Optional<Employee>> findEmployeeByDiscordId(long discordId) {
        return findEmployeeByDiscordId(String.valueOf(discordId));
    }

    public static InformationGrabber forConnection(MysqlConnection connection) {
        return new InformationGrabber(connection);
    }
}
