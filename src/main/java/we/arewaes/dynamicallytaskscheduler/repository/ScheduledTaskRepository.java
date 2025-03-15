package we.arewaes.dynamicallytaskscheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import we.arewaes.dynamicallytaskscheduler.entity.ScheduledTask;

import java.util.Optional;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, String> {
    Optional<ScheduledTask> findByIdAndOnHoldTrue(String id);
}
