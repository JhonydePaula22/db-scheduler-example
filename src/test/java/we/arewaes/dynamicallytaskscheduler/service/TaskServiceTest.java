package we.arewaes.dynamicallytaskscheduler.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import we.arewaes.dynamicallytaskscheduler.domain.TaskRequest;
import we.arewaes.dynamicallytaskscheduler.entity.ScheduledTask;
import we.arewaes.dynamicallytaskscheduler.repository.ScheduledTaskRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskSchedulerService taskSchedulerService;
    @Mock
    private ScheduledTaskRepository scheduledTaskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTask_shouldCreateTaskSuccessfully() {
        String taskId = "newTaskId";
        String cronExpression = "0 0/5 14 * * *";
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTaskId(taskId);
        taskRequest.setCron(cronExpression);

        when(scheduledTaskRepository.save(any())).thenReturn(new ScheduledTask());
        doNothing().when(taskSchedulerService).scheduleTaskExecution(taskId, cronExpression);

        taskService.scheduleTask(taskRequest);

        verify(scheduledTaskRepository).save(any());
        verify(taskSchedulerService).scheduleTaskExecution(taskId, cronExpression);
    }

    @Test
    void holdTask_shouldPutTaskOnHoldSuccessfully() {
        String taskId = "taskToHold";
        when(scheduledTaskRepository.findById(taskId)).thenReturn(Optional.of(new ScheduledTask()));
        when(scheduledTaskRepository.save(any())).thenReturn(new ScheduledTask());

        taskService.setTaskOnHold(taskId, true);

        verify(scheduledTaskRepository).findById(taskId);
        verify(scheduledTaskRepository).save(any());
    }

    @Test
    void holdTask_shouldFailToPutTaskOnHold_whenTaskDoesNotExist() {
        String taskId = "nonExistentTaskId";
        when(scheduledTaskRepository.findById(taskId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> taskService.setTaskOnHold(taskId, true));

        assertEquals("Task not found", exception.getMessage());
        verify(scheduledTaskRepository).findById(taskId);
        verify(scheduledTaskRepository, never()).save(any());
    }

    @Test
    void deleteTask_shouldDeleteTaskSuccessfully() {
        String taskId = "taskToDelete";

        doNothing().when(scheduledTaskRepository).deleteById(taskId);
        doNothing().when(taskSchedulerService).cancelTaskExecution(taskId);

        taskService.cancelTask(taskId);

        verify(scheduledTaskRepository).deleteById(taskId);
        verify(taskSchedulerService).cancelTaskExecution(taskId);
    }
}