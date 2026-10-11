package subscriptions.payment;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import su.onno.migration.AppMigration;
import su.onno.migration.MigrationContext;
import su.onno.repository.EnumerationPersistence;

@Component
public class PaymentMethodMigration implements AppMigration {

    @Override
    public String version() {
        return "2026.10.11.002";
    }

    @Override
    public String description() {
        return "Replace transfer and cash payment methods with SBP";
    }

    @Override
    public void migrate(MigrationContext context) {
        var document = context.registry().getDocumentDescriptor(Payment.class);
        var methodColumn = document.attributes().stream()
            .filter(attribute -> attribute.fieldName().equals("paymentMethod"))
            .findFirst()
            .orElseThrow()
            .columnName();

        // Identifiers come from validated onno metadata; values are bound parameters.
        String sql = "UPDATE " + document.tableName()
            + " SET " + methodColumn + " = :newId"
            + " WHERE " + methodColumn + " = :oldId";
        UUID newId = EnumerationPersistence.resolveId(PaymentMethod.class, PaymentMethod.SBP);

        for (String oldMethod : List.of("BANK_TRANSFER", "CASH")) {
            UUID oldId = UUID.nameUUIDFromBytes(
                ("subscriptions.payment.PaymentMethod." + oldMethod)
                    .getBytes(StandardCharsets.UTF_8)
            );
            context.handle().createUpdate(sql)
                .bind("oldId", oldId)
                .bind("newId", newId)
                .execute();
        }
    }
}
