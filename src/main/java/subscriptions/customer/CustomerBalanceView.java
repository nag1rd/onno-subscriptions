package subscriptions.customer;

import org.springframework.stereotype.Component;

import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

@Component
public class CustomerBalanceView implements EntityView<CustomerBalance> {
    @Override
    public Class<CustomerBalance> entity() {
        return CustomerBalance.class;
    }

    @Override
    public void list(ListSpec<CustomerBalance> list) {
        list.column(CustomerBalance::getCustomer, "Клиент");
        list.column(CustomerBalance::getAmount, "Сумма");
    }

    @Override
    public void fields(EntityConfigBuilder<CustomerBalance> fields) {
        fields.field(CustomerBalance::getCustomer)
            .order(10)
            .hint("Клиент, которому принадлежит лицевой счёт");

        fields.field(CustomerBalance::getAmount)
            .order(20)
            .format("currency:RUB")
            .hint("Остаток денег на лицевом счёте");
    }
}
