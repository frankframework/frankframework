import { Component, ElementRef, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';
import { LoggingService, LoggingFile } from './logging.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { SortEvent, ThSortableDirective, basicTableSort } from 'src/app/components/th-sortable.directive';

@Component({
  selector: 'app-logging',
  templateUrl: './logging.component.html',
  styleUrls: ['./logging.component.scss']
})
export class LoggingComponent implements OnInit {
  viewFile: boolean | SafeResourceUrl = false;
  alert: boolean | string = false;
  directory: string = "";
  path: string = "";
  fileName: string = "";
  originalList: LoggingFile[] = [];
  sortedlist: LoggingFile[] = [];

  @ViewChild('iframe') iframeRef!: ElementRef<HTMLIFrameElement>;
  @ViewChildren(ThSortableDirective) headers!: QueryList<ThSortableDirective>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private appService: AppService,
    private miscService: MiscService,
    private loggingService: LoggingService,
    private sanitizer: DomSanitizer,
  ) { };

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      const directoryParam = params.get('directory') ?? ""
      const fileParam = params.get('file') ?? ""

      //This is only "" when the user opens the logging page
      let directory = (directoryParam && directoryParam.length > 0) ? directoryParam : "";
      //The file param is only set when the user copy pastes an url in their browser
      if (fileParam && fileParam.length > 0) {
        let file = fileParam;
        this.directory = directory;
        this.path = directory + "/" + file;
        this.openFile({ path: directory + "/" + file, name: file });
      }
      else {
        this.openDirectory(directory);
      }
    });
  };

  closeFile() {
    this.viewFile = false;
    this.router.navigate(['/logging'], { queryParams: { directory: this.directory } });
  };

  download(file: LoggingFile) {
    let url = this.appService.getServerPath() + "FileViewerServlet?resultType=bin&fileName=" + this.miscService.escapeURL(file.path);
    window.open(url, "_blank");
  };

  open(file: LoggingFile) {
    if (file.type == "directory") {
      this.router.navigate(['/logging'], { queryParams: { directory: file.path } });
    } else {
      this.router.navigate(['/logging'], { queryParams: { directory: this.directory, file: file.name } });
    };
  }

  openFile(file: { name: string, path: string }) {
    const URL = this.appService.getServerPath() + "FileViewerServlet?resultType=html&fileName=" + this.miscService.escapeURL(file.path);
    this.viewFile = this.sanitizer.bypassSecurityTrustResourceUrl(URL);

    setTimeout(() => {
      let iframe = this.iframeRef.nativeElement;

      if (iframe) {
        iframe.onload = () => {
          let iframeBody = $(iframe.contentWindow!.document.body);
          $(iframe).css({ "height": iframeBody.height()! + 50 });
        };
      }
    });
  };

  openDirectory(directory: string) {
    this.loggingService.getLogging(directory).subscribe({
      next: (data) => {
        this.alert = false;
        this.originalList = data.list;
        this.sortedlist = data.list;
        this.directory = data.directory;
        this.path = data.directory;
        if (data.count > data.list.length) {
          this.alert = "Total number of items [" + data.count + "] exceeded maximum number, only showing first [" + (data.list.length - 1) + "] items!";
        }
      }, error: (data) => {
        this.alert = (data.error) ? data.error.error : "An unknown error occured!";
      }
    });
  };

  copyToClipboard(path: string) {
    const textToCopy = path.trim();
    this.appService.copyToClipboard(textToCopy);
  };

  onSort(event: SortEvent) {
    this.sortedlist = basicTableSort(this.originalList, this.headers, event);
  }
}
