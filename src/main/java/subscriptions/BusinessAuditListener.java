package subscriptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import su.onno.posting.DocumentPostedEvent;
import su.onno.posting.DocumentUnpostedEvent;

@Component
public class BusinessAuditListener {
    private static final Logger log = LoggerFactory.getLogger(BusinessAuditListener.class);
    @EventListener public void posted(DocumentPostedEvent event) {
        var doc = event.document();
        log.info("Document posted/reposted: type={}, number={}, id={}", doc.getClass().getSimpleName(), doc.getNumber(), doc.getId());
    }
    @EventListener public void unposted(DocumentUnpostedEvent event) {
        var doc = event.document();
        log.info("Document unposted: type={}, number={}, id={}", doc.getClass().getSimpleName(), doc.getNumber(), doc.getId());
    }
}
