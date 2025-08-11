import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';
import { Base64Service } from '../../services/base64.service';

export type MessageStore = {
  fields: MessageField[];
  totalMessages: number;
  skipMessages: number;
  messageCount: number;
  recordsFiltered: number;
  messages: Message[];
  targetStates?: Record<string, { name: string }>;
};

export type Message = {
  [key: string]: unknown;
  id: string; //StorageId
  insertDate?: number;
};

export type PartialMessage = {
  id: string;
  processing: boolean;
};

export type MessageField = {
  fieldName: string;
  property: keyof Message;
  displayName: string;
  type: string;
};

export type StorageMetadata = {
  fields: MessageField[];
};

export type StorageParameters = {
  adapterName: string;
  configuration: string;
  processState: string;
  storageSource: string;
  storageSourceName: string;
  messageId: string | null;
};

export type Note = {
  type: string;
  message: string;
};

@Injectable({
  providedIn: 'root',
})
export class StorageService {
  public notes: Note[] = [];
  public storageParams: StorageParameters = {
    adapterName: '',
    configuration: '',
    processState: '',
    storageSource: '',
    storageSourceName: '',
    messageId: null,
  };
  public selectedMessages: Record<string, boolean> = {};
  public tableUpdateTrigger$: Observable<void>;

  private readonly base64Service: Base64Service = inject(Base64Service);
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);
  private readonly Misc: MiscService = inject(MiscService);

  private baseUrl = '';
  private tableUpdateTriggerSubject = new Subject<void>();

  constructor() {
    this.tableUpdateTrigger$ = this.tableUpdateTriggerSubject.asObservable();
  }

  updateStorageParams(parameters: Partial<StorageParameters>): void {
    this.storageParams = Object.assign(this.storageParams, parameters); // dont make this a new object
    this.baseUrl = `${this.appService.absoluteApiPath}configurations/${this.Misc.escapeURL(
      this.storageParams.configuration,
    )}/adapters/${this.Misc.escapeURL(this.storageParams.adapterName)}/${
      this.storageParams.storageSource
    }/${this.Misc.escapeURL(this.storageParams.storageSourceName)}/stores/${this.storageParams.processState}`;

    setTimeout(() => {
      this.appService.updateTitle(`${this.storageParams.processState} List`);
    }, 0);
  }

  addNote(type: string, message: string): void {
    this.notes.push({ type: type, message: message });
  }

  closeNote(index: number): void {
    this.notes.splice(index, 1);
  }

  closeNotes(): void {
    this.notes = [];
  }

  deleteMessage(message: PartialMessage, callback?: () => void): void {
    message.processing = true;
    const messageId = message.id;
    this.http.delete(`${this.baseUrl}/messages/${this.base64Service.encode(messageId)}`).subscribe({
      next: () => {
        if (callback != undefined && typeof callback == 'function') callback();
        this.addNote('success', `Successfully deleted message with ID: ${messageId}`);
        this.updateTable();
      },
      error: () => {
        message.processing = false;
        this.addNote('danger', `Unable to delete messages with ID: ${messageId}`);
        this.updateTable();
      },
    });
  }

  downloadMessage(messageId: string): void {
    window.open(`${this.baseUrl}/messages/${this.base64Service.encode(messageId)}/download`);
  }

  resendMessage(message: PartialMessage, callback?: () => void): void {
    message.processing = true;
    const messageId = message.id;
    this.http.put(`${this.baseUrl}/messages/${this.base64Service.encode(messageId)}`, false).subscribe({
      next: () => {
        if (callback != undefined) callback();
        message.processing = false;
        this.addNote('success', `Message with ID: ${messageId} will be reprocessed`);
        this.updateTable();
      },
      error: (data: HttpErrorResponse) => {
        message.processing = false;
        data = data.error?.error ?? data.error;
        this.addNote('danger', `Unable to resend message [${messageId}]. ${data}`);
        this.updateTable();
      },
    }); // TODO no intercept
  }

  moveMessage(message: PartialMessage, callback?: () => void): void {
    message.processing = true;
    const messageId = message.id;
    const data = new FormData();
    data.set('messageIds', messageId);
    this.http.post(`${this.baseUrl}/move/Error`, data).subscribe({
      next: () => {
        if (callback != undefined) callback();
        message.processing = false;
        this.addNote('success', `Message with ID: ${messageId} will be moved to Error state`);
        this.updateTable();
      },
      error: (data: HttpErrorResponse) => {
        message.processing = false;
        data = data.error?.error ?? data.error;
        this.addNote('danger', `Unable to move message [${messageId}]. ${data}`);
        this.updateTable();
      },
    }); // TODO no intercept
  }

  updateTable(): void {
    for (const index in this.selectedMessages) {
      this.selectedMessages[index] = false;
    }
    this.tableUpdateTriggerSubject.next();
  }

  getStorageList(queryParameters: string): Observable<MessageStore> {
    return this.http.get<MessageStore>(this.baseUrl + queryParameters);
  }

  getStorageFields(): Observable<StorageMetadata> {
    return this.http.get<StorageMetadata>(`${this.baseUrl}/fields`);
  }

  getMessage(messageId: string): Observable<Message> {
    return this.http.get<Message>(`${this.baseUrl}/messages/${messageId}`);
  }

  postResendMessages(data: FormData): Observable<object> {
    return this.http.post(this.baseUrl, data);
  }

  postMoveMessages(data: FormData): Observable<object> {
    return this.http.post(`${this.baseUrl}/move/Error`, data);
  }

  postDownloadMessages(data: FormData): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/messages/download`, data, {
      responseType: 'blob',
    });
  }

  postChangeProcessState(data: FormData, targetState: string): Observable<object> {
    return this.http.post(`${this.baseUrl}/move/${targetState}`, data);
  }

  deleteMessages(data: FormData): Observable<object> {
    return this.http.delete(this.baseUrl, { body: data });
  }
}
