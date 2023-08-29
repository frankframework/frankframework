import { appModule } from "../../app.module";

const LoggingController = function (Api, Misc, $timeout, $state, $stateParams) {
    const ctrl = this;

    ctrl.viewFile = false;
    ctrl.alert = false;

    ctrl.$onInit = function () {
        //This is only false when the user opens the logging page
        var directory = ($stateParams.directory && $stateParams.directory.length > 0) ? $stateParams.directory : false;
        //The file param is only set when the user copy pastes an url in their browser
        if ($stateParams.file && $stateParams.file.length > 0) {
            var file = $stateParams.file;

            ctrl.directory = directory;
            ctrl.path = directory + "/" + file;
            openFile({ path: directory + "/" + file, name: file });
        }
        else {
            openDirectory(directory);
        }
    };

    ctrl.closeFile = function () {
        ctrl.viewFile = false;
        $state.transitionTo('pages.logging_show', { directory: ctrl.directory });
    };

    ctrl.download = function (file) {
        var url = Misc.getServerPath() + "FileViewerServlet?resultType=bin&fileName=" + Misc.escapeURL(file.path);
        window.open(url, "_blank");
    };

    ctrl.open = function (file) {
        if (file.type == "directory") {
            $state.transitionTo('pages.logging_show', { directory: file.path });
        } else {
            $state.transitionTo('pages.logging_show', { directory: ctrl.directory, file: file.name }, { notify: false, reload: false });
        }
    };

    var getFileType = function (fileName) {
        if (fileName.indexOf('-stats_') >= 0)
            return 'stats';
        else if (fileName.indexOf('_xml.log') >= 0)
            return 'log4j';
        else if (fileName.indexOf('-stats_') >= 0 || fileName.indexOf('_xml.log') >= 0)
            return 'xml';
        else if (fileName.indexOf('-stats_') < 0 && fileName.indexOf('_xml.log') < 0)
            return 'html';
    };

    var openFile = function (file) {
        var resultType = "";
        var params = "";
        var as = getFileType(file.name);
        switch (as) {
            case "stats":
                resultType = "html";
                params += "&stats=true";
                break;

            case "log4j":
                resultType = "html";
                params += "&log4j=true";

            default:
                resultType = as;
                break;
        }

        var URL = Misc.getServerPath() + "FileViewerServlet?resultType=" + resultType + "&fileName=" + Misc.escapeURL(file.path) + params;
        if (resultType == "xml") {
            window.open(URL, "_blank");
            return;
        }

        ctrl.viewFile = URL;
        $timeout(function () {
            var iframe = angular.element("iframe");

            iframe[0].onload = function () {
                var iframeBody = $(iframe[0].contentWindow.document.body);
                iframe.css({ "height": iframeBody.height() + 50 });
            };
        });
    };

    var openDirectory = function (directory) {
        var url = "logging";
        if (directory) {
            url = "logging?directory=" + directory;
        }

        Api.Get(url, function (data) {
            ctrl.alert = false;
            $.extend(ctrl, data);
            ctrl.path = data.directory;
            if (data.count > data.list.length) {
                ctrl.alert = "Total number of items [" + data.count + "] exceeded maximum number, only showing first [" + (data.list.length - 1) + "] items!";
            }
        }, function (data) {
            ctrl.alert = (data) ? data.error : "An unknown error occured!";
        }, false);
    };
};

appModule.component('logging', {
    controller: ['Api', 'Misc', '$timeout', '$state', '$stateParams', LoggingController],
    templateUrl: 'js/app/views/logging/logging.component.html'
});
