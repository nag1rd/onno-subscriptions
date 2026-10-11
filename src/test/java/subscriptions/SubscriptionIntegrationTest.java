package subscriptions;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import su.onno.access.AccessSubject;
import su.onno.metadata.MetadataRegistry;
import su.onno.posting.PostingService;
import su.onno.types.Ref;
import su.onno.ui.DocumentCommandService;
import su.onno.repository.EnumerationPersistence;
import subscriptions.customer.*;
import subscriptions.payment.*;
import subscriptions.subscription.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:subscription-tests;DB_CLOSE_DELAY=-1",
    "org.jobrunr.background-job-server.enabled=false",
    "org.jobrunr.dashboard.enabled=false"
})
class SubscriptionIntegrationTest {
    @Autowired CustomerRepository customers;
    @Autowired TariffRepository tariffs;
    @Autowired PaymentRepository payments;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired CustomerBalanceRepository balances;
    @Autowired TariffRevenueRepository revenue;
    @Autowired PostingService posting;
    @Autowired SubscriptionService service;
    @Autowired SubscriptionStatusJob job;
    @Autowired DocumentCommandService commands;
    @Autowired MetadataRegistry metadata;
    @Autowired MutableClock clock;
    Customer customer;
    Tariff month;

    @BeforeEach void prepare() {
        clock.set(LocalDate.of(2026, 10, 11));
        customer = new Customer(); customer.setDescription("Клиент " + UUID.randomUUID());
        customer = customers.save(customer);
        month = tariff("Месяц", "100.25", 30, true);
    }
    Tariff tariff(String name, String price, int days, boolean available) {
        var t = new Tariff(); t.setDescription(name); t.setPrice(new BigDecimal(price));
        t.setPeriodDays(days); t.setAvailable(available); return tariffs.save(t);
    }
    SubscriptionLine line(Tariff tariff, int periods) {
        var l = new SubscriptionLine(); l.setTariff(new Ref<>(Tariff.class, tariff.getId()));
        l.setPeriods(periods); return l;
    }
    Subscription draft(SubscriptionLine... lines) {
        var s = new Subscription(); s.setCustomer(new Ref<>(Customer.class, customer.getId()));
        s.setLines(new ArrayList<>(Arrays.asList(lines))); return subscriptions.save(s);
    }
    Payment pay(String amount) {
        var p = new Payment(); p.setCustomer(new Ref<>(Customer.class, customer.getId()));
        p.setAmount(new BigDecimal(amount)); p.setPaymentMethod(PaymentMethod.SBP);
        p = payments.save(p); posting.post(p); return p;
    }
    BigDecimal balance() {
        return balances.getBalance(f -> f.where(CustomerBalance::getCustomer, new Ref<>(Customer.class, customer.getId())))
            .stream().map(CustomerBalance::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    Subscription reload(Subscription s) { return subscriptions.findById(s.getId()).orElseThrow(); }
    BigDecimal earnings(Subscription s) {
        return revenue.getRecordsByDocument(s.getId()).stream().filter(TariffRevenue::isActive).map(TariffRevenue::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test void defaultsAndMaximumDurationAndDecimalAmounts() {
        var year = tariff("Год", "200.00", 365, true);
        var s = draft(line(month, 2), line(year, 1));
        assertThat(s.getDate()).isEqualTo(LocalDateTime.of(2026, 10, 11, 12, 0));
        assertThat(s.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(s.getEndDate()).isEqualTo(s.getStartDate().plusDays(365));
        assertThat(s.getTotal()).isEqualByComparingTo("400.50");
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.DRAFT);
        assertThat(balance()).isZero();
        assertThat(revenue.getRecordsByDocument(s.getId()).stream().filter(TariffRevenue::isActive)).isEmpty();
        assertThat(reload(s).getLines()).hasSize(2);
    }
    @Test void numberOfPeriodsParticipatesInMaximum() {
        var s = draft(line(month, 13), line(tariff("Год", "200", 365, true), 1));
        assertThat(s.getEndDate()).isEqualTo(s.getStartDate().plusDays(390));
    }
    @Test void exactBalancePostingRecognizesRevenueAndRepostIsIdempotent() {
        pay("200.50"); var s = draft(line(month, 2)); posting.post(s);
        assertThat(balance()).isZero();
        assertThat(earnings(s)).isEqualByComparingTo("200.50");
        assertThat(revenue.getRecordsByDocument(s.getId()).getFirst().getPeriods()).isEqualByComparingTo("2");
        assertThat(reload(s).isPosted()).isTrue();
        posting.repost(reload(s));
        assertThat(balance()).isZero(); assertThat(earnings(s)).isEqualByComparingTo("200.50");
    }
    @Test void insufficientMoneyRollsBackBothRegistersAndPostedFlag() {
        pay("100.00"); var s = draft(line(month, 1));
        assertThatThrownBy(() -> posting.post(s)).isInstanceOf(RuntimeException.class);
        assertThat(balance()).isEqualByComparingTo("100.00");
        assertThat(reload(s).isPosted()).isFalse();
        assertThat(balances.getRecordsByDocument(s.getId()).stream().filter(CustomerBalance::isActive)).isEmpty();
        assertThat(revenue.getRecordsByDocument(s.getId()).stream().filter(TariffRevenue::isActive)).isEmpty();
    }
    @Test void cancelledDraftNeverCreatesMovementsEvenIfPostedLater() {
        var s = draft(line(month, 1)); service.cancel(s.getId(), "Передумал");
        posting.post(reload(s));
        assertThat(reload(s).getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(balance()).isZero();
        assertThat(balances.getRecordsByDocument(s.getId()).stream().filter(CustomerBalance::isActive)).isEmpty();
        assertThat(revenue.getRecordsByDocument(s.getId()).stream().filter(TariffRevenue::isActive)).isEmpty();
    }
    @Test void cancellationReturnsFundsAndRemovesRevenueAndIsIdempotent() {
        pay("500"); var s = draft(line(month, 2)); posting.post(s);
        service.cancel(s.getId(), "  Ошибка оформления  ");
        var cancelled = reload(s);
        assertThat(cancelled.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("Ошибка оформления");
        assertThat(balance()).isEqualByComparingTo("500");
        assertThat(balances.getRecordsByDocument(s.getId()).stream().filter(CustomerBalance::isActive)).isEmpty();
        assertThat(revenue.getRecordsByDocument(s.getId()).stream().filter(TariffRevenue::isActive)).isEmpty();
        service.cancel(s.getId(), "Повторная отмена"); posting.repost(reload(s));
        assertThat(balance()).isEqualByComparingTo("500");
    }
    @Test void cancellationReasonRequiredAndDirectCancellationOfPostedDocumentRejected() {
        pay("500"); var s = draft(line(month, 1)); posting.post(s);
        assertThatThrownBy(() -> service.cancel(s.getId(), " ")).isInstanceOf(IllegalArgumentException.class);
        var edited = reload(s); edited.setStatus(SubscriptionStatus.CANCELLED); edited.setCancellationReason("обход действия");
        assertThatThrownBy(() -> subscriptions.save(edited)).isInstanceOf(RuntimeException.class);
        assertThat(reload(s).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(balance()).isEqualByComparingTo("399.75");
    }
    @Test void savedPriceAndDurationAreSnapshotsAndSubmittedPriceCannotChangeThem() {
        var s = draft(line(month, 1));
        month.setPrice(new BigDecimal("999")); month.setPeriodDays(10); tariffs.save(month);
        var edited = reload(s); edited.getLines().getFirst().setPeriods(2);
        edited.getLines().getFirst().setPrice(new BigDecimal("0.01"));
        edited = subscriptions.save(edited);
        assertThat(edited.getTotal()).isEqualByComparingTo("200.50");
        assertThat(edited.getEndDate()).isEqualTo(edited.getStartDate().plusDays(60));
    }
    @Test void generatedRestSaveUsesHooksAndPreservesSnapshots() {
        var descriptor = metadata.getDocumentDescriptor(Subscription.class);
        Map<String,Object> body = Map.of("customer", customer.getId().toString(), "lines", List.of(
            Map.of("tariff", month.getId().toString(), "periods", 2, "price", "0.01", "periodDays", 1)));
        var created = commands.create(descriptor, body, AccessSubject.system());
        var id = UUID.fromString(created.get("_id").toString());
        var s = subscriptions.findById(id).orElseThrow();
        assertThat(s.getTotal()).isEqualByComparingTo("200.50");
        assertThat(s.getEndDate()).isEqualTo(s.getStartDate().plusDays(60));
        month.setPrice(new BigDecimal("999")); month.setPeriodDays(1); tariffs.save(month);
        commands.update(descriptor, id, Map.of("lines", List.of(Map.of("tariff", month.getId().toString(), "periods", 3))), AccessSubject.system());
        s = subscriptions.findById(id).orElseThrow();
        assertThat(s.getTotal()).isEqualByComparingTo("300.75");
        assertThat(s.getEndDate()).isEqualTo(s.getStartDate().plusDays(90));
    }
    @Test void missingCustomerEmptyLinesZeroNegativeAndUnavailableTariffRejected() {
        var noCustomer = new Subscription(); noCustomer.setLines(List.of(line(month, 1)));
        assertThatThrownBy(() -> subscriptions.save(noCustomer)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> draft()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> draft(line(month, 0))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> draft(line(month, -1))).isInstanceOf(RuntimeException.class);
        var unavailable = tariff("Закрыт", "100", 30, false);
        assertThatThrownBy(() -> draft(line(unavailable, 1))).isInstanceOf(RuntimeException.class);
        var unknown = new SubscriptionLine(); unknown.setPeriods(1); unknown.setTariff(new Ref<>(Tariff.class, UUID.randomUUID()));
        assertThatThrownBy(() -> draft(unknown)).isInstanceOf(RuntimeException.class);
    }
    @Test void unavailableAfterSaveBlocksFirstPostingButNotHistoricalRepost() {
        pay("500"); var s = draft(line(month, 1));
        month.setAvailable(false); tariffs.save(month);
        assertThatThrownBy(() -> posting.post(s)).isInstanceOf(RuntimeException.class);
        month.setAvailable(true); tariffs.save(month); posting.post(reload(s));
        month.setAvailable(false); tariffs.save(month);
        posting.repost(reload(s));
        assertThat(earnings(s)).isEqualByComparingTo("100.25");
    }
    @Test void postedFinancialTermsCannotBeChangedWithoutUnposting() {
        var p = pay("500"); var editPayment = payments.findById(p.getId()).orElseThrow();
        editPayment.setAmount(new BigDecimal("900"));
        assertThatThrownBy(() -> payments.save(editPayment)).isInstanceOf(RuntimeException.class);
        var s = draft(line(month, 1)); posting.post(s); var edited = reload(s);
        edited.getLines().getFirst().setPeriods(2);
        assertThatThrownBy(() -> subscriptions.save(edited)).isInstanceOf(RuntimeException.class);
        assertThat(balance()).isEqualByComparingTo("399.75");
    }
    @Test void statusJobHonorsExclusiveEndAndSkipsDraftAndCancelled() {
        pay("1000"); var active = draft(line(month, 1));
        active.setStartDate(clock.date().plusDays(1)); active = subscriptions.save(active); posting.post(active);
        var untouched = draft(line(month, 1)); var cancelled = draft(line(month, 1)); service.cancel(cancelled.getId(), "Отмена");
        assertThat(reload(active).getStatus()).isEqualTo(SubscriptionStatus.WAITING);
        clock.set(LocalDate.of(2026,10,12)); job.execute();
        assertThat(reload(active).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        var end = reload(active).getEndDate(); clock.set(end.minusDays(1)); job.execute();
        assertThat(reload(active).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        clock.set(end); job.execute();
        assertThat(reload(active).getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(reload(untouched).getStatus()).isEqualTo(SubscriptionStatus.DRAFT);
        assertThat(reload(cancelled).getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(balance()).isEqualByComparingTo("899.75");
    }
    @Test void concurrentSubscriptionsCannotSpendSameMoneyTwice() throws Exception {
        pay("100"); var expensive = tariff("70 рублей", "70", 30, true);
        var first = draft(line(expensive, 1)); var second = draft(line(expensive, 1));
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (var document : List.of(first, second)) futures.add(executor.submit(() -> {
                ready.countDown(); start.await();
                try { posting.post(document); return true; } catch (RuntimeException e) { return false; }
            }));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            int success = 0; for (var f : futures) if (f.get(15, TimeUnit.SECONDS)) success++;
            assertThat(success).isEqualTo(1);
        }
        assertThat(balance()).isEqualByComparingTo("30");
        assertThat(earnings(first).add(earnings(second))).isEqualByComparingTo("70");
    }
    @Test void positivePaymentRequiredAndUnpostingSpentPaymentIsRejected() {
        var bad = new Payment(); bad.setCustomer(new Ref<>(Customer.class, customer.getId()));
        bad.setPaymentMethod(PaymentMethod.CARD); bad.setAmount(BigDecimal.ZERO);
        assertThatThrownBy(() -> payments.save(bad)).isInstanceOf(RuntimeException.class);
        var p = pay("100.25"); var s = draft(line(month, 1)); posting.post(s);
        assertThatThrownBy(() -> posting.unpost(payments.findById(p.getId()).orElseThrow())).isInstanceOf(RuntimeException.class);
        assertThat(balance()).isZero(); assertThat(payments.findById(p.getId()).orElseThrow().isPosted()).isTrue();
    }
    @Test void unpostingSubscriptionReturnsFundsAndRestoresDraftStatus() {
        pay("500"); var s = draft(line(month, 1)); posting.post(s);
        posting.unpost(reload(s));
        assertThat(reload(s).isPosted()).isFalse();
        assertThat(reload(s).getStatus()).isEqualTo(SubscriptionStatus.DRAFT);
        assertThat(balance()).isEqualByComparingTo("500");
        assertThat(earnings(s)).isZero();
    }
    @Test void generatedApiCannotChangePostedMoneyOrCancelWithoutAction() {
        var p = pay("500");
        assertThatThrownBy(() -> commands.update(metadata.getDocumentDescriptor(Payment.class), p.getId(),
            Map.of("amount", "999"), AccessSubject.system())).isInstanceOf(RuntimeException.class);
        var s = draft(line(month, 1)); posting.post(s);
        var cancelledId = EnumerationPersistence.resolveId(SubscriptionStatus.class, SubscriptionStatus.CANCELLED);
        assertThatThrownBy(() -> commands.update(metadata.getDocumentDescriptor(Subscription.class), s.getId(),
            Map.of("status", cancelledId.toString(), "cancellationReason", "Обход"), AccessSubject.system()))
            .isInstanceOf(RuntimeException.class);
        assertThat(balance()).isEqualByComparingTo("399.75");
        assertThat(earnings(s)).isEqualByComparingTo("100.25");
    }
    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary MutableClock testClock() { return new MutableClock(); }
    }
    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current = new AtomicReference<>();
        void set(LocalDate date) { current.set(date.atTime(12,0).atZone(getZone()).toInstant()); }
        LocalDate date() { return LocalDate.now(this); }
        @Override public ZoneId getZone() { return ZoneId.of("Europe/Moscow"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return current.get() == null ? Instant.parse("2026-10-11T09:00:00Z") : current.get(); }
    }
}
