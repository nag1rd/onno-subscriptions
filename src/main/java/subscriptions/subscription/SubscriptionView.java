package subscriptions.subscription;
import org.springframework.stereotype.Component;
import su.onno.ui.*;
@Component
public class SubscriptionView implements EntityView<Subscription> {
    private final SubscriptionService service;
    public SubscriptionView(SubscriptionService service) { this.service = service; }
    @Override public Class<Subscription> entity() { return Subscription.class; }
    @Override public void list(ListSpec<Subscription> list) {
        list.column(Subscription::getNumber, "Номер");
        list.column(Subscription::getDate, "Дата");
        list.column(Subscription::getCustomer, "Клиент");
        list.column(Subscription::getStatus, "Статус");
        list.column(Subscription::getStartDate, "Начало");
        list.column(Subscription::getEndDate, "Окончание");
        list.column(Subscription::getTotal, "Итого");
        list.column(Subscription::isPosted, "Проведена");
    }
    @Override public void fields(EntityConfigBuilder<Subscription> fields) {
        fields.field(Subscription::getNumber).order(10).width("full").hint("Номер присваивается автоматически");
        fields.field(Subscription::getDate).order(20).width("full").format("dd.MM.yyyy HH:mm");
        fields.field(Subscription::getCustomer).order(30).width("full");
        fields.field(Subscription::getStartDate).order(40).width("full").format("dd.MM.yyyy").hint("Первый день действия подписки");
        fields.field(Subscription::getStatus).order(50).width("full").hideInForm().hint("Рассчитывается автоматически; отмена через действие");
        fields.field(Subscription::getEndDate).order(60).width("full").format("dd.MM.yyyy").hideInForm().hint("С этой даты подписка уже не действует");
        fields.field(Subscription::getTotal).order(70).width("full").format("currency:RUB").hideInForm();
        fields.field(Subscription::getCancellationReason).order(80).width("full").hideInForm();
        fields.field(Subscription::isPosted).hideInForm();
        fields.field(Subscription::getLines).label("Тарифы подписки").order(90).width("full");
        fields.rowField(Subscription::getLines, SubscriptionLine::getTariff).order(10).hint("Выберите доступный тариф");
        fields.rowField(Subscription::getLines, SubscriptionLine::getPeriods).order(20).hint("Целое число больше нуля");
        fields.rowField(Subscription::getLines, SubscriptionLine::getPrice).order(30).format("currency:RUB").hideInForm().hint("Снимок цены при первом сохранении строки");
        fields.rowField(Subscription::getLines, SubscriptionLine::getAmount).order(40).format("currency:RUB").hideInForm();
        fields.rowField(Subscription::getLines, SubscriptionLine::getPeriodDays).hideInForm();
    }
    @Override public void actions(ActionSpec actions) {
        for (var scope : new ActionScope[]{ActionScope.ROW, ActionScope.DETAIL}) {
            actions.action("cancel-" + scope.name().toLowerCase()).label("Отменить подписку").icon("x").scope(scope)
                .visibleWhen(row -> row.enumValue(Subscription::getStatus, SubscriptionStatus.class) != SubscriptionStatus.CANCELLED)
                .form(form -> form.input("reason").label("Причина отмены").type(InputType.TEXTAREA).required())
                 .handler(ctx -> {
                    try { service.cancel(ctx.id(), ctx.input("reason")); }
                    catch (IllegalArgumentException e) { throw ActionRejectedException.message(e.getMessage()); }
                    return ActionResult.reload();
                });
        }
    }
}
