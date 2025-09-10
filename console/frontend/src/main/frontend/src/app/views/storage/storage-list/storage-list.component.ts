import { AfterViewInit, Component, inject, OnDestroy, OnInit } from '@angular/core';
import { MessageField, MessageStore, Note, StorageService } from '../storage.service';
import { StorageListDtComponent } from './storage-list-dt/storage-list-dt.component';
import { SessionService } from 'src/app/services/session.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
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
import { SortDirection } from '../../../components/th-sortable.directive';

type FieldSearchInfo = {
  fieldName: string;
  filter: string;
  display: boolean;
};

type SearchColumn = MessageField & FieldSearchInfo;

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
  protected targetStates: Record<string, { name: string }> = {};

  protected truncated = false;
  protected truncateButtonText = 'Truncate displayed data';
  protected filterBoxExpanded = false;

  protected messagesProcessing = false;
  protected changingProcessState = false;
  protected searching = false;
  protected clearSearchLadda = false;
  protected messagesDownloading = false;

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
  protected displayedColumns: DataTableColumn<MessageData>[] = [];
  protected messageFields: SearchColumn[] = [];
  protected sortOptions: { name: string; value: SortDirection }[] = [
    { name: 'Ascending', value: 'ASC' },
    { name: 'Descending', value: 'DESC' },
  ];
  protected sortDirection: SortDirection = 'DESC';

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

  protected getDisplayedColumns(): void {
    const searchSession = this.Session.get<FieldSearchInfo[]>('storageFiltering');
    this.storageService.getStorageFields().subscribe((response) => {
      this.displayedColumns = [
        ...this.initialDisplayedColumns,
        ...response.fields.map<DataTableColumn<MessageData>>((field) => ({
          name: field.fieldName ?? field.property,
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
    this.datasource.setServerRequest(
      (requestInfo) =>
        new Promise((resolve, reject) => {
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

  setInitialSearchFilters(searchSession: FieldSearchInfo[]): void {
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

  updateSort(): void {
    this.datasource.options.serverSort = this.sortDirection;
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
    this.Session.remove('storageFiltering');

    for (const column of this.messageFields) {
      column.filter = '';
    }
    this.storageService.updateTable();
  }

  updateColumnDisplay(column: string): void {
    const displayedColumn = this.displayedColumns.find((displayedColumn) => displayedColumn.name === column);
    const searchColumn = this.messageFields.find((messageField) => messageField.fieldName === column);
    if (displayedColumn && searchColumn) {
      displayedColumn.hidden = !searchColumn.display;
      this.updateSessionStorage();
    }
  }

  updateSessionStorage(onColumnUpdate?: (column: SearchColumn) => void): void {
    const searchSession: FieldSearchInfo[] = [];
    for (const column of this.messageFields) {
      if (column.filter !== '' || !column.display) {
        if (onColumnUpdate) onColumnUpdate(column);
        searchSession.push({
          fieldName: column.fieldName,
          filter: column.filter,
          display: column.display,
        });
      }
    }
    this.Session.set('storageFiltering', searchSession);
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

  private setBreadcrumbs(): void {
    this.appService.customBreadcrumbs(
      `${this.storageParams['adapterName']} > ${
        this.storageParams['storageSource'] == 'pipes' ? 'Pipes' : 'Receivers'
      } > ${this.storageParams['storageSourceName']} > ${this.storageParams['processState']} List`,
    );
  }
}
