package subscriptions.subscription;
import java.math.BigDecimal;
import su.onno.annotations.Attribute;
import su.onno.model.TabularSectionRow;
import su.onno.types.Ref;
import subscriptions.Tariff;
public class SubscriptionLine extends TabularSectionRow {
    @Attribute(displayName = "Тариф", required = true)
    private Ref<Tariff> tariff;
    @Attribute(displayName = "Периодов", required = true, min = 1)
    private Integer periods;
    @Attribute(displayName = "Цена за период", precision = 15, scale = 2)
    private BigDecimal price;
    @Attribute(displayName = "Сумма", precision = 15, scale = 2)
    private BigDecimal amount;
    @Attribute(displayName = "Дней в периоде")
    private Integer periodDays;
    public Ref<Tariff> getTariff() { return tariff; }
    public void setTariff(Ref<Tariff> value) { tariff = value; }
    public Integer getPeriods() { return periods; }
    public void setPeriods(Integer value) { periods = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { price = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public Integer getPeriodDays() { return periodDays; }
    public void setPeriodDays(Integer value) { periodDays = value; }
}
