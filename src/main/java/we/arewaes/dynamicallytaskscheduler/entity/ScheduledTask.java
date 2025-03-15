package we.arewaes.dynamicallytaskscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "SCHEDULED_TASK")
@Data
public class ScheduledTask {

    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    @Column(name = "CRON", nullable = false)
    private String cron;

    @Column(name = "ON_HOLD", nullable = false, columnDefinition = "boolean")
    private boolean onHold;

}
