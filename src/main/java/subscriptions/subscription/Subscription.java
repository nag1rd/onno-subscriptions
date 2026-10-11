package subscriptions.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Transient;

import su.onno.annotations.Attribute;
import su.onno.annotations.Document;
import su.onno.annotations.TabularSection;
import su.onno.lifecycle.BeforeWriteHandler;
import su.onno.lifecycle.OnFillingHandler;
import su.onno.lifecycle.Postable;
import su.onno.model.DocumentObject;
import su.onno.posting.PostingContext;
import su.onno.rules.BusinessRule;
import su.onno.rules.Validated;
import su.onno.types.Ref;
import subscriptions.BusinessRuntime;
import subscriptions.customer.Customer;
import subscriptions.customer.CustomerBalance;

@Document(name = "Subscriptions", title = "Подписки", numberPrefix = "SUB-")
public class Subscription extends DocumentObject implements OnFillingHandler, BeforeWriteHandler, Validated, Postable {
    @Attribute(displayName = "Клиент", required = true)
    private Ref<Customer> customer;
    @Attribute(displayName = "Статус")
    private SubscriptionStatus status;
    @Attribute(displayName = "Дата начала")
    private LocalDate startDate;
    @Attribute(displayName = "Дата окончания")
    private LocalDate endDate;
    @Attribute(displayName = "Итого", precision = 15, scale = 2)
    private BigDecimal total;
    @Attribute(displayName = "Причина отмены", length = 500)
    private String cancellationReason;
    @TabularSection(name = "lines")
    private List<SubscriptionLine> lines = new ArrayList<>();
    @Transient
    private boolean cancellationAuthorized;

    @Override public void onFilling() {
        var runtime = BusinessRuntime.get();
        if (getDate() == null) setDate(runtime.now());
        if (startDate == null) startDate = runtime.today();
        if (status == null) status = SubscriptionStatus.DRAFT;
    }
    @Override public void beforeWrite() {
        var runtime = BusinessRuntime.get();
        Subscription old = runtime.find(Subscription.class, getId()).orElse(null);
        if (old != null && old.status == SubscriptionStatus.CANCELLED && status != SubscriptionStatus.CANCELLED)
            throw new IllegalArgumentException("Отменённую подписку нельзя возобновить");
        if (old != null && (old.isPosted() || old.status == SubscriptionStatus.CANCELLED)) {
            if (!sameTerms(old)) throw new IllegalArgumentException("Проведённую подписку нельзя редактировать: сначала отмените проведение");
            if (status == SubscriptionStatus.CANCELLED && old.status != SubscriptionStatus.CANCELLED && !cancellationAuthorized)
                throw new IllegalArgumentException("Используйте действие «Отменить подписку»");
        }
        if (status == SubscriptionStatus.CANCELLED && old == null)
            throw new IllegalArgumentException("Сначала сохраните подписку, затем отмените её с указанием причины");
        if (lines != null) for (int index = 0; index < lines.size(); index++) {
            SubscriptionLine line = lines.get(index);
            if (line == null || line.getTariff() == null) continue;
            SubscriptionLine previous = old == null || old.lines == null ? null : old.lines.stream()
                .filter(l -> l.getId() != null && l.getId().equals(line.getId()) && Objects.equals(l.getTariff(), line.getTariff()))
                .findFirst().orElse(null);
            if (previous == null && old != null && old.lines != null && index < old.lines.size()
                    && Objects.equals(old.lines.get(index).getTariff(), line.getTariff())) previous = old.lines.get(index);
            if (previous != null) {
                line.setPrice(previous.getPrice());
                line.setPeriodDays(previous.getPeriodDays());
            } else {
                var tariff = runtime.tariff(line.getTariff());
                line.setPrice(tariff.getPrice());
                line.setPeriodDays(tariff.getPeriodDays());
            }
        }
        var result = SubscriptionCalculator.calculate(startDate, lines);
        total = result.total();
        endDate = result.endDate();
        if (status != SubscriptionStatus.CANCELLED) {
            cancellationReason = null;
            status = isPosted() ? SubscriptionCalculator.status(runtime.today(), startDate, endDate) : SubscriptionStatus.DRAFT;
        }
    }
    private boolean sameTerms(Subscription old) {
        if (!Objects.equals(customer, old.customer) || !Objects.equals(startDate, old.startDate)
                || !Objects.equals(getDate(), old.getDate()) || lines == null || old.lines == null || lines.size() != old.lines.size()) return false;
        for (int i = 0; i < lines.size(); i++) {
            var a = lines.get(i); var b = old.lines.get(i);
            if (a == null || !Objects.equals(a.getTariff(), b.getTariff()) || !Objects.equals(a.getPeriods(), b.getPeriods())) return false;
        }
        return true;
    }
    @Override public List<BusinessRule> rules() {
        return List.of(
            new BusinessRule("customer-required", "Укажите клиента", () -> customer != null),
            new BusinessRule("start-required", "Укажите дату начала", () -> startDate != null),
            new BusinessRule("lines-required", "Добавьте хотя бы одну строку", () -> lines != null && !lines.isEmpty()),
            new BusinessRule("periods-positive", "Число периодов должно быть больше нуля", () -> lines != null && lines.stream().allMatch(l -> l != null && l.getPeriods() != null && l.getPeriods() > 0)),
            new BusinessRule("tariff-available", "Тариф недоступен для подключения", () -> status == SubscriptionStatus.CANCELLED || isPosted() || lines != null && lines.stream().allMatch(l -> l != null && l.getTariff() != null && BusinessRuntime.get().tariff(l.getTariff()).isAvailable())),
            new BusinessRule("cancellation-reason", "Укажите причину отмены (до 500 символов)", () -> status != SubscriptionStatus.CANCELLED || cancellationReason != null && !cancellationReason.isBlank() && cancellationReason.length() <= 500)
        );
    }
    @Override public void handlePosting(PostingContext context) {
        if (status == SubscriptionStatus.CANCELLED) return;
        status = SubscriptionCalculator.status(BusinessRuntime.get().today(), startDate, endDate);
        if (total.signum() > 0) context.movements(CustomerBalance.class).addExpense(m -> {
            m.setCustomer(customer); m.setAmount(total);
        });
        for (SubscriptionLine line : lines) context.movements(TariffRevenue.class).addReceipt(m -> {
            m.setCustomer(customer); m.setTariff(line.getTariff());
            m.setAmount(line.getAmount()); m.setPeriods(BigDecimal.valueOf(line.getPeriods()));
        });
    }
    void cancel(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 500)
            throw new IllegalArgumentException("Укажите причину отмены (до 500 символов)");
        status = SubscriptionStatus.CANCELLED; cancellationReason = reason.strip(); cancellationAuthorized = true;
    }
    public Ref<Customer> getCustomer() { return customer; }
    public void setCustomer(Ref<Customer> value) { customer = value; }
    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus value) { status = value; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { startDate = value; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate = value; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal value) { total = value; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String value) { cancellationReason = value; }
    public List<SubscriptionLine> getLines() { return lines; }
    public void setLines(List<SubscriptionLine> value) { lines = value; }
}
