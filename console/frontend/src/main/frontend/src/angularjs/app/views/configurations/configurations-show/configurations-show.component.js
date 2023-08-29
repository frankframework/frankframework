import { appModule } from "../../../app.module";

const ConfigurationsShowController = function (Api, $state, $location, appService) {
	const ctrl = this;

	ctrl.selectedConfiguration = ($state.params.name != '') ? $state.params.name : "All";
	ctrl.loadedConfiguration = ($state.params.loaded != undefined && $state.params.loaded == false);
	ctrl.anchor = $location.hash();

	ctrl.$onInit = function () {
		ctrl.configurations = appService.configurations;
    appService.configurations$.subscribe(function () { ctrl.configurations = appService.configurations; });
		ctrl.getConfiguration();
	};

	ctrl.update = function () {
		ctrl.getConfiguration();
	};

	ctrl.changeConfiguration = function (name) {
		ctrl.selectedConfiguration = name;
		$location.hash(''); //clear the hash from the url
		ctrl.anchor = null; //unset hash anchor
		ctrl.getConfiguration();
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

	ctrl.getConfiguration = function () {
		ctrl.updateQueryParams();
		var uri = "configurations";
		if (ctrl.selectedConfiguration != "All") uri += "/" + ctrl.selectedConfiguration;
		if (ctrl.loadedConfiguration) uri += "?loadedConfiguration=true";
		Api.Get(uri, function (data) {
			ctrl.configuration = data;

			if (ctrl.anchor) {
				$location.hash(anchor);
			}
		});
	};
};

appModule.component('configurationsShow', {
	controller: ['Api', '$state', '$location', 'appService', ConfigurationsShowController],
	templateUrl: 'js/app/views/configurations/configurations-show/configurations-show.component.html',
});
