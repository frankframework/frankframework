import { AfterViewInit, Component, inject, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { LaddaModule } from 'angular2-ladda';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import {
  faChevronDown,
  faChevronUp,
  faExclamationTriangle,
  faRepeat,
  faSearch,
  faTimes,
} from '@fortawesome/free-solid-svg-icons';
import { faArrowAltCircleLeft, faArrowAltCircleDown } from '@fortawesome/free-regular-svg-icons';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

import { Message, MessageField, MessageStore, Note, StorageService } from '../storage.service';
import { StorageListDtComponent } from './storage-list-dt/storage-list-dt.component';
import { SessionService } from '../../../services/session.service';
import { SweetalertService } from '../../../services/sweetalert.service';
import { getProcessStateIcon } from '../../../utilities';
import { AppService } from '../../../app.service';
import {
  DataTableColumn,
  DatatableComponent,
  DataTableDataSource,
  DataTableServerResponseInfo,
} from '../../../components/datatable/datatable.component';
import { KeyValuePipe, NgClass } from '@angular/common';
import { OrderByPipe } from '../../../pipes/orderby.pipe';
import { HasAccessToLinkDirective } from '../../../components/has-access-to-link.directive';
import { DtContentDirective } from '../../../components/datatable/dt-content.directive';
import { DropLastCharPipe } from '../../../pipes/drop-last-char.pipe';
import { SortDirection } from '../../../components/th-sortable.directive';

type FieldSearchInfo = {
  fieldName: string;
  filter: string;
  display: boolean;
};

type SearchColumn = MessageField & FieldSearchInfo;

type MessageData = MessageStore['messages'][number];

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const TEST_API_RESPONSE: DataTableServerResponseInfo<Message> = {
  data: [
    {
      id: '8',
      originalId: 'fallback-message-id-0a0032d6--7568074c_19e73c3cda7_-536d',
      correlationId: 'Test Tool correlation id(4)',
      host: 'mldev-1337qt',
      insertDate: 1_780_061_273_850,
      comment:
        "Pipe [error] <?xml version='1.0' encoding='UTF-8'?>\n<Envelope>\n\t<Header>\n\t\t<MessageID>1</MessageID>\n\t</Header>\n</Envelope>\n",
      position: 1,
    },
    {
      id: '9',
      originalId: 'example2',
      correlationId: 'Test Tool correlation id(4)',
      host: 'mldev-1337qt',
      insertDate: 1_780_061_274_000,
      comment:
        "Pipe [error] <?xml version='1.0' encoding='UTF-8'?>\n<Envelope>\n\t<Header>\n\t\t<MessageID>1</MessageID>\n\t</Header>\n</Envelope>\n",
      position: 2,
    },
    {
      id: '10',
      originalId: 'example3',
      correlationId: 'Test Tool correlation id(4)',
      host: 'mldev-1337qt',
      insertDate: 1_780_061_300_000,
      comment:
        "Pipe [error] <?xml version='1.0' encoding='UTF-8'?>\n<Envelope>\n\t<Header>\n\t\t<MessageID>1</MessageID>\n\t</Header>\n</Envelope>\n",
      position: 3,
    },
  ],
  totalEntries: 3,
  filteredEntries: 3,
  offset: 0,
  size: 3,
};

@Component({
  selector: 'app-storage-list',
  templateUrl: './storage-list.component.html',
  styleUrls: ['./storage-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    NgbAlert,
    OrderByPipe,
    FormsModule,
    LaddaModule,
    RouterLink,
    NgClass,
    HasAccessToLinkDirective,
    DatatableComponent,
    StorageListDtComponent,
    DtContentDirective,
    DropLastCharPipe,
    KeyValuePipe,
    FaIconComponent,
  ],
})
export class StorageListComponent implements OnInit, AfterViewInit, OnDestroy {
  protected targetStates: Record<string, { name: string }> = {};
  protected truncated = false;
  protected truncateButtonText = 'Truncate displayed data';
  protected filterBoxExpanded = false;
  protected messagesProcessing = false;
  protected changingProcessState = false;
  protected searching = false;
  protected clearSearchLadda = false;
  protected messagesDownloading = false;
  protected datasource: DataTableDataSource<MessageData> = new DataTableDataSource<MessageData>();
  protected displayedColumns: DataTableColumn<MessageData>[] = [];
  protected messageFields: SearchColumn[] = [];
  protected sortOptions: { name: string; value: SortDirection }[] = [
    { name: 'Ascending', value: 'ASC' },
    { name: 'Descending', value: 'DESC' },
  ];
  protected sortDirection: SortDirection = 'DESC';
  protected readonly storageService: StorageService = inject(StorageService);
  protected readonly getProcessStateIconFn = getProcessStateIcon;
  protected readonly storageParams = this.storageService.storageParams;
  protected readonly staticMessageFields: SearchColumn[] = [
    { fieldName: 'message', property: 'message', type: 'string', display: true, filter: '', displayName: 'Message' },
    { fieldName: 'startDate', property: 'startDate', type: 'date', display: true, filter: '', displayName: 'From' },
    { fieldName: 'endDate', property: 'endDate', type: 'date', display: true, filter: '', displayName: 'To' },
  ];
  protected readonly faChevronUp = faChevronUp;
  protected readonly faChevronDown = faChevronDown;
  protected readonly faSearch = faSearch;
  protected readonly faTimes = faTimes;
  protected readonly faArrowAltCircleLeft = faArrowAltCircleLeft;
  protected readonly faExclamationTriangle = faExclamationTriangle;
  protected readonly faArrowAltCircleDown = faArrowAltCircleDown;
  protected readonly faRepeat = faRepeat;

  private Session: SessionService = inject(SessionService);
  private SweetAlert: SweetalertService = inject(SweetalertService);
  private appService: AppService = inject(AppService);

  private subscriptions: Subscription = new Subscription();
  private initialDisplayedColumns: DataTableColumn<MessageData>[] = [
    {
      name: 'actions',
      property: null,
      displayName: '',
      className: 'm-b-xxs storage-actions',
      html: true,
    },
    {
      name: 'pos',
      property: 'position',
      displayName: 'No.',
    },
  ];

  ngOnInit(): void {
    this.storageService.closeNotes();
    this.setBreadcrumbs();

    this.datasource.options = {
      filter: false,
      serverSide: true,
      serverSort: 'DESC',
    };

    this.getDisplayedColumns();
    this.setupMessagesRequest();
  }

  ngAfterViewInit(): void {
    const tableTriggerSubscription = this.storageService.tableUpdateTrigger$.subscribe(() =>
      this.datasource.updateTable(),
    );
    this.subscriptions.add(tableTriggerSubscription);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  protected closeNote = (index: number): void => {
    this.storageService.closeNote(index);
  };

  protected getDisplayedColumns(): void {
    const searchSession = this.Session.get<FieldSearchInfo[]>('storageFiltering');
    this.storageService.getStorageFields().subscribe((response) => {
      this.displayedColumns = [
        ...this.initialDisplayedColumns,
        ...response.fields.map<DataTableColumn<MessageData>>((field) => ({
          name: field.property as string,
          property: field.property,
          displayName: field.displayName,
          className: field.type === 'date' ? 'date' : undefined,
          sortable: field.property === 'insertDate',
        })),
      ];
      this.messageFields = response.fields.map((field) => ({ ...field, display: true, filter: '' }));
      if (searchSession) this.setInitialSearchFilters(searchSession);
    });
  }

  protected setupMessagesRequest(): void {
    this.datasource.setServerRequest((requestInfo) => {
      return new Promise((resolve, reject) => {
        let queryParameters = `?max=${requestInfo.size}&skip=${requestInfo.offset}&sort=${requestInfo.sort}`;
        this.updateSessionStorage((column) => {
          if (column.filter !== '') {
            queryParameters += `&${column.property}=${encodeURIComponent(column.filter)}`;
          }
        });

        this.storageService.getStorageList(queryParameters).subscribe({
          next: (response) => {
            this.targetStates = response.targetStates ?? {};
            resolve({
              data: response.messages,
              totalEntries: response.totalMessages,
              filteredEntries: response.recordsFiltered,
              offset: response.skipMessages,
              size: response.messages.length,
            });
            this.searching = false;
            this.clearSearchLadda = false;
            for (const message of response.messages) {
              if (!Object.hasOwn(this.storageService.selectedMessages, message.id)) {
                this.storageService.selectedMessages[message.id] = false;
              }
            }
            for (const messageId in this.storageService.selectedMessages) {
              const messageExists = response.messages.some((message) => message.id === messageId);
              if (!messageExists) {
                delete this.storageService.selectedMessages[messageId];
              }
            }
          },
          error: (error: unknown) => {
            this.searching = false;
            this.clearSearchLadda = false;
            reject(error);
          },
        });
      });
    });
  }

  protected getNotes(): Note[] {
    return this.storageService.notes;
  }

  protected updateSort(): void {
    this.datasource.options.serverSort = this.sortDirection;
  }

  protected searchUpdated(): void {
    this.searching = true;
    this.storageService.updateTable();
  }

  protected truncate(): void {
    this.truncated = !this.truncated;
    this.truncateButtonText = this.truncated ? 'Show original' : 'Truncate displayed data';
    this.storageService.updateTable();
  }

  protected clearSearch(): void {
    this.clearSearchLadda = true;
    this.Session.remove('storageFiltering');

    for (const column of this.messageFields) {
      column.filter = '';
    }
    for (const column of this.staticMessageFields) {
      column.filter = '';
    }
    this.storageService.updateTable();
  }

  protected updateColumnDisplay(column: string): void {
    const displayedColumn = this.displayedColumns.find((displayedColumn) => displayedColumn.name === column);
    const searchColumn = this.messageFields.find((messageField) => messageField.fieldName === column);
    if (displayedColumn && searchColumn) {
      displayedColumn.hidden = !searchColumn.display;
      this.updateSessionStorage();
    }
  }

  protected selectAll(): void {
    for (const index in this.storageService.selectedMessages) {
      this.storageService.selectedMessages[index] = true;
    }
  }

  protected unselectAll(): void {
    for (const index in this.storageService.selectedMessages) {
      this.storageService.selectedMessages[index] = false;
    }
  }

  protected resendMessages(): void {
    const fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.messagesProcessing = true;
      this.storageService.postResendMessages(fd).subscribe({
        next: () => {
          this.messagesProcessing = false;
          this.storageService.addNote('success', 'Selected messages will be reprocessed');
          this.storageService.updateTable();
        },
        error: () => {
          this.messagesProcessing = false;
          this.storageService.addNote('danger', 'Something went wrong, unable to resend all messages!');
          this.storageService.updateTable();
        },
      });
    }
  }

  protected deleteMessages(): void {
    const fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.messagesProcessing = true;
      this.storageService.deleteMessages(fd).subscribe({
        next: () => {
          this.messagesProcessing = false;
          this.storageService.addNote('success', 'Successfully deleted messages');
          this.storageService.updateTable();
        },
        error: () => {
          this.messagesProcessing = false;
          this.storageService.addNote('danger', 'Something went wrong, unable to delete all messages!');
          this.storageService.updateTable();
        },
      });
    }
  }

  protected moveMessages(): void {
    const fd = this.getFormData();
    if (!this.isSelectedMessages(fd)) return;

    this.SweetAlert.warning({
      title: 'Move state of messages',
      text: 'The messages might still be processing in the background. Are you sure you want to move them to Error?',
      confirmButtonText: 'Move to Error',
      cancelButtonText: 'Cancel',
      showCancelButton: true,
    }).then((value) => {
      if (!value.isConfirmed) return;

      this.messagesProcessing = true;
      this.storageService.postMoveMessages(fd).subscribe({
        next: () => {
          this.messagesProcessing = false;
          this.storageService.addNote('success', 'Selected messages will be moved to Error state');
          this.storageService.updateTable();
        },
        error: () => {
          this.messagesProcessing = false;
          this.storageService.addNote('danger', 'Something went wrong, unable to move all messages!');
          this.storageService.updateTable();
        },
      });
    });
  }

  protected downloadMessages(): void {
    const fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.messagesDownloading = true;
      this.storageService.postDownloadMessages(fd).subscribe({
        next: (response) => {
          const blob = new Blob([response], {
            type: 'application/octet-stream',
          });
          const downloadLink = document.createElement('a');
          downloadLink.href = globalThis.URL.createObjectURL(blob);
          downloadLink.setAttribute('download', 'messages.zip');
          document.body.append(downloadLink);
          downloadLink.click();
          downloadLink.remove();
          this.storageService.addNote('success', 'Successfully downloaded messages');
          this.messagesDownloading = false;
        },
        error: () => {
          this.messagesDownloading = false;
          this.storageService.addNote('danger', 'Something went wrong, unable to download selected messages!');
        },
      }); // TODO no intercept
    }
  }

  protected changeProcessState(targetState: string): void {
    const fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.changingProcessState = true;
      this.storageService.postChangeProcessState(fd, targetState).subscribe({
        next: () => {
          this.changingProcessState = false;
          this.storageService.addNote('success', `Successfully changed the state of messages to ${targetState}`);
          this.storageService.updateTable();
        },
        error: () => {
          this.changingProcessState = false;
          this.storageService.addNote('danger', 'Something went wrong, unable to move selected messages!');
          this.storageService.updateTable();
        },
      });
    }
  }

  private getFormData(): FormData {
    const messageIds: string[] = [];
    for (const index in this.storageService.selectedMessages) {
      if (!Object.hasOwn(this.storageService.selectedMessages, index)) continue;

      messageIds.push(index);
      this.storageService.selectedMessages[index] = false; // unset the messageId
    }

    const fd = new FormData();
    fd.append('messageIds', messageIds as unknown as string);
    return fd;
  }

  private setInitialSearchFilters(searchSession: FieldSearchInfo[]): void {
    this.filterBoxExpanded = searchSession.length > 0;
    for (const column of searchSession) {
      const searchColumn = this.messageFields.find((searchColumn) => searchColumn.fieldName === column.fieldName);
      const displayedColumn = this.displayedColumns.find(
        (displayedColumn) => displayedColumn.name === column.fieldName,
      );
      if (searchColumn && displayedColumn) {
        searchColumn.filter = column.filter;
        searchColumn.display = column.display;
        displayedColumn.hidden = !column.display;
      }
    }
  }

  private updateSessionStorage(onColumnUpdate?: (column: SearchColumn) => void): void {
    const searchSession: FieldSearchInfo[] = [];
    const columns: SearchColumn[] = [...this.messageFields, ...this.staticMessageFields];
    for (const column of columns) {
      if (!(column.filter !== '' || !column.display)) continue;
      if (onColumnUpdate) onColumnUpdate(column);
      searchSession.push({
        fieldName: column.fieldName,
        filter: column.filter,
        display: column.display,
      });
    }
    this.Session.set('storageFiltering', searchSession);
  }

  private isSelectedMessages(data: FormData): boolean {
    const selectedMessages = data.get('messageIds') as unknown as string[];
    if (!selectedMessages || selectedMessages.length === 0) {
      this.SweetAlert.warning('No message selected!');
      return false;
    }
    return true;
  }

  private setBreadcrumbs(): void {
    this.appService.customBreadcrumbs(
      `${this.storageParams['adapterName']} > ${
        this.storageParams['storageSource'] == 'pipes' ? 'Pipes' : 'Receivers'
      } > ${this.storageParams['storageSourceName']} > ${this.storageParams['processState']} List`,
    );
  }
}
