package subscriptions.subscription;
import java.math.BigDecimal;
import su.onno.annotations.AccumulationRegister;
import su.onno.annotations.Dimension;
import su.onno.annotations.Resource;
import su.onno.model.AccumulationRecord;
import su.onno.model.AccumulationType;
import su.onno.types.Ref;
import subscriptions.Tariff;
import subscriptions.customer.Customer;
@AccumulationRegister(name = "TariffRevenue", title = "Выручка по тарифам", type = AccumulationType.TURNOVER)
public class TariffRevenue extends AccumulationRecord {
    @Dimension(displayName = "Тариф") private Ref<Tariff> tariff;
    @Dimension(displayName = "Клиент") private Ref<Customer> customer;
    @Resource(displayName = "Сумма", precision = 15, scale = 2) private BigDecimal amount;
    @Resource(displayName = "Периодов") private BigDecimal periods;
    public Ref<Tariff> getTariff() { return tariff; }
    public void setTariff(Ref<Tariff> value) { tariff = value; }
    public Ref<Customer> getCustomer() { return customer; }
    public void setCustomer(Ref<Customer> value) { customer = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public BigDecimal getPeriods() { return periods; }
    public void setPeriods(BigDecimal value) { periods = value; }
}
