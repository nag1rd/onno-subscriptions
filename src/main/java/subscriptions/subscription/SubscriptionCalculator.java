package subscriptions.subscription;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public final class SubscriptionCalculator {
    private SubscriptionCalculator() { }
    public record Result(BigDecimal total, LocalDate endDate) { }
    public static Result calculate(LocalDate start, List<SubscriptionLine> lines) {
        if (start == null) throw new IllegalArgumentException("Укажите дату начала");
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("Добавьте хотя бы одну строку");
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        long longest = 0;
        for (SubscriptionLine line : lines) {
            if (line == null || line.getPeriods() == null || line.getPeriods() <= 0)
                throw new IllegalArgumentException("Число периодов должно быть больше нуля");
            if (line.getPrice() == null || line.getPrice().signum() < 0
                    || line.getPeriodDays() == null || line.getPeriodDays() <= 0)
                throw new IllegalArgumentException("У тарифа некорректная цена или длительность");
            BigDecimal amount = line.getPrice().multiply(BigDecimal.valueOf(line.getPeriods()))
                .setScale(2, RoundingMode.UNNECESSARY);
            if (amount.precision() > 15) throw new IllegalArgumentException("Сумма строки слишком велика");
            line.setAmount(amount);
            total = total.add(amount);
            longest = Math.max(longest, (long) line.getPeriodDays() * line.getPeriods());
        }
        if (total.precision() > 15) throw new IllegalArgumentException("Итоговая сумма слишком велика");
        try { return new Result(total, start.plusDays(longest)); }
        catch (java.time.DateTimeException e) { throw new IllegalArgumentException("Срок подписки слишком велик", e); }
    }
    public static SubscriptionStatus status(LocalDate today, LocalDate start, LocalDate end) {
        if (today.isBefore(start)) return SubscriptionStatus.WAITING;
        return today.isBefore(end) ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED;
    }
}
