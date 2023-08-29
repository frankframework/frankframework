import { appModule } from "../../app.module";

const MonitorsController = function (Api, $state, Misc, $rootScope, appService) {
	const ctrl = this;

	ctrl.selectedConfiguration = null;
	ctrl.monitors = [];
	ctrl.destinations = [];
	ctrl.eventTypes = [];
	ctrl.totalRaised = 0;

	ctrl.$onInit = function () {
		ctrl.configurations = appService.configurations;
    appService.configurations$.subscribe(function () {
			ctrl.configurations = appService.configurations;

			if (ctrl.configurations.length > 0) {
				ctrl.updateConfigurations();
			}
		});

		if (ctrl.configurations.length > 0) {
			ctrl.updateConfigurations();
		}
	};

	ctrl.updateConfigurations = function(){
		var configName = $state.params.configuration; //See if the configuration query param is populated
		if (!configName) configName = ctrl.configurations[0].name; //Fall back to the first configuration
		ctrl.changeConfiguration(configName); //Update the view
	}

	ctrl.changeConfiguration = function (name) {
		ctrl.selectedConfiguration = name;

		if ($state.params.configuration == "" || $state.params.configuration != name) { //Update the URL
			$state.transitionTo('pages.monitors', { configuration: name }, { notify: false, reload: false });
		}

		ctrl.update();
	};

	ctrl.update = function() {
		Api.Get("configurations/" + ctrl.selectedConfiguration + "/monitors", function (data) {
			$.extend(ctrl, data);

			ctrl.totalRaised = 0;
			for (const i in ctrl.monitors) {
				if (ctrl.monitors[i].raised) ctrl.totalRaised++;
				var monitor = ctrl.monitors[i];
				monitor.activeDestinations = [];
				for (const j in ctrl.destinations) {
					var destination = ctrl.destinations[j];
					monitor.activeDestinations[destination] = (monitor.destinations.indexOf(destination) > -1);
				}
			}
		});
	}

	ctrl.getUrl = function(monitor, trigger) {
		var url = "configurations/" + ctrl.selectedConfiguration + "/monitors/" + monitor.name;
		if (trigger != undefined && trigger != "") url += "/triggers/" + trigger.id;
		return url;
	}

	ctrl.raise = function (monitor) {
		Api.Put(ctrl.getUrl(monitor), { action: "raise" }, function () {
			ctrl.update();
		});
	}
	ctrl.clear = function (monitor) {
		Api.Put(ctrl.getUrl(monitor), { action: "clear" }, function () {
			ctrl.update();
		});
	}
	ctrl.edit = function (monitor) {
		var destinations = [];
		for (const dest in monitor.activeDestinations) {
			if (monitor.activeDestinations[dest]) {
				destinations.push(dest);
			}
		}
		Api.Put(ctrl.getUrl(monitor), { action: "edit", name: monitor.displayName, type: monitor.type, destinations: destinations }, function () {
			ctrl.update();
		});
	}
	ctrl.deleteMonitor = function (monitor) {
		Api.Delete(ctrl.getUrl(monitor), function () {
			ctrl.update();
		});
	}

	ctrl.deleteTrigger = function (monitor, trigger) {
		Api.Delete(ctrl.getUrl(monitor, trigger), function () {
			ctrl.update();
		});
	}

	ctrl.downloadXML = function (monitorName) {
		var url = Misc.getServerPath() + "iaf/api/configurations/" + ctrl.selectedConfiguration + "/monitors";
		if (monitorName) {
			url += "/" + monitorName;
		}
		window.open(url + "?xml=true", "_blank");
	}
};

appModule.component('monitors', {
	controller: ['Api', '$state', 'Misc', '$rootScope', 'appService', MonitorsController],
	templateUrl: 'js/app/views/monitors/monitors.component.html',
});
