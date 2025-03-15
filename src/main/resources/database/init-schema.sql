create table if not exists scheduled_tasks
(
    task_name
    text
    not
    null,
    task_instance
    text
    not
    null,
    task_data
    bytea,
    execution_time
    timestamp
    with
    time
    zone
    not
    null,
    picked
    BOOLEAN
    not
    null,
    picked_by
    text,
    last_success
    timestamp
    with
    time
    zone,
    last_failure
    timestamp
    with
    time
    zone,
    consecutive_failures
    INT,
    last_heartbeat
    timestamp
    with
    time
    zone,
    version
    BIGINT
    not
    null,
    priority
    SMALLINT,
    PRIMARY
    KEY
(
    task_name,
    task_instance
)
    );

CREATE INDEX IF NOT EXISTS execution_time_idx ON scheduled_tasks (execution_time);
CREATE INDEX IF NOT EXISTS last_heartbeat_idx ON scheduled_tasks (last_heartbeat);
CREATE INDEX IF NOT EXISTS priority_execution_time_idx on scheduled_tasks (priority desc, execution_time asc);

CREATE TABLE IF NOT EXISTS SCHEDULED_TASK
(
    ID
    VARCHAR
(
    36
) NOT NULL PRIMARY KEY,
    ON_HOLD BOOLEAN,
    CRON VARCHAR
(
    80
) NOT NULL
    );