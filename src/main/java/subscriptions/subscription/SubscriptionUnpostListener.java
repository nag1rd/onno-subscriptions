package subscriptions.subscription;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import su.onno.posting.DocumentUnpostedEvent;

@Component
public class SubscriptionUnpostListener {
    private final SubscriptionRepository subscriptions;
    public SubscriptionUnpostListener(SubscriptionRepository subscriptions) { this.subscriptions = subscriptions; }
    @EventListener public void unposted(DocumentUnpostedEvent event) {
        if (!(event.document() instanceof Subscription document)) return;
        var latest = subscriptions.findActiveById(document.getId()).orElse(null);
        if (latest == null || latest.isPosted() || latest.getStatus() == SubscriptionStatus.CANCELLED) return;
        latest.setStatus(SubscriptionStatus.DRAFT);
        try { subscriptions.save(latest); }
        catch (RuntimeException e) {
            // The regular job converges this non-financial status if an edit won the race.
            LoggerFactory.getLogger(getClass()).warn("Deferred status refresh for {}", latest.getNumber(), e);
        }
    }
}
