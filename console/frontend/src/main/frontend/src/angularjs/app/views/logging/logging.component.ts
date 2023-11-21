import { appModule } from "../../app.module";
import { ApiService } from "../../services/api.service";
import { MiscService } from "../../services/misc.service";
import { StateParams, StateService } from "@uirouter/angularjs";

class LoggingController {
  viewFile: boolean = false;
  alert: boolean | string = false;
  directory: any;
  path: string = "";

  constructor(
    private Api: ApiService,
    private Misc: MiscService,
    private $state: StateService,
    private $stateParams: StateParams
  ) { };

  $onInit() {
    //This is only false when the user opens the logging page
    var directory = (this.$stateParams['directory'] && this.$stateParams['directory'].length > 0) ? this.$stateParams['directory'] : false;
    //The file param is only set when the user copy pastes an url in their browser
    if (this.$stateParams['file'] && this.$stateParams['file'].length > 0) {
      var file = this.$stateParams['file'];
      this.directory = directory;
      this.path = directory + "/" + file;
      this.openFile({ path: directory + "/" + file, name: file });
    }
    else {
      this.openDirectory(directory);
    }
  };

  closeFile() {
    this.viewFile = false;
    this.$state.transitionTo('pages.logging_show', { directory: this.directory });
  };

  download(file: any) {
    var url = this.Misc.getServerPath() + "FileViewerServlet?resultType=bin&fileName=" + this.Misc.escapeURL(file.path);
    window.open(url, "_blank");
  };

  open(file: any) {
    if (file.type == "directory") {
      this.$state.transitionTo('pages.logging_show', { directory: file.path });
    } else {
      this.$state.transitionTo('pages.logging_show', { directory: this.directory, file: file.name }, { notify: false, reload: false });
    }
  };

  getFileType(fileName: any) {
    if (fileName.indexOf('-stats_') >= 0)
      return 'stats';
    else if (fileName.indexOf('_xml.log') >= 0)
      return 'log4j';
    else if (fileName.indexOf('-stats_') >= 0 || fileName.indexOf('_xml.log') >= 0)
      return 'xml';
    else if (fileName.indexOf('-stats_') < 0 && fileName.indexOf('_xml.log') < 0)
      return 'html';

    return '';
  };

  openFile(file: any) {
    var resultType = "";
    var params = "";
    var as = this.getFileType(file.name);
    switch (as) {
      case "stats":
        resultType = "html";
        params += "&stats=true";
        break;

      case "log4j":
        resultType = "html";
        params += "&log4j=true";

      default:
        resultType = as;
        break;
    }

    var URL = this.Misc.getServerPath() + "FileViewerServlet?resultType=" + resultType + "&fileName=" + this.Misc.escapeURL(file.path) + params;
    if (resultType == "xml") {
      window.open(URL, "_blank");
      return;
    }

    this.viewFile = URL.length > 0;

    setTimeout(() => {
      var iframe = angular.element("iframe");

      if (iframe[0]) {
        iframe[0].onload = () => {
          var iframeBody = $((iframe[0] as HTMLIFrameElement).contentWindow!.document.body);
          iframe.css({ "height": iframeBody.height()! + 50 });
        };
      }
    });
  };

  openDirectory(directory: any) {
    var url = "logging";
    if (directory) {
      url = "logging?directory=" + directory;
    }

    this.Api.Get(url, (data) => {
      this.alert = false;
      Object.assign(this, data);
      this.path = data.directory;
      if (data.count > data.list.length) {
        this.alert = "Total number of items [" + data.count + "] exceeded maximum number, only showing first [" + (data.list.length - 1) + "] items!";
      }
    }, (data) => {
      this.alert = (data) ? data.error : "An unknown error occured!";
    });
  };
};

appModule.component('logging', {
  controller: ['Api', 'Misc', '$timeout', '$state', '$stateParams', LoggingController],
  templateUrl: 'js/app/views/logging/logging.component.html'
});

