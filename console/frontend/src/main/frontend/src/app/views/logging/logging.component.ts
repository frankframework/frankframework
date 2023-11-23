import { Component, OnInit } from '@angular/core';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { MiscService } from 'src/angularjs/app/services/misc.service';
import { StateParams, StateService } from "@uirouter/angularjs";

interface File {
  name: string
  type: string
  path: string
  size: string
  lastModified: string
}

@Component({
  selector: 'app-logging',
  templateUrl: './logging.component.html',
  styleUrls: ['./logging.component.scss']
})
export class LoggingComponent implements OnInit {
  viewFile: boolean = false;
  alert: boolean | string = false;
  directory: string = "";
  path: string = "";
  fileName: string = "";
  list: File[] = [];

  constructor(
    private apiService: ApiService,
    private miscService: MiscService,
    private stateService: StateService,
    private stateParams: StateParams
  ) { };

  ngOnInit(): void {
    //This is only false when the user opens the logging page
    let directory = (this.stateParams['directory'] && this.stateParams['directory'].length > 0) ? this.stateParams['directory'] : false;
    //The file param is only set when the user copy pastes an url in their browser
    if (this.stateParams['file'] && this.stateParams['file'].length > 0) {
      let file = this.stateParams['file'];
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
    this.stateService.transitionTo('pages.logging_show', { directory: this.directory });
  };

  download(file: File) {
    let url = this.miscService.getServerPath() + "FileViewerServlet?resultType=bin&fileName=" + this.miscService.escapeURL(file.path);
    window.open(url, "_blank");
  };

  open(file: File) {
    if (file.type == "directory") {
      this.stateService.transitionTo('pages.logging_show', { directory: file.path });
    } else {
      this.stateService.transitionTo('pages.logging_show', { directory: this.directory, file: file.name }, { notify: false, reload: false });
    };
  };

  getFileType(fileName: string) {
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

  openFile(file: { name: string, path: string }) {
    let resultType = "";
    let params = "";
    let as = this.getFileType(file.name);
    switch (as) {
      case "stats":
        resultType = "html";
        params += "&stats=true";
        break;
      case "log4j":
        resultType = "html";
        params += "&log4j=true";
        break;
      default:
        resultType = as;
        break;
    };

    let URL = this.miscService.getServerPath() + "FileViewerServlet?resultType=" + resultType + "&fileName=" + this.miscService.escapeURL(file.path) + params;
    if (resultType == "xml") {
      window.open(URL, "_blank");
      return;
    };

    this.viewFile = URL.length > 0;

    setTimeout(() => {
      let iframe = angular.element("iframe");

      if (iframe[0]) {
        iframe[0].onload = () => {
          let iframeBody = $((iframe[0] as HTMLIFrameElement).contentWindow!.document.body);
          iframe.css({ "height": iframeBody.height()! + 50 });
        };
      }
    });
  };

  openDirectory(directory: string) {
    let url = "logging";
    if (directory) {
      url = "logging?directory=" + directory;
    }

    this.apiService.Get(url, (data) => {
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

  copyToClipboard(path: string) {
    let textToCopy = path.trim();

    if (textToCopy) {
      let el = document.createElement('textarea');
      el.value = textToCopy;
      el.setAttribute('readonly', '');
      el.style.position = 'absolute';
      el.style.left = '-9999px';
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy');
      document.body.removeChild(el);
    }
  };
}
