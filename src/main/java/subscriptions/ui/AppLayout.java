package subscriptions.ui;

import org.springframework.stereotype.Component;

import su.onno.ui.Layout;
import su.onno.ui.LayoutSpec;
import subscriptions.Tariff;
import subscriptions.customer.Customer;
import subscriptions.customer.CustomerBalance;
import subscriptions.payment.Payment;

@Component
public class AppLayout implements Layout {

    @Override
    public void configure(LayoutSpec layout) {
        layout.title("Сервис подписок");
        layout.shell().nav(su.onno.ui.NavStyle.SIDEBAR);

        layout.section("Обзор").page("/", "Дашборд", "chart-column");

        layout.section("Справочники")
            .catalog(Customer.class)
            .catalog(Tariff.class);
        layout.section("Отчёты")
            .register(CustomerBalance.class)
            .register(subscriptions.subscription.TariffRevenue.class);
        layout.section("Документы")
            .document(Payment.class)
            .document(subscriptions.subscription.Subscription.class);
    }
}