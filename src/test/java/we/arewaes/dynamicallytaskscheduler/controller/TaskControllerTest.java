package we.arewaes.dynamicallytaskscheduler.controller;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import we.arewaes.dynamicallytaskscheduler.domain.TaskRequest;
import we.arewaes.dynamicallytaskscheduler.service.TaskService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@WireMockTest(httpPort = 8080)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        doNothing().when(taskService).scheduleTask(any(TaskRequest.class));
        doNothing().when(taskService).cancelTask(anyString());
        doNothing().when(taskService).setTaskOnHold(anyString(), any(Boolean.class));
        doNothing().when(taskService).setTasOnHoldOnlyDb(anyString(), any(Boolean.class));
    }

    @Test
    void createTask_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/task/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"exampleTaskId\",\"cron\":\"0 0/5 14 * * *\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Task created successfully"));
    }

    @Test
    void holdTask_shouldReturnSuccess_whenOnlyDbIsTrue() throws Exception {
        mockMvc.perform(post("/task/hold")
                        .param("taskId", "exampleTaskId")
                        .param("onHold", "true")
                        .param("onlyDb", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("Task put on hold successfully"));
    }

    @Test
    void holdTask_shouldReturnSuccess_whenOnlyDbIsFalse() throws Exception {
        mockMvc.perform(post("/task/hold")
                        .param("taskId", "exampleTaskId")
                        .param("onHold", "true")
                        .param("onlyDb", "false"))
                .andExpect(status().isOk())
                .andExpect(content().string("Task put on hold successfully"));
    }

    @Test
    void deleteTask_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/task/delete")
                        .param("taskId", "exampleTaskId"))
                .andExpect(status().isOk())
                .andExpect(content().string("Task deleted successfully"));
    }
}