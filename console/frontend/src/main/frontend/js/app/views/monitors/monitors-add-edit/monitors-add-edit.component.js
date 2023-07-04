import { appModule } from "../../../app.module";

const MonitorsAddEditController = function ($scope, Api, $state) {
	const ctrl = this;
	ctrl.loading = true;
	ctrl.$state = $state;

	ctrl.selectedConfiguration = null;
	ctrl.monitor = "";
	ctrl.events = "";
	ctrl.severities = [];
	ctrl.triggerId = "";
	ctrl.trigger = {
		type: "Alarm",
		filter: "none",
		events: [],
	}

	ctrl.$onInit = function () {
		$scope.$on('loading', function () {
			ctrl.loading = false;
		});

		if ($state.params.configuration == "" || $state.params.monitor == "") {
			$state.go('pages.monitors');
		} else {
			ctrl.selectedConfiguration = $state.params.configuration;
			ctrl.monitor = $state.params.monitor;
			ctrl.triggerId = $state.params.trigger || "";
			ctrl.url = "configurations/" + ctrl.selectedConfiguration + "/monitors/" + ctrl.monitor + "/triggers/" + ctrl.triggerId;
			Api.Get(ctrl.url, function (data) {
				$.extend(ctrl, data);
				calculateEventSources();
				if (data.trigger && data.trigger.sources) {
					var sources = data.trigger.sources;
					ctrl.trigger.sources = [];
					ctrl.trigger.adapters = [];
					for (const adapter in sources) {
						if (data.trigger.filter == "SOURCE") {
							for (const i in sources[adapter]) {
								ctrl.trigger.sources.push(adapter + "$$" + sources[adapter][i]);
							}
						} else {
							ctrl.trigger.adapters.push(adapter);
						}
					}
				}
			}, function () {
				$state.go('pages.monitors', $state.params);
			});
		}
	};


	ctrl.getAdaptersForEvents = function (events) {
		if (!events) return [];

		var adapters = [];
		for (const eventName in ctrl.events) {
			if (events.indexOf(eventName) > -1) {
				let sourceList = ctrl.events[eventName].sources;
				adapters = adapters.concat(Object.keys(sourceList));
			}
		}
		return Array.from(new Set(adapters));
	}
	ctrl.eventSources = [];
	function calculateEventSources() {
		for (const eventCode in ctrl.events) {
			var retVal = [];
			var eventSources = ctrl.events[eventCode].sources;
			for (const adapter in eventSources) {
				for (const i in eventSources[adapter]) {
					retVal.push({ adapter: adapter, source: eventSources[adapter][i] });
				}
			}
			ctrl.eventSources[eventCode] = retVal;
		}
	}
	ctrl.getSourceForEvents = function (events) {
		var retval = [];
		for (const i in events) {
			var eventCode = events[i];
			retval = retval.concat(ctrl.eventSources[eventCode]);
		}
		return retval;
	}

	ctrl.submit = function (trigger) {
		if (trigger.filter == "ADAPTER") {
			delete trigger.sources;
		} else if (trigger.filter == "SOURCE") {
			delete trigger.adapters;
			var sources = trigger.sources;
			trigger.sources = {};
			for (const i in sources) {
				var s = sources[i].split("$$");
				var adapter = s[0];
				var source = s[1];
				if (!trigger.sources[adapter]) trigger.sources[adapter] = [];
				trigger.sources[adapter].push(source);
			}
		}
		if (ctrl.triggerId && ctrl.triggerId > -1) {
			Api.Put(ctrl.url, trigger, function (returnData) {
				$state.go('pages.monitors', $state.params);
			});
		} else {
			Api.Post(ctrl.url, JSON.stringify(trigger), function (returnData) {
				$state.go('pages.monitors', $state.params);
			});
		}
	}
};

appModule.component('monitorsAddEdit', {
	controller: ['$scope', 'Api', '$state', MonitorsAddEditController],
	templateUrl: 'js/app/views/monitors/monitors-add-edit/monitors-add-edit.component.html',
});
