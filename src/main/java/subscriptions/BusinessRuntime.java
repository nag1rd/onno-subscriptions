package subscriptions;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import su.onno.types.Ref;
import su.onno.types.RefResolver;

@Component
public final class BusinessRuntime {
    private static volatile BusinessRuntime instance;
    private final RefResolver resolver;
    private final Clock clock;
    public BusinessRuntime(RefResolver resolver, Clock clock) {
        this.resolver = resolver;
        this.clock = clock;
        instance = this;
    }
    public static BusinessRuntime get() {
        if (instance == null) throw new IllegalStateException("Business runtime is not initialized");
        return instance;
    }
    public LocalDate today() { return LocalDate.now(clock); }
    public LocalDateTime now() { return LocalDateTime.now(clock); }
    public <T> Optional<T> find(Class<T> type, UUID id) {
        return id == null ? Optional.empty() : resolver.resolve(new Ref<>(type, id));
    }
    public Tariff tariff(Ref<Tariff> ref) {
        if (ref == null) throw new IllegalArgumentException("Укажите тариф в каждой строке");
        return resolver.resolve(ref).filter(t -> !t.isDeletionMark())
            .orElseThrow(() -> new IllegalArgumentException("Тариф не найден или удалён"));
    }
}
