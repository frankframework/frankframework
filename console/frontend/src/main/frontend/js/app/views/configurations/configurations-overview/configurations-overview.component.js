import { appModule } from "../../../app.module";

const ConfigurationsOverviewController = function ($scope, Api, $state, $location) {
	const ctrl = this;

	ctrl.selectedConfiguration = ($state.params.name != '') ? $state.params.name : "All";
	ctrl.loadedConfiguration = ($state.params.loaded != undefined && $state.params.loaded == false);

	ctrl.update = function () {
		getConfiguration();
	};

	var anchor = $location.hash();
	ctrl.changeConfiguration = function (name) {
		ctrl.selectedConfiguration = name;
		$location.hash(''); //clear the hash from the url
		anchor = null; //unset hash anchor
		getConfiguration();
	};

	ctrl.updateQueryParams = function () {
		var transitionObj = {};
		if (ctrl.selectedConfiguration != "All")
			transitionObj.name = ctrl.selectedConfiguration;
		if (!ctrl.loadedConfiguration)
			transitionObj.loaded = ctrl.loadedConfiguration;

		$state.transitionTo('pages.configuration', transitionObj, { notify: false, reload: false });
	};

	ctrl.clipboard = function () {
		if (ctrl.configuration) {
			var el = document.createElement('textarea');
			el.value = ctrl.configuration;
			el.setAttribute('readonly', '');
			el.style.position = 'absolute';
			el.style.left = '-9999px';
			document.body.appendChild(el);
			el.select();
			document.execCommand('copy');
			document.body.removeChild(el);
		}
	}

	const getConfiguration = function () {
		ctrl.updateQueryParams();
		var uri = "configurations";
		if (ctrl.selectedConfiguration != "All") uri += "/" + ctrl.selectedConfiguration;
		if (ctrl.loadedConfiguration) uri += "?loadedConfiguration=true";
		Api.Get(uri, function (data) {
			ctrl.configuration = data;

			if (anchor) {
				$location.hash(anchor);
			}
		});
	};
	getConfiguration();
};

appModule.component('configurationsOverview', {
	controller: ['$scope', 'Api', '$state', '$location', ConfigurationsOverviewController],
	templateUrl: 'js/app/views/configurations/configurations-overview/configurations-overview.component.html',
});
