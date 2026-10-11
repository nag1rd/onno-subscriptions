package subscriptions.subscription;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import su.onno.annotations.ScheduledJob;
import su.onno.jobs.BackgroundTask;
@Component
@ScheduledJob(name = "subscription-statuses", cron = "* * * * *")
public class SubscriptionStatusJob implements BackgroundTask {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionStatusJob.class);
    private final SubscriptionRepository subscriptions;
    private final Clock clock;
    public SubscriptionStatusJob(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions; this.clock = clock;
    }
    @Override public void execute() {
        int changed = 0;
        LocalDate today = LocalDate.now(clock);
        for (var doc : subscriptions.findAllActive()) {
            if (doc.getStatus() == SubscriptionStatus.CANCELLED) continue;
            var next = doc.isPosted() ? SubscriptionCalculator.status(today, doc.getStartDate(), doc.getEndDate()) : SubscriptionStatus.DRAFT;
            if (doc.getStatus() != next) {
                doc.setStatus(next);
                try { subscriptions.save(doc); changed++; }
                catch (org.springframework.dao.OptimisticLockingFailureException e) {
                    log.info("Status update deferred after concurrent edit: {}", doc.getNumber());
                }
            }
        }
        if (changed > 0) log.info("Updated subscription statuses: {}", changed);
    }
}
