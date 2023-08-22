import { StateService } from "angular-ui-router";
import {appModule} from "../../app.module";

class PagesTopinfobarController {
	loading = false;

  constructor(private $scope: angular.IScope, private $state: StateService){
    this.$state = $state; // not really needed, just to make sure nothing breaks
  }

	$onInit(){
		this.$scope.$on('loading', (event, loading) => { this.loading = loading; });
	}
}

appModule.component('pagesTopinfobar', {
	controller: ['$scope', '$state', PagesTopinfobarController],
  templateUrl: 'angularjs/app/components/pages/pages-topinfobar.component.html',
});
