import { AfterViewInit, Component, inject, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { MessageStore, Note, StorageService } from '../storage.service';
import { StorageListDtComponent } from './storage-list-dt/storage-list-dt.component';
import { SessionService } from 'src/app/services/session.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { WebStorageService } from 'src/app/services/web-storage.service';
import { getProcessStateIcon } from 'src/app/utils';
import { AppService } from '../../../app.service';
import {
  DataTableColumn,
  DatatableComponent,
  DataTableDataSource,
} from '../../../components/datatable/datatable.component';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';
import { KeyValuePipe, NgClass } from '@angular/common';
import { OrderByPipe } from '../../../pipes/orderby.pipe';
import { FormsModule } from '@angular/forms';
import { LaddaModule } from 'angular2-ladda';
import { RouterLink } from '@angular/router';
import { HasAccessToLinkDirective } from '../../../components/has-access-to-link.directive';
import { DtContentDirective } from '../../../components/datatable/dt-content.directive';
import { DropLastCharPipe } from '../../../pipes/drop-last-char.pipe';
import { Subscription } from 'rxjs';

type DisplayColumn = {
  actions: boolean;
  id: boolean;
  insertDate: boolean;
  host: boolean;
  originalId: boolean;
  correlationId: boolean;
  comment: boolean;
  expiryDate: boolean;
  label: boolean;
};

type MessageData = MessageStore['messages'][number];

@Component({
  selector: 'app-storage-list',
  templateUrl: './storage-list.component.html',
  styleUrls: ['./storage-list.component.scss'],
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
  ],
})
export class StorageListComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('storageListDt') storageListDt!: TemplateRef<StorageListDtComponent>;

  protected targetStates: Record<string, { name: string }> = {};

  protected truncated = false;
  protected truncateButtonText = 'Truncate displayed data';
  protected filterBoxExpanded = false;

  protected messagesProcessing = false;
  protected changingProcessState = false;

  protected search: Record<string, string> = {};
  protected searching = false;
  protected clearSearchLadda = false;
  protected messagesDownloading = false;
  protected displayColumn: DisplayColumn = {
    actions: true,
    id: true,
    insertDate: true,
    host: true,
    originalId: true,
    correlationId: true,
    comment: true,
    expiryDate: true,
    label: true,
  };

  // service bindings
  protected storageService: StorageService = inject(StorageService);
  protected storageParams = this.storageService.storageParams;
  closeNote = (index: number): void => {
    this.storageService.closeNote(index);
  };
  getProcessStateIconFn = (processState: string): string => {
    return getProcessStateIcon(processState);
  };

  protected datasource: DataTableDataSource<MessageData> = new DataTableDataSource<MessageData>();
  protected displayedColumns: DataTableColumn<MessageData>[] = [
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
    { name: 'id', property: 'id', displayName: 'Storage ID' },
    {
      name: 'insertDate',
      property: 'insertDate',
      displayName: 'Timestamp',
      className: 'date',
    },
    { name: 'host', property: 'host', displayName: 'Host' },
    {
      name: 'originalId',
      property: 'originalId',
      displayName: 'Original ID',
    },
    {
      name: 'correlationId',
      property: 'correlationId',
      displayName: 'Correlation ID',
    },
    { name: 'comment', property: 'comment', displayName: 'Comment' },
    {
      name: 'expiryDate',
      property: 'expiryDate',
      displayName: 'Expires',
      className: 'date',
    },
    { name: 'label', property: 'label', displayName: 'Label' },
  ];

  private webStorageService: WebStorageService = inject(WebStorageService);
  private Session: SessionService = inject(SessionService);
  private SweetAlert: SweetalertService = inject(SweetalertService);
  private appService: AppService = inject(AppService);

  private subscriptions: Subscription = new Subscription();

  ngOnInit(): void {
    this.storageService.closeNotes();

    this.appService.customBreadcrumbs(
      `Adapter > ${
        this.storageParams['storageSource'] == 'pipes' ? `Pipes > ${this.storageParams['storageSourceName']} > ` : ''
      }${this.storageParams['processState']} List`,
    );

    this.datasource.options = {
      filter: false,
      serverSide: true,
    };

    this.setupMessagesRequest();

    const searchSession = this.Session.get<Record<string, string>>('search');

    this.search = searchSession
      ? {
          id: searchSession['id'],
          startDate: searchSession['startDate'],
          endDate: searchSession['endDate'],
          host: searchSession['host'],
          messageId: searchSession['messageId'],
          correlationId: searchSession['correlationId'],
          comment: searchSession['comment'],
          label: searchSession['label'],
          message: searchSession['message'],
        }
      : {
          id: '',
          startDate: '',
          endDate: '',
          host: '',
          messageId: '',
          correlationId: '',
          comment: '',
          label: '',
          message: '',
        };

    const search = this.search;
    if (search) {
      for (const column in search) {
        const value = search[column as keyof typeof search];
        if (value && value != '') {
          this.filterBoxExpanded = true;
        }
      }
    }
  }

  ngAfterViewInit(): void {
    const filterCookie = this.webStorageService.get<DisplayColumn>(`${this.storageParams.processState}Filter`);
    if (filterCookie) {
      for (const column of this.displayedColumns) {
        if (column.name && !filterCookie[column.name as keyof DisplayColumn]) {
          column.hidden = true;
        }
      }
      this.displayColumn = filterCookie;
    } else {
      this.displayColumn = {
        actions: true,
        id: true,
        insertDate: true,
        host: true,
        originalId: true,
        correlationId: true,
        comment: true,
        expiryDate: true,
        label: true,
      };
    }

    const tableTriggerSubscription = this.storageService.tableUpdateTrigger$.subscribe(() =>
      this.datasource.updateTable(),
    );
    this.subscriptions.add(tableTriggerSubscription);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  setupMessagesRequest(): void {
    this.datasource.setServerRequest(
      (requestInfo) =>
        new Promise((resolve, reject) => {
          let queryParameters = `?max=${requestInfo.size}&skip=${requestInfo.offset}&sort=${requestInfo.sort}`;
          const search = this.search;
          const searchSession: Record<keyof typeof search, string> = {};
          for (const column in search) {
            const text = search[column as keyof typeof search];
            if (text) {
              queryParameters += `&${column}=${text}`;
              searchSession[column as keyof typeof search] = text;
            }
          }
          this.Session.set('search', searchSession);

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
                if (!(message.id in this.storageService.selectedMessages)) {
                  this.storageService.selectedMessages[message.id] = false;
                }
              }
              for (const messageId in this.storageService.selectedMessages) {
                const messageExists = response.messages.some((message) => message.id === messageId);
                if (!messageExists) {
                  delete this.storageService.selectedMessages[messageId];
                }
              }
              console.log(
                'Table properties:',
                this.displayedColumns.map((column) => column.property),
              );
              console.log('MessageBrowser Fields:', response.fields);
            },
            error: (error: unknown) => {
              this.searching = false;
              this.clearSearchLadda = false;
              reject(error);
            },
          });
        }),
    );
  }

  getNotes(): Note[] {
    return this.storageService.notes;
  }

  getFormData(): FormData {
    const messageIds: string[] = [];
    for (const index in this.storageService.selectedMessages) {
      if (this.storageService.selectedMessages[index]) {
        messageIds.push(index);
        this.storageService.selectedMessages[index] = false; //unset the messageId
      }
    }

    const fd = new FormData();
    fd.append('messageIds', messageIds as unknown as string);
    return fd;
  }

  searchUpdated(): void {
    this.searching = true;
    this.storageService.updateTable();
  }

  truncate(): void {
    this.truncated = !this.truncated;
    this.truncateButtonText = this.truncated ? 'Show original' : 'Truncate displayed data';
    this.storageService.updateTable();
  }

  clearSearch(): void {
    this.clearSearchLadda = true;
    this.Session.remove('search');
    this.search = {};
    this.storageService.updateTable();
  }

  updateFilter(column: string): void {
    this.webStorageService.set(`${this.storageParams.processState}Filter`, this.displayColumn);

    const tableColumn = this.displayedColumns.find((displayedColumn) => displayedColumn.name === column);
    if (tableColumn) {
      tableColumn.hidden = !this.displayColumn[column as keyof typeof this.displayColumn];
    }
  }

  selectAll(): void {
    for (const index in this.storageService.selectedMessages) {
      this.storageService.selectedMessages[index] = true;
    }
  }

  unselectAll(): void {
    for (const index in this.storageService.selectedMessages) {
      this.storageService.selectedMessages[index] = false;
    }
  }

  resendMessages(): void {
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

  deleteMessages(): void {
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

  moveMessages(): void {
    const fd = this.getFormData();
    if (!this.isSelectedMessages(fd)) return;

    this.SweetAlert.Warning({
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

  downloadMessages(): void {
    const fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.messagesDownloading = true;
      this.storageService.postDownloadMessages(fd).subscribe({
        next: (response) => {
          const blob = new Blob([response], {
            type: 'application/octet-stream',
          });
          const downloadLink = document.createElement('a');
          downloadLink.href = window.URL.createObjectURL(blob);
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

  changeProcessState(targetState: string): void {
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

  isSelectedMessages(data: FormData): boolean {
    const selectedMessages = data.get('messageIds') as unknown as string[];
    if (!selectedMessages || selectedMessages.length === 0) {
      this.SweetAlert.Warning('No message selected!');
      return false;
    } else {
      return true;
    }
  }
}
