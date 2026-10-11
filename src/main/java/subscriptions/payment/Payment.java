package subscriptions.payment;

import java.math.BigDecimal;
import java.util.List;

import su.onno.annotations.Attribute;
import su.onno.annotations.Document;
import su.onno.lifecycle.OnFillingHandler;
import su.onno.lifecycle.Postable;
import su.onno.model.DocumentObject;
import su.onno.posting.PostingContext;
import su.onno.rules.BusinessRule;
import su.onno.rules.Validated;
import su.onno.types.Ref;
import subscriptions.customer.Customer;
import subscriptions.customer.CustomerBalance;

@Document(
    name = "Payments",
    title = "Платежи",
    numberPrefix = "PAY-"
)
public class Payment extends DocumentObject
        implements OnFillingHandler, su.onno.lifecycle.BeforeWriteHandler, Validated, Postable {

    @Attribute(displayName = "Клиент", required = true)
    private Ref<Customer> customer;

    @Attribute(
        displayName = "Сумма",
        required = true,
        precision = 15,
        scale = 2
    )
    private BigDecimal amount;

    @Attribute(displayName = "Способ оплаты", required = true)
    private PaymentMethod paymentMethod;

    @Override
    public void onFilling() {
        if (getDate() == null) {
            setDate(subscriptions.BusinessRuntime.get().now());
        }
    }

    @Override
    public void beforeWrite() {
        var old = subscriptions.BusinessRuntime.get().find(Payment.class, getId()).orElse(null);
        if (old != null && old.isPosted()
                && (!java.util.Objects.equals(customer, old.customer)
                    || amount == null || old.amount == null || amount.compareTo(old.amount) != 0
                    || paymentMethod != old.paymentMethod
                    || !java.util.Objects.equals(getDate(), old.getDate()))) {
            throw new IllegalArgumentException("Проведённый платёж нельзя редактировать: сначала отмените проведение");
        }
    }

    @Override
    public List<BusinessRule> rules() {
        return List.of(
            new BusinessRule(
                "customer-required",
                "Укажите клиента",
                () -> customer != null
            ),
            new BusinessRule(
                "amount-positive",
                "Сумма платежа должна быть больше нуля",
                () -> amount != null && amount.signum() > 0
            ),
            new BusinessRule(
                "payment-method-required",
                "Укажите способ оплаты",
                () -> paymentMethod != null
            )
        );
    }

    @Override
    public void handlePosting(PostingContext context) {
        context.movements(CustomerBalance.class)
            .addReceipt(movement -> {
                movement.setCustomer(customer);
                movement.setAmount(amount);
            });
    }

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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}