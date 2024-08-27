import { AfterViewInit, Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import type { ADTColumns, ADTSettings } from 'angular-datatables/src/models/settings';
// import { DataTable } from "simple-datatables"
import { Note, StorageService } from '../storage.service';
import { DataTableDirective } from 'angular-datatables';
import { StorageListDtComponent } from './storage-list-dt/storage-list-dt.component';
import { Subject } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { SessionService } from 'src/app/services/session.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { WebStorageService } from 'src/app/services/web-storage.service';
import { getProcessStateIcon } from 'src/app/utils';

type DisplayColumn = {
  id: boolean;
  insertDate: boolean;
  host: boolean;
  originalId: boolean;
  correlationId: boolean;
  comment: boolean;
  expiryDate: boolean;
  label: boolean;
};

@Component({
  selector: 'app-storage-list',
  templateUrl: './storage-list.component.html',
  styleUrls: ['./storage-list.component.scss'],
})
export class StorageListComponent implements OnInit, AfterViewInit {
  // @ViewChild('datatable') dtElement!: ElementRef<HTMLTableElement>;
  @ViewChild(DataTableDirective) dataTable!: DataTableDirective;
  @ViewChild('storageListDt') storageListDt!: TemplateRef<StorageListDtComponent>;
  @ViewChild('dateDt') dateDt!: TemplateRef<string>;

  protected targetStates: Record<string, { name: string }> = {};

  protected truncated = false;
  protected truncateButtonText = 'Truncate displayed data';
  protected filterBoxExpanded = false;

  protected messagesResending = false;
  protected messagesDeleting = false;

  protected changingProcessState = false;

  protected search: Record<string, string> = {};
  protected searching = false;
  protected clearSearchLadda = false;
  protected messagesDownloading = false;
  protected displayColumn: DisplayColumn = {
    id: true,
    insertDate: true,
    host: true,
    originalId: true,
    correlationId: true,
    comment: true,
    expiryDate: true,
    label: true,
  };
  protected dtOptions: ADTSettings = {};
  protected dtTrigger = new Subject<ADTSettings>();

  // service bindings
  protected storageParams = this.storageService.storageParams;
  closeNote = (index: number): void => {
    this.storageService.closeNote(index);
  };
  getProcessStateIconFn = (processState: string): string => {
    return getProcessStateIcon(processState);
  };

  private initialColumns: ADTColumns[] = [
    {
      data: null,
      defaultContent: '',
      className: 'm-b-xxs storageActions',
      orderable: false,
    },
    { name: 'pos', data: 'position', orderable: false, defaultContent: '' },
    { name: 'id', data: 'id', orderable: false, defaultContent: '' },
    {
      name: 'insertDate',
      data: 'insertDate',
      className: 'date',
      defaultContent: '',
    },
    { name: 'host', data: 'host', orderable: false, defaultContent: '' },
    {
      name: 'originalId',
      data: 'originalId',
      orderable: false,
      defaultContent: '',
    },
    {
      name: 'correlationId',
      data: 'correlationId',
      orderable: false,
      defaultContent: '',
    },
    { name: 'comment', data: 'comment', orderable: false, defaultContent: '' },
    {
      name: 'expiryDate',
      data: 'expiryDate',
      className: 'date',
      orderable: false,
      defaultContent: '',
    },
    { name: 'label', data: 'label', orderable: false, defaultContent: '' },
  ];

  constructor(
    private webStorageService: WebStorageService,
    private Session: SessionService,
    private SweetAlert: SweetalertService,
    public storageService: StorageService,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.storageService.closeNotes();

    this.appService.customBreadcrumbs(
      `Adapter > ${
        this.storageParams['storageSource'] == 'pipes' ? `Pipes > ${this.storageParams['storageSourceName']} > ` : ''
      }${this.storageParams['processState']} List`,
    );
    // this.$state.current.data.breadcrumbs = "Adapter > " + (this.$state.params["storageSource"] == 'pipes' ? "Pipes > " + this.$state.params["storageSourceName"] + " > " : "") + this.$state.params["processState"] + " List";

    this.dtOptions = {
      searching: false,
      scrollX: true,
      // bAutoWidth: false,
      autoWidth: false,
      orderCellsTop: true,
      serverSide: true,
      processing: true,
      paging: true,
      lengthMenu: [10, 25, 50, 100, 500, 999],
      order: [[3, 'asc']],
      columns: this.initialColumns,
      // sAjaxDataProp: 'messages',
      ajax: (data, callback): void => {
        const dataRecord = data as Record<string, NonNullable<unknown>>;
        const start = dataRecord['start'];
        const length = dataRecord['length'];
        const order = (dataRecord['order'] as unknown[])[0] as { dir: 'asc' } | { dir: 'desc' };
        const direction = order.dir;

        let queryParameters = `?max=${length}&skip=${start}&sort=${direction}`;
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
            callback({
              draw: dataRecord['draw'],
              recordsTotal: response.totalMessages,
              recordsFiltered: response.recordsFiltered,
              data: response.messages,
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
          error: () => {
            this.searching = false;
            this.clearSearchLadda = false;
          },
        });
      },
    };

    const searchSession = this.Session.get<Record<string, string>>('search');

    this.search = {
      id: searchSession ? searchSession['id'] : '',
      startDate: searchSession ? searchSession['startDate'] : '',
      endDate: searchSession ? searchSession['endDate'] : '',
      host: searchSession ? searchSession['host'] : '',
      messageId: searchSession ? searchSession['messageId'] : '',
      correlationId: searchSession ? searchSession['correlationId'] : '',
      comment: searchSession ? searchSession['comment'] : '',
      label: searchSession ? searchSession['label'] : '',
      message: searchSession ? searchSession['message'] : '',
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
    this.storageService.dtElement = this.dataTable;
    const columns: ADTColumns[] = [...this.initialColumns];

    for (const column of columns) {
      if (column.data === null) {
        column['ngTemplateRef'] = {
          ref: this.storageListDt,
        };
      }
      if (column.className === 'date') {
        column['ngTemplateRef'] = {
          ref: this.dateDt,
          context: {
            captureEvents: (): void => {}, // required for some weird reason
            userData: {
              column: column.data,
            },
          },
        };
      }
    }

    this.dtOptions = {
      ...this.dtOptions,
      stateSave: true,
      stateSaveCallback: (settings, data): void => {
        this.Session.set(`DataTable${this.storageParams.processState}`, data);
      },
      stateLoadCallback: (): void => {
        return this.Session.get(`DataTable${this.storageParams.processState}`);
      },
      columns: columns,
      columnDefs: [
        {
          targets: 0,
          // Targets is index 0 but render function goes over every column
          render: (data, type): unknown => {
            if (type === 'display' && this.truncated) {
              for (const index in data) {
                if (index == 'id') continue;
                const columnData = data[index];
                if (typeof columnData == 'string' && columnData.length > 30) {
                  const title = columnData.replaceAll('"', '&quot;');
                  const leftTrancate = columnData.slice(0, 15);
                  const rightTrancate = columnData.slice(-15);
                  data[index] = `<span title="${title}">${leftTrancate}&#8230;${rightTrancate}</span>`;
                }
              }
            }
            return data;
          },
        },
      ],
    };

    const filterCookie = this.webStorageService.get<DisplayColumn>(`${this.storageParams.processState}Filter`);
    if (filterCookie) {
      for (const column of columns) {
        if (column.name && filterCookie[column.name as keyof DisplayColumn] === false) {
          column.visible = false;
        }
      }
      this.displayColumn = filterCookie;
    } else {
      this.displayColumn = {
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

    // simple-datatables
    /* const table = new DataTable(this.dtElement.nativeElement, {
      // columns: columns,
      searchable: false,
      paging: true,
    });

    table.data = {
      headings: [
        { text: '', data: defaultContent, type: 'html' },
        { text: "pos", data: "position", type: 'string'  },
        { text: "id", data: "messageId", type: 'string' },
        { text: "insertDate", data: "insertDate", type: 'string' },
        { text: "host", data: "host", type: 'string' },
        { text: "originalId", data: "originalId", type: 'string' },
        { text: "correlationId", data: "correlationId", type: 'string' },
        { text: "comment", data: "comment", type: 'string' },
        { text: "expiryDate", data: "expiryDate", type: 'string' },
        { text: "label", data: "label", type: 'string' },
      ],
      data: []
    }; */

    this.dtTrigger.next(this.dtOptions);
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

    this.dataTable.dtInstance.then((table) => {
      const tableColumn = table.column(`${column}:name`);
      if (tableColumn && tableColumn.length == 1)
        tableColumn.visible(this.displayColumn[column as keyof typeof this.displayColumn]);
      table.draw();
    });
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
      this.messagesResending = true;
      this.storageService.postResendMessages(fd).subscribe({
        next: () => {
          this.messagesResending = false;
          this.storageService.addNote('success', 'Selected messages will be reprocessed');
          this.storageService.updateTable();
        },
        error: () => {
          this.messagesResending = false;
          this.storageService.addNote('danger', 'Something went wrong, unable to resend all messages!');
          this.storageService.updateTable();
        },
      });
    }
  }

  deleteMessages(): void {
    const fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.messagesDeleting = true;
      this.storageService.deleteMessages(fd).subscribe({
        next: () => {
          this.messagesDeleting = false;
          this.storageService.addNote('success', 'Successfully deleted messages');
          this.storageService.updateTable();
        },
        error: () => {
          this.messagesDeleting = false;
          this.storageService.addNote('danger', 'Something went wrong, unable to delete all messages!');
          this.storageService.updateTable();
        },
      });
    }
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
