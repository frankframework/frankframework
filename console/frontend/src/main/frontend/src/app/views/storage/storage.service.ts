import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';
import { Base64Service } from '../../services/base64.service';

export type MessageStore = {
  totalMessages: number;
  skipMessages: number;
  messageCount: number;
  recordsFiltered: number;
  messages: Message[];
  targetStates?: Record<string, { name: string }>;
};

export type Message = {
  id: string; //StorageId
  originalId: string;
  correlationId: string;
  type: string;
  host: string;
  insertDate: number;
  comment: string;
  message?: string;
  expiryDate?: number;
  label?: string;
  position?: number;
};

export type PartialMessage = {
  id: string;
  resending: boolean;
  deleting: boolean;
};

export type StorageParams = {
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
  baseUrl = '';
  notes: Note[] = [];
  storageParams: StorageParams = {
    adapterName: '',
    configuration: '',
    processState: '',
    storageSource: '',
    storageSourceName: '',
    messageId: null,
  };
  selectedMessages: Record<string, boolean> = {};

  private readonly base64Service: Base64Service = inject(Base64Service);
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);
  private readonly Misc: MiscService = inject(MiscService);

  updateStorageParams(parameters: Partial<StorageParams>): void {
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

  deleteMessage(message: PartialMessage, callback?: (messageId: string) => void): void {
    message.deleting = true;
    const messageId = message.id;
    this.http.delete(`${this.baseUrl}/messages/${this.base64Service.encode(messageId)}`).subscribe({
      next: () => {
        if (callback != undefined && typeof callback == 'function') callback(messageId);
        this.addNote('success', `Successfully deleted message with ID: ${messageId}`);
        this.updateTable();
      },
      error: () => {
        message.deleting = false;
        this.addNote('danger', `Unable to delete messages with ID: ${messageId}`);
        this.updateTable();
      },
    });
  }

  downloadMessage(messageId: string): void {
    window.open(`${this.baseUrl}/messages/${this.base64Service.encode(messageId)}/download`);
  }

  resendMessage(message: PartialMessage, callback?: (messageId: string) => void): void {
    message.resending = true;
    const messageId = message.id;
    this.http.put(`${this.baseUrl}/messages/${this.base64Service.encode(messageId)}`, false).subscribe({
      next: () => {
        if (callback != undefined) callback(message.id);
        this.addNote('success', `Message with ID: ${messageId} will be reprocessed`);
        this.updateTable();
      },
      error: (data: HttpErrorResponse) => {
        message.resending = false;
        data = data.error?.error ?? data.error;
        this.addNote('danger', `Unable to resend message [${messageId}]. ${data}`);
        this.updateTable();
      },
    }); // TODO no intercept
  }

  updateTable(): void {
    for (const index in this.selectedMessages) {
      this.selectedMessages[index] = false;
    }
  }

  getStorageList(queryParameters: string): Observable<MessageStore> {
    return this.http.get<MessageStore>(this.baseUrl + queryParameters);
  }

  getMessage(messageId: string): Observable<Message> {
    return this.http.get<Message>(`${this.baseUrl}/messages/${messageId}`);
  }

  postResendMessages(data: FormData): Observable<object> {
    return this.http.post(this.baseUrl, data);
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
