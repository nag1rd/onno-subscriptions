package subscriptions;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import subscriptions.subscription.*;
import static org.assertj.core.api.Assertions.*;
class SubscriptionCalculatorTest {
    SubscriptionLine line(String price, int days, int periods) {
        var l = new SubscriptionLine(); l.setPrice(new BigDecimal(price)); l.setPeriodDays(days); l.setPeriods(periods); return l;
    }
    @Test void leapYearAndZeroPriceAreSupported() {
        var result = SubscriptionCalculator.calculate(LocalDate.of(2024,2,28), List.of(line("0.00",1,2)));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2024,3,1));
        assertThat(result.total()).isEqualByComparingTo("0");
    }
    @Test void moneyDoesNotLoseKopecks() {
        var result = SubscriptionCalculator.calculate(LocalDate.of(2026,1,1), List.of(line("0.10",30,3), line("0.20",1,1)));
        assertThat(result.total()).isEqualByComparingTo("0.50");
    }
    @Test void excessiveMoneyAndDatesAreRejected() {
        assertThatThrownBy(() -> SubscriptionCalculator.calculate(LocalDate.of(2026,1,1), List.of(line("9999999999999.99",1,2))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SubscriptionCalculator.calculate(LocalDate.MAX, List.of(line("1.00",1,1))))
            .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void boundariesAreStartInclusiveAndEndExclusive() {
        LocalDate start = LocalDate.of(2026,10,11), end = start.plusDays(30);
        assertThat(SubscriptionCalculator.status(start.minusDays(1), start, end)).isEqualTo(SubscriptionStatus.WAITING);
        assertThat(SubscriptionCalculator.status(start, start, end)).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(SubscriptionCalculator.status(end.minusDays(1), start, end)).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(SubscriptionCalculator.status(end, start, end)).isEqualTo(SubscriptionStatus.EXPIRED);
    }
}
