package subscriptions.subscription;
import su.onno.annotations.Enumeration;
import su.onno.annotations.EnumLabel;
@Enumeration(name = "SubscriptionStatuses", title = "Статусы подписок")
public enum SubscriptionStatus {
    @EnumLabel("Черновик") DRAFT,
    @EnumLabel("Ожидает начала") WAITING,
    @EnumLabel("Активна") ACTIVE,
    @EnumLabel("Истекла") EXPIRED,
    @EnumLabel("Отменена") CANCELLED
}
