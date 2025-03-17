package we.arewaes.dynamicallytaskscheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DisplayName("Test Multiple Instances of Dynamically Task Scheduler Integration Test")
class MultiInstanceDynamicallyTaskSchedulerITTest implements BeforeAllCallback, AfterAllCallback {

    private static final String DOCKER_IMAGE_POSTGRES_17_ALPINE = "postgres:17-alpine";

    @Container
    private static final GenericContainer<?> postgreSQLContainer =
            new PostgreSQLContainer(DockerImageName.parse(DOCKER_IMAGE_POSTGRES_17_ALPINE))
                    .withDatabaseName("test")
                    .withUsername("sa")
                    .withPassword("pass")
                    .withDatabaseName("db")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(
                                    "database/init-schema.sql"),
                            "/docker-entrypoint-initdb.d/"
                    );

    private Path dockerfilePath;
    private String jdbc_url;

    @Override
    public void beforeAll(ExtensionContext context) {
        postgreSQLContainer.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        postgreSQLContainer.stop();
    }

    @BeforeEach
    void beforeEach() {
        dockerfilePath = Path.of(".");
        jdbc_url = ((PostgreSQLContainer<?>) postgreSQLContainer).getJdbcUrl()
                .replace("localhost", "host.docker.internal");
    }

    @Test
    @DisplayName("Test Multiple Instances of Dynamically Task Scheduler")
    void testMultipleInstancesExecutingTasks() throws InterruptedException {

        try (
                GenericContainer<?> instance1 = getSchedulerTaskServiceInstance("InstanceID-1", "8081");
                GenericContainer<?> instance2 = getSchedulerTaskServiceInstance("InstanceID-2", "8082");
                GenericContainer<?> instance3 = getSchedulerTaskServiceInstance("InstanceID-3", "8083");
                GenericContainer<?> instance4 = getSchedulerTaskServiceInstance("InstanceID-4", "8084")
        ) {
            instance1.start();
            instance2.start();

            // Execute curl command inside the container instances to create tasks
            String[] command = getCommand("http://localhost:8081/task/create",
                    "{\"taskId\":\"taskId-1\",\"cron\":\"0/5 * * * * *\"}");
            executeCommandOnContainer(instance1, command);

            String[] command2 = getCommand("http://localhost:8082/task/create",
                    "{\"taskId\":\"taskId-2\",\"cron\":\"0/5 * * * * *\"}");
            executeCommandOnContainer(instance2, command2);

            // Simulate instances being replaced.
            Thread.sleep(15000);
            List<String> logsInstance1 = Arrays.asList(instance1.getLogs().split("\\n"));
            instance1.stop();
            Thread.sleep(15000);
            instance3.start();
            Thread.sleep(5000);
            List<String> logsInstance2 = Arrays.asList(instance2.getLogs().split("\\n"));
            instance2.stop();
            Thread.sleep(15000);
            instance4.start();
            Thread.sleep(15000);
            List<String> logsInstance3 = Arrays.asList(instance3.getLogs().split("\\n"));
            List<String> logsInstance4 = Arrays.asList(instance4.getLogs().split("\\n"));

            // Get task execution logs
            List<String> allExecutions = Stream.of(logsInstance1, logsInstance2, logsInstance3, logsInstance4)
                    .flatMap(List::stream)
                    .filter(s -> s.matches("INSTANCE ID: .* -> Task .* started at .*"))
                    .sorted()
                    .toList();
            String taskId_1 = "taskId-1";
            String taskId_2 = "taskId-2";

            // Asserting execution multiple instances
            assertTrue(taskWasExecutedInMultipleInstances(allExecutions, taskId_1));
            assertTrue(taskWasExecutedInMultipleInstances(allExecutions, taskId_2));

            // Asserting no execution was duplicated
            assertFalse(logsHaveNoDuplicatedExecutions(allExecutions, taskId_1));
            assertFalse(logsHaveNoDuplicatedExecutions(allExecutions, taskId_2));

            // Asserting that the tasks were executed every 5 seconds
            assertTrue(assertExecutionEvery_5_seconds(allExecutions, taskId_1));
            assertTrue(assertExecutionEvery_5_seconds(allExecutions, taskId_2));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GenericContainer getSchedulerTaskServiceInstance(String instanceId, String port) {
        return new GenericContainer(
                new ImageFromDockerfile()
                        .withFileFromPath(".", dockerfilePath))
                .withEnv("INSTANCE_ID", instanceId)
                .withEnv("PORT", port)
                .withEnv("JDBC_URL", jdbc_url)
                .withExposedPorts(Integer.valueOf(port))
                .dependsOn(postgreSQLContainer);
    }

    private void executeCommandOnContainer(GenericContainer<?> instance, String[] command) throws IOException, InterruptedException {
        String result = instance.execInContainer(command).getStdout();
        System.out.printf("INSTANCE ID: %s - Command result: %s \n", instance.getEnvMap().get("INSTANCE_ID"), result);
    }

    private String[] getCommand(String url, String body) {
        return new String[]{"curl", "-X", "POST", url, "-H", "Content-Type: application/json", "-d", body};
    }

    private boolean taskWasExecutedInMultipleInstances(List<String> allExecutions, String taskId) {
        return allExecutions.stream()
                .filter(s -> s.contains(taskId))
                .map(s -> s.substring(0, 25))
                .collect(Collectors.toSet())
                .size() > 1;
    }

    private boolean logsHaveNoDuplicatedExecutions(List<String> allExecutions, String taskId) {
        List<String> assertionList = allExecutions.stream()
                .filter(s -> s.contains(taskId))
                .map(s -> s.substring(54, s.length() - 11))
                .sorted()
                .toList();

        return assertionList.stream().anyMatch(s -> assertionList.stream().filter(s::equals).count() > 1);
    }

    private boolean assertExecutionEvery_5_seconds(List<String> allExecutions, String taskId) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        List<LocalDateTime> dateTimes = allExecutions.stream()
                .filter(s -> s.contains(taskId))
                .map(s -> s.substring(54, s.length() - 11))
                .map(dateTime -> LocalDateTime.parse(dateTime, formatter))
                .sorted()
                .toList();

        for (int i = 1; i < dateTimes.size(); i++) {
            if (ChronoUnit.SECONDS.between(dateTimes.get(i - 1), dateTimes.get(i)) != 5) {
                return false;
            }
        }
        return true;
    }
}
