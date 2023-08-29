import { appModule } from "../../app.module";

const NotificationsController = function ($stateParams, Hooks, Notification) {
    const ctrl = this;

    ctrl.$onInit = function () {
        if ($stateParams.id > 0) {
            ctrl.notification = Notification.get($stateParams.id);
        } else {
            ctrl.text = ("Showing a list with all notifications!");
        }

        Hooks.register("adapterUpdated:2", function (adapter) {
            console.warn("What is the scope of: ", adapter);
        });
    };
};

appModule.component('notifications', {
    controller: ['$stateParams', 'Hooks', 'Notification', NotificationsController],
    templateUrl: 'js/app/views/notifications/notifications.component.html'
});
