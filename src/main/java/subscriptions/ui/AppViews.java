package subscriptions.ui;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;
import subscriptions.Tariff;

@Configuration
public class AppViews {

    @Bean
    public EntityView<Tariff> tariffView() {
        return new EntityView<Tariff>() {

            @Override
            public Class<Tariff> entity() {
                return Tariff.class;
            }

            @Override
            public void list(ListSpec<Tariff> list) {
                list.column(Tariff::getCode, "Код");
                list.column(Tariff::getDescription, "Название");
                list.column(Tariff::getPrice, "Цена за период");
                list.column(Tariff::getPeriodDays, "Дней в периоде");
                list.column(Tariff::isAvailable, "Доступен");
            }

            @Override
            public void fields(EntityConfigBuilder<Tariff> fields) {
                fields.field(Tariff::getDescription)
                    .label("Название тарифа")
                    .order(10)
                    .width("full");

                fields.field(Tariff::getPrice)
                    .order(20)
                    .width("full")
                    .format("currency:RUB")
                    .hint("Стоимость одного периода в рублях");

                fields.field(Tariff::getPeriodDays)
                    .order(30)
                    .width("full")
                    .hint("Длительность одного периода, минимум один день");

                fields.field(Tariff::isAvailable)
                    .order(40)
                    .width("full")
                    .hint("Можно ли выбрать тариф при оформлении подписки");
            }
        };
    }
}