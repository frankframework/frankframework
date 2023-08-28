import { StateService } from "@uirouter/angularjs";
import { appModule } from "../../app.module";
import { Adapter, Alert, AppService, Configuration, MessageLog, MessageSummary, Receiver, Summary } from "../../app.service";
import { ConfigurationFilter } from "../../filters/configuration-filter.filter";
import { ApiService } from "../../services/api.service";
import { PollerService } from "../../services/poller.service";
import { MiscService } from "../../services/misc.service";

type Filter = Record<'started' | 'stopped' | 'warning', boolean>;

class StatusController {

  filter: Filter = {
		started: true,
		stopped: true,
		warning: true
	};
  configurations: Configuration[] = [];
  adapters: Record<string, Adapter> = {};
  adapterName = "";
	searchText = "";
	selectedConfiguration = "All";
	reloading = false;
	configurationFlowDiagram: string | null = null;
	isConfigStubbed: Record<string, boolean> = {};
	isConfigReloading: Record<string, boolean> = {};

  adapterSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    error: 0
  };
  receiverSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    error: 0
  };
  messageSummary: MessageSummary = {
    info: 0,
    warn: 0,
    error: 0
  };
  alerts: Alert[] = [];
  messageLog: Record<string, MessageLog> = {};
  // functions
  getProcessStateIcon = this.appService.getProcessStateIcon;
  getProcessStateIconColor = this.appService.getProcessStateIconColor;

  constructor(
    private $scope: angular.IScope,
    private $rootScope: angular.IRootScopeService,
    private Api: ApiService,
    private Poller: PollerService,
    private $filter: angular.IFilterService,
    private $state: StateService,
    private Misc: MiscService,
    private $anchorScroll: angular.IAnchorScrollService,
    private $location: angular.ILocationService,
    private $http: angular.IHttpService,
    private appService: AppService
  ){}

	$onInit() {
    var hash = this.$location.hash();
    this.adapterName = this.$state.params["adapter"];
		if (this.adapterName == "" && hash != "") { //If the adapter param hasn't explicitly been set
			this.adapterName = hash;
		} else {
      this.$location.hash(this.adapterName);
		}

    if (this.$state.params["filter"] != "") {
      var filter = this.$state.params["filter"].split("+");
			for (const f in this.filter) {
        this.filter[f as keyof Filter] = (filter.indexOf(f) > -1);
			}
		}
    if (this.$state.params["search"] != "") {
      this.searchText = this.$state.params["search"];
		}

    this.$scope.$on('appConstants', () => {
			this.updateConfigurationFlowDiagram(this.selectedConfiguration);
		});

		this.check4StubbedConfigs();
    this.adapterSummary = this.appService.adapterSummary;
    this.receiverSummary = this.appService.receiverSummary;
    this.messageSummary = this.appService.messageSummary;
    this.alerts = this.appService.alerts;
    this.messageLog = this.appService.messageLog;
    this.adapters = this.appService.adapters;
    this.$rootScope.$on('configurations', () => this.check4StubbedConfigs());
    this.$rootScope.$on('summaries', () => {
      this.adapterSummary = this.appService.adapterSummary;
      this.receiverSummary = this.appService.receiverSummary;
      this.messageSummary = this.appService.messageSummary;
		});
    this.$rootScope.$on('alerts', () => { this.alerts = this.appService.alerts; });
    this.$rootScope.$on('messageLog', () => { this.messageLog = this.appService.messageLog; });
    this.$rootScope.$on('adapters', () => { this.adapters = this.appService.adapters; });

    if (this.$state.params["configuration"] != "All")
      this.changeConfiguration(this.$state.params["configuration"]);
	};

	showContent(adapter: Adapter) {
		if (adapter.status == "stopped") {
			return true;
		} else if (this.adapterName != "" && adapter.name == this.adapterName) {
      this.$anchorScroll();
			return true;
		} else {
			return false;
		}
	};

	applyFilter(filter: Filter) {
		this.filter = filter;
		this.updateQueryParams();
	};

	updateQueryParams() {
		var filterStr = [];
		for (const f in this.filter) {
			if (this.filter[f as keyof Filter])
				filterStr.push(f);
		}
		var transitionObj: Record<string, string> = {};
		transitionObj["filter"] = filterStr.join("+");
		if (this.selectedConfiguration != "All")
			transitionObj["configuration"] = this.selectedConfiguration;
		if (this.searchText.length > 0)
			transitionObj["search"] = this.searchText;

    this.$state.transitionTo('pages.status', transitionObj, { notify: false, reload: false });
	};

	collapseAll() {
		$(".adapters").each((i, e) => {
      var a = $(e).find("div.ibox-title");
        // @ts-expect-error unknown property on scope
			angular.element(a).scope().showContent = false;
		});
	};
	expandAll() {
		$(".adapters").each((i, e) => {
			setTimeout(() => {
				var a = $(e).find("div.ibox-title");
        // @ts-expect-error unknown property on scope
				angular.element(a).scope().showContent = true;
			}, i * 10);
		});
	};
	stopAll() {
		let compiledAdapterList = Array();
		// let adapters = $filter('configurationFilter')($scope.adapters, $scope);
    let adapters = this.$filter<ConfigurationFilter>('configurationFilter')(this.adapters, this);
		for (const adapter in adapters) {
      let configuration = adapters[adapter].configuration;
			compiledAdapterList.push(configuration + "/" + adapter);
		}
    this.Api.Put("adapters", { "action": "stop", "adapters": compiledAdapterList });
	};
	startAll() {
		let compiledAdapterList = Array();
		// let adapters = $filter('configurationFilter')($scope.adapters, $scope);
    let adapters = this.$filter<ConfigurationFilter>('configurationFilter')(this.adapters, this);
		for (const adapter in adapters) {
			let configuration = adapters[adapter].configuration;
			compiledAdapterList.push(configuration + "/" + adapter);
		}
    this.Api.Put("adapters", { "action": "start", "adapters": compiledAdapterList });
	};
	reloadConfiguration() {
		if (this.selectedConfiguration == "All") return;

		this.isConfigReloading[this.selectedConfiguration] = true;

    this.Poller.getAll().stop();
    this.Api.Put("configurations/" + this.selectedConfiguration, { "action": "reload" }, () => {
			this.startPollingForConfigurationStateChanges(() => {
        this.Poller.getAll().start();
			});
		});
	};
	fullReload() {
		this.reloading = true;
    this.Poller.getAll().stop();
    this.Api.Put("configurations", { "action": "reload" }, () => {
			this.reloading = false;
			this.startPollingForConfigurationStateChanges(() => {
        this.Poller.getAll().start();
			});
		});
	};

	startPollingForConfigurationStateChanges(callback?: () => void) {
    this.Poller.add("server/configurations", (configurations) => {
      this.appService.updateConfigurations(configurations);

			var ready = true;
			for (var i in configurations) {
				var config = configurations[i];
				//When all configurations are in state STARTED or in state STOPPED with an exception, remove the poller
				if (config.state != "STARTED" && !(config.state == "STOPPED" && config.exception != null)) {
					ready = false;
					break;
				}
			}
			if (ready) { //Remove poller once all states are STARTED
        this.Poller.remove("server/configurations");
				if (callback != null && typeof callback == "function") callback();
			}
		}, true);
	}

	showReferences() {
		window.open(this.configurationFlowDiagram!);
	};
	updateConfigurationFlowDiagram(configurationName: string) {
    var url = this.Misc.getServerPath() + 'iaf/api/configurations/';
		if (configurationName == "All") {
			url += "?flow=true";
		} else {
			url += configurationName + "/flow";
		}
    this.$http.get(url).then((data) => {
			let status = (data && data.status) ? data.status : 204;
			if (status == 200) {
				this.configurationFlowDiagram = url;
			}
		});
	}

	check4StubbedConfigs() {
		this.configurations = this.appService.configurations;
    for (var i in this.appService.configurations) {
      var config = this.appService.configurations[i];
			this.isConfigStubbed[config.name] = config.stubbed;
			this.isConfigReloading[config.name] = config.state == "STARTING" || config.state == "STOPPING"; //Assume reloading when in state STARTING (LOADING) or in state STOPPING (UNLOADING)
		}
	};

  // Commented out in template, so unused
	closeAlert(index: number) {
    this.appService.alerts.splice(index, 1);
    this.appService.updateAlerts(this.appService.alerts);
	};

	changeConfiguration(name: string) {
		this.selectedConfiguration = name;
    this.appService.updateAdapterSummary(name);
		this.updateQueryParams();
		this.updateConfigurationFlowDiagram(name);
	};

	startAdapter(adapter: Adapter) {
		adapter.state = 'starting';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name), { "action": "start" });
	};
	stopAdapter(adapter: Adapter) {
		adapter.state = 'stopping';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name), { "action": "stop" });
	};
	startReceiver(adapter: Adapter, receiver: Receiver) {
		receiver.state = 'loading';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name) + "/receivers/" + this.Misc.escapeURL(receiver.name), { "action": "start" });
	};
  stopReceiver(adapter: Adapter, receiver: Receiver) {
		receiver.state = 'loading';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name) + "/receivers/" + this.Misc.escapeURL(receiver.name), { "action": "stop" });
	};
  addThread(adapter: Adapter, receiver: Receiver) {
		receiver.state = 'loading';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name) + "/receivers/" + this.Misc.escapeURL(receiver.name), { "action": "incthread" });
	};
  removeThread(adapter: Adapter, receiver: Receiver) {
		receiver.state = 'loading';
		this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name) + "/receivers/" + this.Misc.escapeURL(receiver.name), { "action": "decthread" });
	};

};

appModule.component('status', {
	controller: ['$scope', '$rootScope', 'Api', 'Poller', '$filter', '$state', 'Misc', '$anchorScroll', '$location', '$http', 'appService', StatusController],
	templateUrl: 'angularjs/app/views/status/status.component.html',
});
