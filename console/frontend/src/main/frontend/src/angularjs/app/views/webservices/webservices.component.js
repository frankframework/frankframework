import { appModule } from "../../app.module";

const WebservicesController = function (Api, Misc) {
    const ctrl = this;

	ctrl.$onInit = function () {
		ctrl.rootURL = Misc.getServerPath();

        Api.Get("webservices", function (data) {
            $.extend(ctrl, data);
        });
    };

    ctrl.compileURL = function (apiListener) {
        return ctrl.rootURL + "iaf/api/webservices/openapi.json?uri=" + encodeURI(apiListener.uriPattern);
    };
};

appModule.component('webservices', {
    controller: ['Api', 'Misc', WebservicesController],
    templateUrl: 'js/app/views/webservices/webservices.component.html'
});
