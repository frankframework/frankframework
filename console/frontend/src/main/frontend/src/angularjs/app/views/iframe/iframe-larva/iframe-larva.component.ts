import * as angular from "angular";
import { appModule } from "../../../app.module";
import { MiscService } from "src/app/services.types";

class IframeLarvaController {
    url = "";

    constructor(
        private $scope: angular.IScope,
        private Misc: MiscService,
        private $interval: angular.IIntervalService
    ) { };

    $onInit() {
        this.url = this.Misc.getServerPath() + "iaf/larva";
    };
};

appModule.component('iframeLarva', {
    controller: ['$scope', 'Misc', '$interval', IframeLarvaController],
    templateUrl: 'angularjs/app/views/iframe/iframe.component.html'
});
