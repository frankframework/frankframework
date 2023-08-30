import { appModule } from "../../app.module";

const TestServiceListenerController = function ($scope, Api, Alert) {
    const ctrl = this;

    ctrl.state = [];
    ctrl.file = null;
    ctrl.processingMessage = false;

    ctrl.$onInit = function () {
        Api.Get("test-servicelistener", function (data) {
            ctrl.services = data.services;
        });
    };

    ctrl.addNote = function (type, message, removeQueue) {
        ctrl.state.push({ type: type, message: message });
    };

    ctrl.updateFile = function (file) {
        ctrl.file = file;
    };

    ctrl.submit = function (formData) {
        ctrl.result = "";
        ctrl.state = [];
        if (!formData) {
            ctrl.addNote("warning", "Please specify a service and message!");
            return;
        }

        var fd = new FormData();
        if (formData.service && formData.service != "")
            fd.append("service", formData.service);
        if (formData.encoding && formData.encoding != "")
            fd.append("encoding", formData.encoding);
        if (formData.message && formData.message != "") {
            var encoding = (formData.encoding && formData.encoding != "") ? ";charset=" + formData.encoding : "";
            fd.append("message", new Blob([formData.message], { type: "text/plain" + encoding }), 'message');
        }
        if (ctrl.file)
            fd.append("file", ctrl.file, ctrl.file.name);

        if (!formData.message && !ctrl.file) {
            ctrl.addNote("warning", "Please specify a file or message!");
            return;
        }

        ctrl.processingMessage = true;
        Api.Post("test-servicelistener", fd, function (returnData) {
            var warnLevel = "success";
            if (returnData.state == "ERROR") warnLevel = "danger";
            ctrl.addNote(warnLevel, returnData.state);
            ctrl.result = (returnData.result);
            ctrl.processingMessage = false;
        }, function (returnData) {
            ctrl.result = (returnData.result);
            ctrl.processingMessage = false;
        });
    };
};

appModule.component('testServiceListener', {
    controller: ['$scope', 'Api', 'Alert', TestServiceListenerController],
    templateUrl: 'js/app/views/test-service-listener/test-service-listener.component.html'
});
