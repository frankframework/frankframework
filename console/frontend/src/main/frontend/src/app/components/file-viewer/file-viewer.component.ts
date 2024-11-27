import { CommonModule } from '@angular/common';
import {
  HttpClient,
  HttpDownloadProgressEvent,
  HttpErrorResponse,
  HttpEventType,
  HttpResponse,
} from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { debounceTime, filter } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';

@Component({
  selector: 'app-file-viewer',
  templateUrl: './file-viewer.component.html',
  styleUrls: ['./file-viewer.component.scss'],
  standalone: true,
  imports: [CommonModule],
})
export class FileViewerComponent implements OnInit {
  @Input()
  fileName: string = '';
  @Input()
  contentType: string = 'text/plain';

  protected fileContents = '';
  protected loading = true;
  protected error = false;

  constructor(
    private http: HttpClient,
    private appService: AppService,
    private miscService: MiscService,
  ) {}

  ngOnInit(): void {
    const requestOptions = {
      headers: { Accept: this.contentType },
      responseType: 'text',
      reportProgress: true,
      observe: 'events',
    } as const;
    this.http
      .request(
        'get',
        `${this.appService.absoluteApiPath}file-viewer?file=${this.miscService.escapeURL(this.fileName)}`,
        requestOptions,
      )
      .pipe(
        filter((event) => event.type === HttpEventType.DownloadProgress || event.type === HttpEventType.Response),
        debounceTime(1000),
      )
      .subscribe({
        next: (event) => {
          if (event.type == HttpEventType.DownloadProgress) {
            const partialDownloadedText = (event as HttpDownloadProgressEvent).partialText;
            this.fileContents = partialDownloadedText ?? '';
          }
          if (event.type === HttpEventType.Response) {
            const response = event as HttpResponse<string>;
            this.fileContents = response.body ?? '<EOF>';
            this.loading = false;
          }
        },
        error: (errorData: HttpErrorResponse) => {
          this.fileContents = `Error requesting file data: ${errorData.message}`;
          this.loading = false;
          this.error = true;
        },
      });
  }
}
