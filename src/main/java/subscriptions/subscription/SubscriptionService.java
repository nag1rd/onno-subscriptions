package subscriptions.subscription;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import su.onno.events.EntityChangedEvent;
import su.onno.posting.PostingService;

@Service
public class SubscriptionService {
    private final SubscriptionRepository subscriptions;
    private final PostingService posting;
    private final ApplicationEventPublisher events;

    public SubscriptionService(SubscriptionRepository subscriptions, PostingService posting,
                               ApplicationEventPublisher events) {
        this.subscriptions = subscriptions;
        this.posting = posting;
        this.events = events;
    }

    public void cancel(UUID id, String reason) {
        var document = subscriptions.findActiveById(id)
            .orElseThrow(() -> new IllegalArgumentException("Подписка не найдена"));
        if (document.getStatus() == SubscriptionStatus.CANCELLED) return;
        document.cancel(reason);
        if (document.isPosted()) {
            posting.repost(document);
        } else {
            subscriptions.save(document);
        }
        events.publishEvent(new EntityChangedEvent(EntityChangedEvent.UPDATED,
            EntityChangedEvent.DOCUMENT, "Subscriptions", document.getId(), document.getNumber()));
        events.publishEvent(new EntityChangedEvent(EntityChangedEvent.CHANGED,
            EntityChangedEvent.REGISTER, "*", document.getId(), null));
    }
}
