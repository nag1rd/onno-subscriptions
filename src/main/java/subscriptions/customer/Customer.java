package subscriptions.customer;

import java.time.LocalDate;

import su.onno.annotations.Attribute;
import su.onno.annotations.Catalog;
import su.onno.lifecycle.OnFillingHandler;
import su.onno.model.CatalogObject;

@Catalog(
    name = "Customers",
    title = "Клиенты",
    codePrefix = "C-"
)
public class Customer extends CatalogObject implements OnFillingHandler {

    @Attribute(displayName = "E-mail", length = 200, email = true)
    private String email;

    @Attribute(displayName = "Телефон", length = 30)
    private String phone;

    @Attribute(displayName = "Статус")
    private CustomerStatus status;

    @Attribute(displayName = "Дата регистрации")
    private LocalDate registeredAt;

    @Override
    public void onFilling() {
        if (status == null) {
            status = CustomerStatus.ACTIVE;
        }

        if (registeredAt == null) {
            registeredAt = LocalDate.now();
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDate registeredAt) {
        this.registeredAt = registeredAt;
    }
}