package subscriptions.subscription;
import org.springframework.stereotype.Component;
import su.onno.ui.*;
@Component
public class TariffRevenueView implements EntityView<TariffRevenue> {
    @Override public Class<TariffRevenue> entity() { return TariffRevenue.class; }
    @Override public void list(ListSpec<TariffRevenue> list) {
        list.column(TariffRevenue::getTariff, "Тариф");
        list.column(TariffRevenue::getCustomer, "Клиент");
        list.column(TariffRevenue::getAmount, "Выручка");
        list.column(TariffRevenue::getPeriods, "Периодов");
    }
    @Override public void fields(EntityConfigBuilder<TariffRevenue> fields) {
        fields.field(TariffRevenue::getTariff).order(10).width("full");
        fields.field(TariffRevenue::getCustomer).order(20).width("full");
        fields.field(TariffRevenue::getAmount).order(30).width("full").format("currency:RUB").hint("Признаётся целиком при проведении подписки");
        fields.field(TariffRevenue::getPeriods).order(40).width("full");
    }
}
