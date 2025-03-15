package we.arewaes.dynamicallytaskscheduler.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import we.arewaes.dynamicallytaskscheduler.domain.TaskRequest;
import we.arewaes.dynamicallytaskscheduler.service.TaskService;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/create")
    public ResponseEntity<String> createTask(@RequestBody TaskRequest taskRequest) {
        taskService.scheduleTask(taskRequest);
        return ResponseEntity.ok("Task created successfully");
    }

    @PostMapping("/hold")
    public ResponseEntity<String> holdTask(@RequestParam String taskId, @RequestParam boolean onHold, @RequestParam boolean onlyDb) {
        if (onlyDb) {
            taskService.setTasOnHoldOnlyDb(taskId, onHold);
            return ResponseEntity.ok("Task put on hold successfully");
        }

        taskService.setTaskOnHold(taskId, onHold);
        return ResponseEntity.ok("Task put on hold successfully");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteTask(@RequestParam String taskId) {
        taskService.cancelTask(taskId);
        return ResponseEntity.ok("Task deleted successfully");
    }
}
