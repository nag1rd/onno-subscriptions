package subscriptions;

import java.math.BigDecimal;

import su.onno.annotations.Attribute;
import su.onno.annotations.Catalog;
import su.onno.model.CatalogObject;

@Catalog(
    name = "Tariffs",
    title = "Тарифы",
    codePrefix = "T-"
)
public class Tariff extends CatalogObject {

    @Attribute(
        displayName = "Цена за период",
        required = true,
        precision = 15,
        scale = 2,
        min = 0
    )
    private BigDecimal price;

    @Attribute(
        displayName = "Длительность периода, дней",
        required = true,
        min = 1
    )
    private Integer periodDays;

    @Attribute(displayName = "Доступен для подключения")
    private boolean available = true;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(Integer periodDays) {
        this.periodDays = periodDays;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}