import * as angular from "angular";
import { appModule } from "../../../app.module";
import { MiscService } from "src/app/services.types";

class IframeLadybugBetaController {
    url = "";

    constructor(
        private $scope: angular.IScope,
        private Misc: MiscService
    ) { };

    $onInit() {
        this.url = this.Misc.getServerPath() + "iaf/ladybug";
    };
};

appModule.component('iframeLadybugBeta', {
    controller: ['$scope', 'Misc', IframeLadybugBetaController],
    templateUrl: 'angularjs/app/views/iframe/iframe.component.html'
});
