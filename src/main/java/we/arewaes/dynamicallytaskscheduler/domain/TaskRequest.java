package we.arewaes.dynamicallytaskscheduler.domain;

import lombok.Data;

@Data
public class TaskRequest {

    private String taskId;
    private String cron;
}
