# Dynamically Scheduling Recurring Tasks in a Multi-Instance Environment

## Context

In my team, I faced a challenge: how to dynamically schedule recurring tasks in a cloud-based, multi-instance
environment.

Previously, we used an on-premises infrastructure where recurring tasks were scheduled using Spring's `TaskScheduler`.
This interface allows scheduling tasks with fixed-rate, fixed-delay, or cron-like expressions. You can read more about
it [here](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-task-scheduler).

The tasks were dynamically generated in response to events, and each task executed at its own time based on a cron
schedule. This setup worked perfectly in a single-instance, on-prem environment. However, moving to the cloud introduced
new challenges:

- **No duplicate task execution**: Tasks must not run on multiple instances simultaneously.
- **Failover support**: If one instance shuts down, other instances should pick up its tasks.
- **Recovery after downtime**: If all instances go down, tasks should resume when the service recovers.
- **Task management**: Tasks should support updates, pauses, and deletions.

Meeting these requirements in a cloud environment is no small feat.

---

## `db-scheduler`

After some research, I discovered a library called [`db-scheduler`](https://github.com/kagkarlsson/db-scheduler).
According to its documentation, it is a "Task-scheduler for Java inspired by the need for a clustered
`java.util.concurrent.ScheduledExecutorService`."

### Key Features

- **Cluster-friendly**: Guarantees execution by a single scheduler instance. (Nice!)
- **Persistent tasks**: Uses a single database table for persistence. (Wow!)
- **Simple**: Easy to use. (I'm in love.)
- And more! Check out the full list of
  features [here](https://github.com/kagkarlsson/db-scheduler?tab=readme-ov-file#features).

### What Can It Do?

`db-scheduler` supports:

- **Recurring static tasks**: For example, sending daily emails with recent transactions to customers.
- **One-time tasks**: Useful for tasks that need to run at application startup.
- **Batch tasks**: Schedule multiple tasks at once.

But how does it help with my specific problem?

---

## How `db-scheduler` Solved My Problem

### Addressing the Challenges

1. **No duplicate execution**: `db-scheduler` ensures that tasks are executed by only one instance at a time.
2. **Failover support**: Tasks are persisted in a database, so new instances can pick up where others left off.
3. **Recovery after downtime**: The database acts as a single source of truth, enabling recovery after all instances go
   down.

What remained was the ability to dynamically schedule, update, delete, and pause tasks. Here's how I implemented it.

---

## Implementation

### Adding the Dependency

First, I added the `db-scheduler` dependency to my project:

```xml

<dependency>
    <groupId>com.github.kagkarlsson</groupId>
    <artifactId>db-scheduler-spring-boot-starter</artifactId>
    <version>15.1.1</version>
</dependency>
```

### Database Setup

I used the scripts provided in
the [documentation](https://github.com/kagkarlsson/db-scheduler/blob/master/db-scheduler/src/test/resources/postgresql_tables.sql)
to create the necessary database table.

---

### Configuration

#### Scheduler Bean

The `Scheduler` bean is the core of the setup. It handles task scheduling and execution.

```java
public static final String DYNAMIC_RECURRING_TASK_NAME = "dynamic-recurring-task";
public static final TaskDescriptor<ScheduleAndNoData> DYNAMIC_RECURRING_TASK =
        TaskDescriptor.of(DYNAMIC_RECURRING_TASK_NAME, ScheduleAndNoData.class);

@Bean
public Scheduler scheduler(DataSource dataSource, TaskExecutorService taskExecutorService, ScheduledTaskRepository scheduledTaskRepository) {
    final RecurringTaskWithPersistentSchedule<ScheduleAndNoData> dynamicRecurringTask =
            Tasks.recurringWithPersistentSchedule(DYNAMIC_RECURRING_TASK)
                    .execute((taskInstance, executionContext) -> taskExecutorService.startTaskExecution(taskInstance.getId()));

    Scheduler scheduler = Scheduler.create(dataSource, dynamicRecurringTask)
            .pollingInterval(Duration.ofSeconds(1))
            .addExecutionInterceptor(new TaskExecutionCustomInterceptor(scheduledTaskRepository))
            .registerShutdownHook()
            .build();

    scheduler.start();
    return scheduler;
}
```

---

#### Task Descriptor

The `TaskDescriptor` defines the characteristics of a task, such as its name and associated data type.

```java
public static final String DYNAMIC_RECURRING_TASK_NAME = "dynamic-recurring-task";
public static final TaskDescriptor<ScheduleAndNoData> DYNAMIC_RECURRING_TASK =
        TaskDescriptor.of(DYNAMIC_RECURRING_TASK_NAME, ScheduleAndNoData.class);
```

---

#### Data Object

The `ScheduleAndNoData` class implements the `ScheduleAndData` interface. It includes the task's schedule and an
`isOnHold` flag to determine whether the task should execute.

```java

@Getter
@RequiredArgsConstructor
public static class ScheduleAndNoData implements ScheduleAndData, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final CronSchedule schedule;
    private final boolean isOnHold;

    @Override
    public Object getData() {
        return null;
    }
}
```

---

#### Execution Interceptor

The `TaskExecutionCustomInterceptor` intercepts task execution to handle tasks that are on hold.
Here I also used the different types of `CompletitionHandler` that the lib offers. In summary you can:
Use `OnCompleteReschedule` for recurring tasks that are on hold, so they are not executed at this time.
Use `OnCompleteReplace` when you need to update the task's state or schedule.

```java

@RequiredArgsConstructor
@Slf4j
static class TaskExecutionCustomInterceptor implements ExecutionInterceptor {
    private final ScheduledTaskRepository scheduledTaskRepository;

    private static boolean isDynamicRecurringTask(TaskInstance<?> taskInstance) {
        return taskInstance.getTaskName().equals(DYNAMIC_RECURRING_TASK_NAME) &&
                taskInstance.getData() instanceof ScheduleAndNoData;
    }

    @Override
    public CompletionHandler<?> execute(TaskInstance<?> taskInstance, ExecutionContext executionContext, ExecutionChain executionChain) {
        log.info("Custom interceptor is being executed");
        if (isDynamicRecurringTask(taskInstance)) {
            TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule = (TaskInstance<ScheduleAndNoData>) taskInstance;
            if (isOnHold(taskInstanceWithSchedule)) {
                return handleOnHold(taskInstanceWithSchedule);
            }

            if (!isOnHold(taskInstanceWithSchedule) && isOnHoldDb(taskInstanceWithSchedule)) {
                return handleOnHoldNotInSyncWithDb("Task is on hold on the DB. Will be replaced", true, taskInstanceWithSchedule);
            }
        }
        log.info("Proceeding with the task execution");
        return executionChain.proceed(taskInstance, executionContext);
    }

    private CompletionHandler.OnCompleteReplace<ScheduleAndNoData> handleOnHoldNotInSyncWithDb(String log, boolean isOnHold, TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
        TaskExecutionCustomInterceptor.log.info(log);
        ScheduleAndNoData newData = new ScheduleAndNoData(taskInstanceWithSchedule.getData().getSchedule(), isOnHold);
        return new CompletionHandler.OnCompleteReplace<>(taskInstanceWithSchedule.getTaskName(), newData);
    }

    private CompletionHandler<?> handleOnHold(TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
        if (isOnHoldDb(taskInstanceWithSchedule)) {
            return rescheduleTaskExecutor(taskInstanceWithSchedule);
        }
        return handleOnHoldNotInSyncWithDb("Task was on hold. Will be replaced", false, taskInstanceWithSchedule);
    }

    private CompletionHandler.OnCompleteReschedule<Object> rescheduleTaskExecutor(TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
        log.info("Task is on hold. Will be rescheduled");
        return new CompletionHandler.OnCompleteReschedule<>(taskInstanceWithSchedule.getData().getSchedule());
    }

    private boolean isOnHold(TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
        return taskInstanceWithSchedule.getData().isOnHold();
    }

    private boolean isOnHoldDb(TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
        return scheduledTaskRepository.findByIdAndOnHoldTrue(taskInstanceWithSchedule.getId()).isPresent();
    }
}
```

---

### Dynamic Task Scheduling

The `TaskSchedulerService` handles dynamic scheduling, updating, and canceling of tasks.

```java

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskSchedulerService {
    private final Scheduler scheduler;

    public void scheduleTaskExecution(String taskId, String trigger) {
        log.info("Schedule task with taskId: {}", taskId);
        this.scheduler.scheduleIfNotExists(DYNAMIC_RECURRING_TASK
                .instance(taskId)
                .data(new SchedulerConfiguration.ScheduleAndNoData(new CronSchedule(trigger), false))
                .scheduledAccordingToData());
    }

    public void cancelTaskExecution(String taskId) {
        log.info("Cancel task with taskId: {}", taskId);
        try {
            this.scheduler.cancel(new TaskInstance<>(DYNAMIC_RECURRING_TASK_NAME, taskId));
        } catch (TaskInstanceNotFoundException e) {
            log.warn("Task not found with taskId: {}", taskId);
        } catch (TaskInstanceCurrentlyExecutingException e) {
            log.warn("Task with taskId {} is currently executing. Will be cancelled after execution.", taskId);
        }
        // in both exceptions, you can choose how you handle according to the project needs.
        // let's assume that after this succeeds, the task data is deleted also on the repository
    }

    public void updateTaskExecution(String taskId, String trigger, boolean onHold) {
        log.info("Update task with taskId: {}", taskId);
        this.cancelTaskExecution(taskId);
        this.scheduler.scheduleIfNotExists(DYNAMIC_RECURRING_TASK
                .instance(taskId)
                .data(new SchedulerConfiguration.ScheduleAndNoData(new CronSchedule(trigger), onHold))
                .scheduledAccordingToData());
    }
}
```

### Testing the service:

As a test, I created a simple controller to schedule, cancel, and update tasks.
So I created an integration test that spins up 2 instances, then schedules two tasks that run every 5 seconds.
Then I stop instance 1 and create an instance 3. And finally, stop instance 2 and create an instance 4.
The test ensures that the tasks are not executed by more than one instance at a time and ensures that the tasks are run
every 5 seconds.
You can check the
test [here](src/test/java/we/arewaes/dynamicallytaskscheduler/MultiInstanceDynamicallyTaskSchedulerITTest.java)

---

## Conclusion

Using `db-scheduler`, I successfully implemented dynamic task scheduling in a multi-instance environment. The solution
ensures no duplicate execution, supports failover, and allows tasks to be updated, paused, or deleted.

You can find the complete code in this [GitHub repository](https://github.com/JhonydePaula22/db-scheduler-example).