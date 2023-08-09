import * as angular from "angular";
import { appModule } from "../../../app.module";
import { MiscService } from "src/app/services.types";

class IframeLadybugController {
    url = "";

    constructor(
        private $scope: angular.IScope,
        private Misc: MiscService,
        private $timeout: angular.ITimeoutService
    ) { };

    $onInit() {
        this.url = this.Misc.getServerPath() + "iaf/testtool";
    };
};

appModule.component('iframeLadybug', {
    controller: ['$scope', 'Misc', '$timeout', IframeLadybugController],
    templateUrl: 'angularjs/app/views/iframe/iframe.component.html'
});
