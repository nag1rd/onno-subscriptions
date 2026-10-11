package subscriptions.customer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Component;

import su.onno.migration.AppMigration;
import su.onno.migration.MigrationContext;
import su.onno.repository.EnumerationPersistence;

@Component
public class CustomerStatusPackageMigration implements AppMigration {

    @Override
    public String version() {
        return "2026.10.11.001";
    }

    @Override
    public String description() {
        return "Preserve customer statuses after customer package move";
    }

    @Override
    public void migrate(MigrationContext context) {
        var catalog = context.registry().getCatalogDescriptor(Customer.class);
        var statusColumn = catalog.attributes().stream()
            .filter(attribute -> attribute.fieldName().equals("status"))
            .findFirst()
            .orElseThrow()
            .columnName();

        // Table/column identifiers come from validated onno metadata.
        String sql = "UPDATE " + catalog.tableName()
            + " SET " + statusColumn + " = :newId"
            + " WHERE " + statusColumn + " = :oldId";

        for (CustomerStatus status : CustomerStatus.values()) {
            UUID oldId = UUID.nameUUIDFromBytes(
                ("subscriptions.CustomerStatus." + status.name())
                    .getBytes(StandardCharsets.UTF_8)
            );
            UUID newId = EnumerationPersistence.resolveId(CustomerStatus.class, status);

            context.handle().createUpdate(sql)
                .bind("oldId", oldId)
                .bind("newId", newId)
                .execute();
        }
    }
}
