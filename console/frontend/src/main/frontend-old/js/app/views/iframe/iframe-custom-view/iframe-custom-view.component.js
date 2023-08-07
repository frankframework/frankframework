import { appModule } from "../../../app.module";

const IframeCustomViewController = function ($scope, Misc, $state, $window) {
    const ctrl = this;

    ctrl.$onInit = function () {
        if ($state.params.url == "")
            $state.go('pages.status');

        if ($state.params.url.indexOf("http") > -1) {
            $window.open($state.params.url, $state.params.name);
            ctrl.redirectURL = $state.params.url;
        }
        else
            ctrl.url = Misc.getServerPath() + $state.params.url;
    };
};

appModule.component('iframeCustomView', {
    controller: ['$scope', 'Misc', '$state', '$window', IframeCustomViewController],
    templateUrl: 'js/app/views/iframe/iframe.component.html'
});
