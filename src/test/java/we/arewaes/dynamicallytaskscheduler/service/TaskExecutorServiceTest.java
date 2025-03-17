package we.arewaes.dynamicallytaskscheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskExecutorServiceTest {

    private static final String INSTANCE_ID = "testInstanceId";
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private TaskExecutorService taskExecutorService;

    @BeforeEach
    void setUp() {
        taskExecutorService = new TaskExecutorService(INSTANCE_ID);
        System.setOut(new PrintStream(outContent));
    }

    @Test
    @DisplayName("Test start task execution and print start message contains correct message")
    void startTaskExecution_shouldPrintStartMessage() {
        String taskId = "testTaskId";
        LocalDateTime beforeExecution = LocalDateTime.now();

        taskExecutorService.startTaskExecution(taskId);

        String expectedMessage = String.format("INSTANCE ID: %s -> Task %s started at ", INSTANCE_ID, taskId);
        String actualMessage = outContent.toString();
        assertTrue(actualMessage.contains(expectedMessage));
        assertTrue(actualMessage.contains(beforeExecution.toLocalDate().toString()));
    }

}