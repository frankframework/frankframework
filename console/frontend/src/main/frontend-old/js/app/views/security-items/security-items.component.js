import { appModule } from "../../app.module";

const SecurityItemsController = function ($scope, Api, $rootScope, appService) {
    const ctrl = this;

    ctrl.sapSystems = [];
    ctrl.serverProps;
    ctrl.authEntries = [];
    ctrl.jmsRealms = [];
    ctrl.securityRoles = [];
    ctrl.certificates = [];

    ctrl.$onInit = function () {
        for (const a in appService.adapters) {
            var adapter = appService.adapters[a];
            if (adapter.pipes) {
                for (const p in adapter.pipes) {
                    var pipe = adapter.pipes[p];
                    if (pipe.certificate)
                        ctrl.certificates.push({
                            adapter: a,
                            pipe: p.name,
                            certificate: pipe.certificate
                        });
                };
            };
        };

        Api.Get("securityitems", function (data) {
            $.extend(ctrl, data);
        });
    };
};

appModule.component('securityItems', {
    controller: ['$scope', 'Api', '$rootScope', 'appService', SecurityItemsController],
    templateUrl: 'js/app/views/security-items/security-items.component.html'
});
