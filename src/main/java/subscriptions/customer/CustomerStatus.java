package subscriptions.customer;

import su.onno.annotations.EnumLabel;
import su.onno.annotations.Enumeration;

@Enumeration(
    name = "CustomerStatuses",
    title = "Статусы клиентов"
)
public enum CustomerStatus {

    @EnumLabel("Активен")
    ACTIVE,

    @EnumLabel("Заблокирован")
    BLOCKED
}