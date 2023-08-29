import { appModule } from "../../../app.module";

const JmsBrowseQueueController = function (Api, Cookies) {
    const ctrl = this;

    ctrl.destinationTypes = ["QUEUE", "TOPIC"];
    ctrl.form = {};
    ctrl.messages = [];
    ctrl.numberOfMessages = -1;
    ctrl.processing = false;

    ctrl.$onInit = function () {
        var browseJmsQueue = Cookies.get("browseJmsQueue");
        if (browseJmsQueue) ctrl.form = browseJmsQueue;
    };

    Api.Get("jms", function (data) {
        $.extend(ctrl, data);
        angular.element("select[name='type']").val(ctrl.destinationTypes[0]);
    });

    ctrl.submit = function (formData) {
        ctrl.processing = true;

        if (!formData || !formData.destination) {
            ctrl.error = "Please specify a connection factory and destination!";
            return;
        };

        Cookies.set("browseJmsQueue", formData);
        if (!formData.connectionFactory) formData.connectionFactory = ctrl.connectionFactories[0] || false;
        if (!formData.type) formData.type = ctrl.destinationTypes[0] || false;

        Api.Post("jms/browse", JSON.stringify(formData), function (data) {
            $.extend(ctrl, data);
            if (!data.messages) ctrl.messages = [];
            ctrl.error = "";
            ctrl.processing = false;
        }, function (errorData, status, errorMsg) {
            ctrl.error = (errorData && errorData.error) ? errorData.error : errorMsg;
            ctrl.processing = false;
        });
    };

    ctrl.reset = function () {
        console.log(ctrl.form)
        ctrl.error = "";
        if (!ctrl.form) return;
        if (ctrl.form.destination) ctrl.form.destination = "";
        if (ctrl.form.rowNumbersOnly) ctrl.form.rowNumbersOnly = "";
        if (ctrl.form.payload) ctrl.form.payload = "";
        if (ctrl.form.lookupDestination) ctrl.form.lookupDestination = "";
        if (ctrl.form.type) ctrl.form.type = ctrl.destinationTypes[0];

        ctrl.messages = [];
        ctrl.numberOfMessages = -1;
        ctrl.processing = false;
    };
};

appModule.component('jmsBrowseQueue', {
    controller: ['Api', 'Cookies', JmsBrowseQueueController],
    templateUrl: 'js/app/views/jms/jms-browse-queue/jms-browse-queue.component.html'
});
