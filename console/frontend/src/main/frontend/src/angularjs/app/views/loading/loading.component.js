import { appModule } from "../../app.module";

const LoadingController = function ($scope, Api, $state) {
    const ctrl = this;

    ctrl.$onInit = function () {
        Api.Get("server/health", function () {
            $state.go("pages.status");
        }, function (data, statusCode) {
            if (statusCode == 401) return;

            if (data.status == "SERVICE_UNAVAILABLE") {
                $state.go("pages.status");
            } else {
                $state.go("pages.errorpage");
            }
        });
    };
};

appModule.component('loading', {
    controller: ['$scope', 'Api', '$state', LoadingController],
    templateUrl: 'js/app/views/loading/loading.component.html'
});
