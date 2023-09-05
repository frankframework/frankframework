import * as angular from "angular";
import { appModule } from "../../../app.module";
import { StateService } from "@uirouter/angularjs";
import { MiscService } from "src/angularjs/app/services/misc.service";

class IframeCustomViewController {
    url = "";
    redirectURL = "";

    constructor(
        private $scope: angular.IScope,
        private Misc: MiscService,
        private $state: StateService,
        private $window: angular.IWindowService
    ) { };

    $onInit() {
        if (this.$state.params["url"] == "")
            this.$state.go('pages.status');

        if (this.$state.params["url"].indexOf("http") > -1) {
            this.$window.open(this.$state.params["url"], this.$state.params["name"]);
            this.redirectURL = this.$state.params["url"];
        }
        else
            this.url = this.Misc.getServerPath() + this.$state.params["url"];
    };
};

appModule.component('iframeCustomView', {
    controller: ['$scope', 'Misc', '$state', '$window', IframeCustomViewController],
    templateUrl: 'angularjs/app/views/iframe/iframe.component.html'
});
