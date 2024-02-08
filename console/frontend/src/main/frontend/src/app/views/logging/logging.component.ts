import { Component, OnInit, QueryList, ViewChildren } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';
import { LoggingService, LoggingFile } from './logging.service';
import { SortEvent, ThSortableDirective, basicTableSort } from 'src/app/components/th-sortable.directive';

@Component({
  selector: 'app-logging',
  templateUrl: './logging.component.html',
  styleUrls: ['./logging.component.scss']
})
export class LoggingComponent implements OnInit {
  viewFile: null | string = null;
  alert: boolean | string = false;
  directory: string = '';
  path: string = '';
  fileName: string = '';
  originalList: LoggingFile[] = [];
  sortedlist: LoggingFile[] = [];

  @ViewChildren(ThSortableDirective) headers!: QueryList<ThSortableDirective>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private appService: AppService,
    private miscService: MiscService,
    private loggingService: LoggingService,
  ) { };

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      const directoryParam = params.get('directory') ?? '';
      const fileParam = params.get('file') ?? '';

      //This is only "" when the user opens the logging page
      const directory = (directoryParam && directoryParam.length > 0) ? directoryParam : '';
      //The file param is only set when the user copy pastes an url in their browser
      if (fileParam && fileParam.length > 0) {
        const file = fileParam;
        this.directory = directory;
        this.path = `${directory}/${file}`;
        this.viewFile = this.path;
      }
      else {
        this.openDirectory(directory);
      }
    });
  };

  closeFile() {
    this.viewFile = null;
    this.router.navigate(['/logging'], { queryParams: { directory: this.directory } });
  };

  download(file: LoggingFile) {
    const url = `${this.appService.absoluteApiPath}file-viewer?file=${this.miscService.escapeURL(file.path)}`;
    window.open(url, '_blank');
  };

  open(file: LoggingFile) {
    if (file.type == "directory") {
      this.router.navigate(['/logging'], { queryParams: { directory: file.path } });
    } else {
      this.router.navigate(['/logging'], { queryParams: { directory: this.directory, file: file.name } });
    };
  }

  openDirectory(directory: string) {
    this.loggingService.getLogging(directory).subscribe({
      next: (data) => {
        this.alert = false;
        this.originalList = data.list;
        this.sortedlist = data.list;
        this.directory = data.directory;
        this.path = data.directory;
        if (data.count > data.list.length) {
          this.alert = `Total number of items [${data.count}] exceeded maximum number, only showing first [${data.list.length - 1}] items!`;
        }
      }, error: (data) => {
        this.alert = data.error?.error || 'An unknown error occured!';
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
