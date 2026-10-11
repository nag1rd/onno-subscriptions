package subscriptions.customer;

import org.springframework.stereotype.Component;

import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

@Component
public class CustomerView implements EntityView<Customer> {
    @Override
    public Class<Customer> entity() {
        return Customer.class;
    }

    @Override
    public void list(ListSpec<Customer> list) {
        list.column(Customer::getCode, "Код");
        list.column(Customer::getDescription, "Имя");
        list.column(Customer::getEmail, "E-mail");
        list.column(Customer::getPhone, "Телефон");
        list.column(Customer::getStatus, "Статус");
        list.column(Customer::getRegisteredAt, "Дата регистрации");
    }

    @Override
    public void fields(EntityConfigBuilder<Customer> fields) {
        fields.field(Customer::getDescription)
            .label("Имя клиента")
            .order(10)
            .width("full");

        fields.field(Customer::getEmail)
            .order(20)
            .width("full");

        fields.field(Customer::getPhone)
            .order(30)
            .width("full");
        fields.field(Customer::getStatus)
            .order(40)
            .width("full")
            .hint("Текущий статус клиента");

        fields.field(Customer::getRegisteredAt)
                    .format("dd.MM.yyyy")
            .order(50)
            .width("full")
            .hint("Дата регистрации клиента в системе");
    }
}
