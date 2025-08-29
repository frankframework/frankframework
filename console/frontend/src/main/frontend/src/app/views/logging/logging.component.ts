import { Component, inject, OnInit, QueryList, ViewChildren } from '@angular/core';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { AppService, ServerErrorResponse } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';
import { LoggingFile, LoggingService } from './logging.service';
import { basicTableSort, SortEvent, ThSortableDirective } from 'src/app/components/th-sortable.directive';
import { copyToClipboard } from 'src/app/utilities';
import { HttpErrorResponse } from '@angular/common/http';

import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { SearchFilterPipe } from '../../pipes/search-filter.pipe';
import { FileViewerComponent } from '../../components/file-viewer/file-viewer.component';
import { ToDateDirective } from '../../components/to-date.directive';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowAltCircleLeft, faClipboard } from '@fortawesome/free-regular-svg-icons';
import {
  faArrowCircleRight,
  faSearch,
  faTimes,
  faArrowAltCircleRight,
  faArrowAltCircleDown,
  faFolderOpen,
} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-logging',
  imports: [
    FormsModule,
    NgbAlert,
    ThSortableDirective,
    SearchFilterPipe,
    SearchFilterPipe,
    FileViewerComponent,
    ToDateDirective,
    FaIconComponent,
  ],
  templateUrl: './logging.component.html',
  styleUrls: ['./logging.component.scss'],
})
export class LoggingComponent implements OnInit {
  @ViewChildren(ThSortableDirective) headers!: QueryList<ThSortableDirective>;

  protected viewFile: null | string = null;
  protected alert: boolean | string = false;
  protected path = '';
  protected fileName = '';
  protected sortedlist: LoggingFile[] = [];
  protected readonly faArrowAltCircleRight = faArrowAltCircleRight;
  protected readonly faArrowAltCircleLeft = faArrowAltCircleLeft;
  protected readonly faClipboard = faClipboard;
  protected readonly faTimes = faTimes;
  protected readonly faSearch = faSearch;
  protected readonly faArrowAltCircleDown = faArrowAltCircleDown;
  protected readonly faArrowCircleRight = faArrowCircleRight;
  protected readonly faFolderOpen = faFolderOpen;

  private directory = '';
  private file = '';
  private previousFile: string | null = null;
  private originalList: LoggingFile[] = [];

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private appService = inject(AppService);
  private miscService = inject(MiscService);
  private loggingService = inject(LoggingService);
  ngOnInit(): void {
    this.route.paramMap.subscribe((parameters) => {
      this.handleUrlParameters(parameters);
    });
    this.route.queryParamMap.subscribe((parameters) => {
      this.handleOldUrlParameters(parameters);
    });
    this.route.fragment.subscribe((fragment) => {
      this.previousFile = fragment;
    });
  }

  handleUrlParameters(parameters: ParamMap): void {
    const directoryParameter = parameters.get('directory') ?? '';
    const fileParameter = parameters.get('file') ?? '';

    this.setBreadcrumb(directoryParameter, fileParameter);

    //This is only "" when the user opens the logging page
    const directory = directoryParameter.length > 0 ? directoryParameter : '';
    //The file param is only set when the user copies and pastes an url in their browser
    if (fileParameter.length > 0) {
      this.directory = directory;
      this.file = fileParameter;
      this.path = `${directory}/${fileParameter}`;
      this.viewFile = this.path;
    } else {
      this.openDirectory(directory);
    }
  }

  /**
   * @deprecated Use the parameters instead `/logging/${directoryParameter}/${fileParameter}`
   * */
  handleOldUrlParameters(parameters: ParamMap): void {
    const directoryParameter = parameters.get('directory') ?? '';
    const fileParameter = parameters.get('file') ?? '';

    if (directoryParameter) {
      this.router.navigate(['/logging', directoryParameter, fileParameter]);
    }
  }

  setBreadcrumb(directory: string, file: string): void {
    let breadcrumb = 'Logging > ';
    breadcrumb += directory ? `Show Directory (${directory})` : 'Show Files';
    if (file) {
      breadcrumb += ` > Show File (${file})`;
    }
    this.appService.customBreadcrumbs(breadcrumb);
  }

  closeFile(): void {
    this.viewFile = null;
    this.router.navigate(['/logging', this.directory], { fragment: this.file });
  }

  download(file: LoggingFile): void {
    const contentType = 'application/octet-stream'; // always download instead of possibly display in new tab
    this.openFileNewTab(file, contentType);
  }

  popout(file: LoggingFile | string): void {
    const contentType = 'text/plain';
    this.openFileNewTab(file, contentType);
  }

  open(file: LoggingFile): void {
    if (file.type == 'directory') {
      this.router.navigate(['/logging', file.path]);
    } else {
      this.router.navigate(['/logging', this.directory, file.name]);
    }
  }

  openDirectory(directory: string): void {
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
        if (this.previousFile) {
          setTimeout(() => {
            const element = document.querySelector(`[data-file-name="${this.previousFile}"]`);
            if (element) {
              element.scrollIntoView({ behavior: 'instant' });
            }
          });
        }
      },
      error: (data: HttpErrorResponse) => {
        const errorResponse = data.error as ServerErrorResponse | undefined;
        this.alert = errorResponse ? errorResponse.error : 'An unknown error occurred!';
      },
    });
  }

  copyAndTrimToClipboard(path: string): void {
    const textToCopy = path.trim();
    copyToClipboard(textToCopy);
  }

  onSort(event: SortEvent): void {
    this.sortedlist = basicTableSort(this.originalList, this.headers, event);
  }

  private openFileNewTab(file: LoggingFile | string, contentType: string): void {
    const filePath = typeof file === 'string' ? file : file.path;
    const url = `${this.appService.absoluteApiPath}file-viewer?file=${this.miscService.escapeURL(filePath)}&accept=${contentType}`;
    window.open(url, '_blank');
  }
}
