import { appModule } from "../../app.module";

const WebservicesController = function ($scope, Api, Misc) {
    const ctrl = this;

    ctrl.rootURL = Misc.getServerPath();
    
    ctrl.$onInit = function () {
        Api.Get("webservices", function (data) {
            $.extend(ctrl, data);
        });
    };

    ctrl.compileURL = function (apiListener) {
        return ctrl.rootURL + "iaf/api/webservices/openapi.json?uri=" + encodeURI(apiListener.uriPattern);
    };
};

appModule.component('webservices', {
    controller: ['$scope', 'Api', 'Misc', WebservicesController],
    templateUrl: 'js/app/views/webservices/webservices.component.html'
});