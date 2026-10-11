package subscriptions.ui;
import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import su.onno.ui.Page;
import su.onno.ui.PageBuilder;
import subscriptions.customer.Customer;
import subscriptions.customer.CustomerBalance;
import subscriptions.payment.Payment;
import subscriptions.subscription.Subscription;
import subscriptions.subscription.TariffRevenue;
@Component
public class DashboardPage implements Page {
    private final Clock clock;
    public DashboardPage(Clock clock) { this.clock = clock; }
    @Override public String route() { return "/"; }
    @Override public void compose(PageBuilder page) {
        LocalDate from = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate to = from.plusMonths(1);
        String period = "_period >= '" + from + " 00:00:00' AND _period < '" + to + " 00:00:00'";
        page.title("Обзор подписок").subtitle("Выручка за текущий календарный месяц: " + from + " — " + to.minusDays(1));
        page.widget("Клиентов").type("count").catalog(Customer.class).width("1/3");
        page.widget("Остатки на счетах").type("metric").register(CustomerBalance.class)
            .metricField(CustomerBalance::getAmount).config("metric", "sum").config("currency", "RUB").config("locale", "ru-RU").width("1/3")
            .hint("Текущая сумма средств всех клиентов");
        page.widget("Выручка за месяц").type("metric").register(TariffRevenue.class)
            .metricField(TariffRevenue::getAmount).config("metric", "sum").config("currency", "RUB").config("locale", "ru-RU")
            .config("filter", period).width("1/3");
        page.chart("Выручка по тарифам за всё время", TariffRevenue.class).category(TariffRevenue::getTariff)
            .sum(TariffRevenue::getAmount).bar().currency("RUB").axis(su.onno.ui.ChartBuilder.Axis.LEFT, axis -> axis.minimum(0).visible(false)).dataLabels(su.onno.ui.ChartBuilder.DataLabels.ALL).config("groupBy", "tariff_code").width("1/2")
            .hint("Коды тарифов; названия и произвольный период доступны в отчёте выручки");
        page.chart("Выручка по клиентам за всё время", TariffRevenue.class).category(TariffRevenue::getCustomer)
            .sum(TariffRevenue::getAmount).bar().currency("RUB").axis(su.onno.ui.ChartBuilder.Axis.LEFT, axis -> axis.minimum(0).visible(false)).dataLabels(su.onno.ui.ChartBuilder.DataLabels.ALL).config("groupBy", "customer_code").width("1/2")
            .hint("Коды клиентов позволяют различать людей с одинаковыми именами");
        page.widget("Последние платежи").type("list").document(Payment.class)
            .dateField(Payment::getDate).titleField(Payment::getNumber).maxItems(5).width("1/2");
        page.widget("Последние подписки").type("list").document(Subscription.class)
            .dateField(Subscription::getDate).titleField(Subscription::getNumber).maxItems(5).width("1/2");
        page.text("В разделе «Выручка по тарифам» можно выбрать произвольный период отчёта. Дата окончания подписки исключительная: в этот день доступ уже истёк.");
    }
}
