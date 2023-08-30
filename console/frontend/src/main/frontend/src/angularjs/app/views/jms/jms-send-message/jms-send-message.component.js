import { appModule } from "../../../app.module";

const JmsSendMessageController = function (Api) {
    const ctrl = this;

    ctrl.destinationTypes = ["QUEUE", "TOPIC"];
    ctrl.processing = false;
    ctrl.file = null;

    Api.Get("jms", function (data) {
        $.extend(ctrl, data);
        angular.element("select[name='type']").val(ctrl.destinationTypes[0]);
    });

    ctrl.submit = function (formData) {
        ctrl.processing = true;
        if (!formData) return;

        var fd = new FormData();
        if (formData.connectionFactory && formData.connectionFactory != "")
            fd.append("connectionFactory", formData.connectionFactory);
        else
            fd.append("connectionFactory", ctrl.connectionFactories[0]);
        if (formData.destination && formData.destination != "")
            fd.append("destination", formData.destination);
        if (formData.type && formData.type != "")
            fd.append("type", formData.type);
        else
            fd.append("type", ctrl.destinationTypes[0]);
        if (formData.replyTo && formData.replyTo != "")
            fd.append("replyTo", formData.replyTo);
        if (formData.persistent && formData.persistent != "")
            fd.append("persistent", formData.persistent);
        if (formData.synchronous && formData.synchronous != "")
            fd.append("synchronous", formData.synchronous);
        if (formData.lookupDestination && formData.lookupDestination != "")
            fd.append("lookupDestination", formData.lookupDestination);

        if (formData.propertyKey && formData.propertyKey != "" && formData.propertyValue && formData.propertyValue != "")
            fd.append("property", formData.propertyKey + "," + formData.propertyValue);
        if (formData.message && formData.message != "") {
            var encoding = (formData.encoding && formData.encoding != "") ? ";charset=" + formData.encoding : "";
            fd.append("message", new Blob([formData.message], { type: "text/plain" + encoding }), 'message');
        }
        if (ctrl.file)
            fd.append("file", ctrl.file, ctrl.file.name);
        if (formData.encoding && formData.encoding != "")
            fd.append("encoding", formData.encoding);

        if (!formData.message && !ctrl.file) {
            ctrl.error = "Please specify a file or message!";
            ctrl.processing = false;
            return;
        }

        Api.Post("jms/message", fd, function (returnData) {
            ctrl.error = null;
            ctrl.processing = false;
        }, function (errorData, status, errorMsg) {
            ctrl.processing = false;
            errorMsg = (errorMsg) ? errorMsg : "An unknown error occured, check the logs for more info.";
            ctrl.error = (errorData.error) ? errorData.error : errorMsg;
        });
    };

    ctrl.reset = function () {
        ctrl.error = "";
        if (!ctrl.form) return;
        if (ctrl.form.destination)
            ctrl.form.destination = "";
        if (ctrl.form.replyTo)
            ctrl.form.replyTo = "";
        if (ctrl.form.message)
            ctrl.form.message = "";
        if (ctrl.form.persistent)
            ctrl.form.persistent = "";
        if (ctrl.form.propertyValue)
            ctrl.form.propertyValue = "";
        if (ctrl.form.propertyKey)
            ctrl.form.propertyKey = "";
        if (ctrl.form.type)
            ctrl.form.type = ctrl.destinationTypes[0];
    };
};

appModule.component('jmsSendMessage', {
    controller: ['Api', JmsSendMessageController],
    templateUrl: 'js/app/views/jms/jms-send-message/jms-send-message.component.html'
});
