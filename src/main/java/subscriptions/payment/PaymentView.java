package subscriptions.payment;

import org.springframework.stereotype.Component;

import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

@Component
public class PaymentView implements EntityView<Payment> {

    @Override
    public Class<Payment> entity() {
        return Payment.class;
    }

    @Override
    public void list(ListSpec<Payment> list) {
        list.column(Payment::getNumber, "Номер");
        list.column(Payment::getDate, "Дата");
        list.column(Payment::getCustomer, "Клиент");
        list.column(Payment::getAmount, "Сумма");
        list.column(Payment::getPaymentMethod, "Способ оплаты");
        list.column(Payment::isPosted, "Проведён");
    }

    @Override
    public void fields(EntityConfigBuilder<Payment> fields) {
        fields.field(Payment::getNumber)
            .label("Номер")
            .order(10)
            .width("full");

        fields.field(Payment::getDate)
            .label("Дата")
            .order(20)
            .width("full")
            .format("dd.MM.yyyy HH:mm");

        fields.field(Payment::getCustomer)
            .order(30)
            .width("full")
            .hint("Клиент, чей лицевой счёт пополняется");

        fields.field(Payment::getAmount)
            .order(40)
            .width("full")
            .format("currency:RUB")
            .hint("Сумма пополнения, больше нуля");

        fields.field(Payment::getPaymentMethod)
            .order(50)
            .width("full");

        fields.field(Payment::isPosted)
            .label("Проведён")
            .order(60)
            .hideInForm();
    }
}