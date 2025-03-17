package we.arewaes.dynamicallytaskscheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import we.arewaes.dynamicallytaskscheduler.domain.TaskRequest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DisplayName("Integration Test for Dynamically Task Scheduler")
public class DynamicallyTaskSchedulerITTest implements BeforeAllCallback, AfterAllCallback {

    private static final String DOCKER_IMAGE_POSTGRES_17_ALPINE = "postgres:17-alpine";

    @Container
    private static GenericContainer<?> postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse(DOCKER_IMAGE_POSTGRES_17_ALPINE))
            .withDatabaseName("test")
            .withUsername("sa")
            .withPassword("pass")
            .withDatabaseName("db")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("database/init-schema.sql"), "/docker-entrypoint-initdb.d/"
            );

    private static ObjectMapper objectMapper;

    @Override
    public void beforeAll(ExtensionContext context) {
        postgreSQLContainer.start();
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> ((PostgreSQLContainer<?>) postgreSQLContainer).getJdbcUrl());
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "pass");
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        postgreSQLContainer.stop();
    }

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8088;
    }

    @Test
    @DisplayName("Create Task should return success")
    void createTask_shouldReturnSuccess() throws JsonProcessingException {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTaskId("exampleTaskId");
        taskRequest.setCron("0/10 * * * * *");
        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(taskRequest))
                .when()
                .post("/task/create")
                .then()
                .statusCode(200)
                .body(equalTo("Task created successfully"));
    }

    @Test
    @DisplayName("On Hold Task should return success when only DB is true")
    void holdTask_shouldReturnSuccess_whenOnlyDbIsTrue() throws JsonProcessingException {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTaskId("exampleTaskId-2");
        taskRequest.setCron("0/10 * * * * *");
        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(taskRequest))
                .when()
                .post("/task/create")
                .then()
                .statusCode(200)
                .body(equalTo("Task created successfully"));

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(taskRequest))
                .when()
                .post("/task/hold?taskId=exampleTaskId-2&onHold=true&onlyDb=true")
                .then()
                .statusCode(200)
                .body(equalTo("Task put on hold successfully"));
    }

    @Test
    @DisplayName("On Hold Task should return success when only DB is false")
    void holdTask_shouldReturnSuccess_whenOnlyDbIsFalse() throws JsonProcessingException {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTaskId("exampleTaskId-3");
        taskRequest.setCron("0/10 * * * * *");
        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(taskRequest))
                .when()
                .post("/task/create")
                .then()
                .statusCode(200)
                .body(equalTo("Task created successfully"));

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(taskRequest))
                .when()
                .post("/task/hold?taskId=exampleTaskId-3&onHold=true&onlyDb=false")
                .then()
                .statusCode(200)
                .body(equalTo("Task put on hold successfully"));
    }

    @Test
    @DisplayName("Delete Task should return success")
    void deleteTask_shouldReturnSuccess() throws JsonProcessingException {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTaskId("exampleTaskId-4");
        taskRequest.setCron("0/10 * * * * *");
        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(taskRequest))
                .when()
                .post("/task/create")
                .then()
                .statusCode(200)
                .body(equalTo("Task created successfully"));

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(taskRequest))
                .when()
                .delete("/task/delete?taskId=exampleTaskId-4")
                .then()
                .statusCode(200)
                .body(equalTo("Task deleted successfully"));
    }
}
