import { appModule } from "../../app.module";

const TestingPipelineController = function ($scope, Api, Alert, $rootScope, appService) {
    const ctrl = this;

    ctrl.state = [];
    ctrl.file = null;
    ctrl.selectedConfiguration = "";

    ctrl.processingMessage = false;

    ctrl.sessionKeyIndex = 1;
    ctrl.sessionKeyIndices = [ctrl.sessionKeyIndex];

    var sessionKeys = [];

    ctrl.$onInit = function () {
        ctrl.configurations = appService.configurations;
        $rootScope.$on('configurations', function () { ctrl.configurations = appService.configurations; });

        ctrl.adapters = appService.adapters;
        $rootScope.$on('adapters', function () { ctrl.adapters = appService.adapters; });
    };

    ctrl.addNote = function (type, message, removeQueue) {
        ctrl.state.push({ type: type, message: message });
    };

    ctrl.updateSessionKeys = function (sessionKey, index) {
        let sessionKeyIndex = sessionKeys.findIndex(f => f.index === index); // find by index
        if (sessionKeyIndex >= 0) {
            if (sessionKey.name == "" && sessionKey.value == "") { // remove row if row is empty
                sessionKeys.splice(sessionKeyIndex, 1);
                ctrl.sessionKeyIndices.splice(sessionKeyIndex, 1);
            } else { // update existing key value pair
                sessionKeys[sessionKeyIndex].key = sessionKey.name;
                sessionKeys[sessionKeyIndex].value = sessionKey.value;
            }
            ctrl.state = [];
        } else if (sessionKey.name && sessionKey.name != "" && sessionKey.value && sessionKey.value != "") {
            let keyIndex = sessionKeys.findIndex(f => f.key === sessionKey.name);	// find by key
            // add new key
            if (keyIndex < 0) {
                ctrl.sessionKeyIndex += 1;
                ctrl.sessionKeyIndices.push(ctrl.sessionKeyIndex);
                sessionKeys.push({ index: index, key: sessionKey.name, value: sessionKey.value });
                ctrl.state = [];
            } else { // key with the same name already exists show warning
                if (ctrl.state.findIndex(f => f.message === "Session keys cannot have the same name!") < 0) //avoid adding it more than once
                    ctrl.addNote("warning", "Session keys cannot have the same name!");
            }
        }
    };

    ctrl.updateFile = function (file) {
        ctrl.file = file;
    };

    ctrl.submit = function (formData) {
        ctrl.result = "";
        ctrl.state = [];
        if (!formData && ctrl.selectedConfiguration == "") {
            ctrl.addNote("warning", "Please specify a configuration");
            return;
        }

        let fd = new FormData();
        fd.append("configuration", ctrl.selectedConfiguration);
        if (formData && formData.adapter && formData.adapter != "") {
            fd.append("adapter", formData.adapter);
        } else {
            ctrl.addNote("warning", "Please specify an adapter!");
            return;
        }
        if (formData.encoding && formData.encoding != "")
            fd.append("encoding", formData.encoding);
        if (formData.message && formData.message != "") {
            let encoding = (formData.encoding && formData.encoding != "") ? ";charset=" + formData.encoding : "";
            fd.append("message", new Blob([formData.message], { type: "text/plain" + encoding }), 'message');
        }
        if (ctrl.file)
            fd.append("file", ctrl.file, ctrl.file.name);

        if (sessionKeys.length > 0) {
            let incompleteKeyIndex = sessionKeys.findIndex(f => (f.key === "" || f.value === ""));
            if (incompleteKeyIndex < 0) {
                fd.append("sessionKeys", JSON.stringify(sessionKeys));
            } else {
                ctrl.addNote("warning", "Please make sure all sessionkeys have name and value!");
                return;
            }
        }

        ctrl.processingMessage = true;
        Api.Post("test-pipeline", fd, function (returnData) {
            var warnLevel = "success";
            if (returnData.state == "ERROR") warnLevel = "danger";
            ctrl.addNote(warnLevel, returnData.state);
            ctrl.result = (returnData.result);
            ctrl.processingMessage = false;
            if (ctrl.file != null) {
                angular.element(".form-file")[0].value = null;
                ctrl.file = null;
                formData.message = returnData.message;
            }
        }, function (errorData) {
            let error = (errorData && errorData.error) ? errorData.error : "An error occured!";
            ctrl.result = "";
            ctrl.addNote("warning", error);
            ctrl.processingMessage = false;
        });
    };
};

appModule.component('testPipeline', {
    controller: ['$scope', 'Api', 'Alert', '$rootScope', 'appService', TestingPipelineController],
    templateUrl: 'js/app/views/test-pipeline/test-pipeline.component.html'
});
