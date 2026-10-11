package subscriptions;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class BusinessConfiguration {
    @Bean
    public Clock businessClock(@Value("${app.time-zone:Europe/Moscow}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
