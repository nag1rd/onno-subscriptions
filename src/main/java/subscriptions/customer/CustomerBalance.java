package subscriptions.customer;

import java.math.BigDecimal;

import su.onno.annotations.AccumulationRegister;
import su.onno.annotations.Dimension;
import su.onno.annotations.Resource;
import su.onno.model.AccumulationRecord;
import su.onno.model.AccumulationType;
import su.onno.types.Ref;

@AccumulationRegister(
    name = "CustomerBalances",
    title = "Лицевые счета",
    type = AccumulationType.BALANCE,
    allowNegative = false
)
public class CustomerBalance extends AccumulationRecord {

    @Dimension(displayName = "Клиент")
    private Ref<Customer> customer;

    @Resource(
        displayName = "Сумма",
        precision = 15,
        scale = 2
    )
    private BigDecimal amount;

    public Ref<Customer> getCustomer() {
        return customer;
    }

    public void setCustomer(Ref<Customer> customer) {
        this.customer = customer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}