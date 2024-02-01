import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import type { DataTableDirective } from 'angular-datatables';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';

export type MessageStore = {
  totalMessages: number;
  skipMessages: number;
  messageCount: number;
  recordsFiltered: number;
  messages: Message[];
  targetStates?: Record<string, { name: string; }>;
}

export type Message = {
  id: string; //MessageId
  originalId: string; //Made up Id?
  correlationId: string;
  type: string;
  host: string;
  insertDate: number;
  comment: string;
  message: string;
  expiryDate?: number;
  label?: string;
  position?: number;
}

export type PartialMessage = {
  id: string;
  resending: boolean;
  deleting: boolean;
}

export type StorageParams = {
  adapterName: string,
  configuration: string,
  processState: string,
  storageSource: string,
  storageSourceName: string,
  messageId: string | null
}

@Injectable({
  providedIn: 'root'
})
export class StorageService {
  baseUrl = "";
  notes: { type: string, message: string }[] = [];
  storageParams: StorageParams = {
    adapterName: "",
    configuration: "",
    processState: "",
    storageSource: "",
    storageSourceName: "",
    messageId: null
  }
  selectedMessages: Record<string, boolean> = {};
  dtElement?: DataTableDirective | null;

  constructor(
    private http: HttpClient,
    private appService: AppService,
    private Misc: MiscService
  ) { }

  updateStorageParams(params: Partial<StorageParams>) {
    this.storageParams = Object.assign(this.storageParams, params); // dont make this a new object
    this.baseUrl = this.appService.absoluteApiPath + "configurations/" + this.Misc.escapeURL(this.storageParams.configuration) +
      "/adapters/" + this.Misc.escapeURL(this.storageParams.adapterName) + "/" + this.storageParams.storageSource +
      "/" + this.Misc.escapeURL(this.storageParams.storageSourceName) + "/stores/" + this.storageParams.processState;

    setTimeout(() => {
      this.appService.updateTitle(this.storageParams.processState + " List");
    }, 0);
  }

  addNote(type: string, message: string) {
    this.notes.push({ type: type, message: message });
  }

  closeNote(index: number) {
    this.notes.splice(index, 1);
  }

  closeNotes() {
    this.notes = [];
  }

  deleteMessage(message: PartialMessage, callback?: (messageId: string) => void) {
    message.deleting = true;
    const messageId = message.id;
    this.http.delete(this.baseUrl + "/messages/" + encodeURIComponent(encodeURIComponent(messageId))).subscribe({ next: () => {
      if (callback != undefined && typeof callback == 'function')
        callback(messageId);
      this.addNote("success", "Successfully deleted message with ID: " + messageId);
      this.updateTable();
    }, error: () => {
      message.deleting = false;
      this.addNote("danger", "Unable to delete messages with ID: " + messageId);
      this.updateTable();
    }});
  };

  downloadMessage(messageId: string) {
    window.open(this.baseUrl + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)) + "/download");
  };

  resendMessage(message: PartialMessage, callback?: (messageId: string) => void) {
    message.resending = true;
    const messageId = message.id;
    this.http.put(this.baseUrl + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)), false).subscribe({ next: () => {
      if (callback != undefined)
        callback(message.id);
      this.addNote("success", "Message with ID: " + messageId + " will be reprocessed");
      this.updateTable();
    }, error: (data: HttpErrorResponse) => {
      message.resending = false;
      data = (data.error?.error) ? data.error.error : data.error;
      this.addNote("danger", "Unable to resend message [" + messageId + "]. " + data);
      this.updateTable();
    }}); // TODO no intercept
  }

  updateTable() {
    this.dtElement?.dtInstance.then(table => table.draw());
    for(const index in this.selectedMessages){
      this.selectedMessages[index] = false;
    }
  }

  getStorageList(queryParams: string){
    return this.http.get<MessageStore>(this.baseUrl + queryParams);
  }

  getMessage(messageId: string){
    return this.http.get<Message>(this.baseUrl + "/messages/" + messageId);
  }

  postResendMessages(data: FormData){
    return this.http.post(this.baseUrl, data);
  }

  postDownloadMessages(data: FormData){
    return this.http.post(this.baseUrl + "/messages/download", data, { responseType: 'blob' });
  }

  postChangeProcessState(data: FormData, targetState: string){
    return this.http.post(this.baseUrl + "/move/" + targetState, data)
  }

  deleteMessages(data: FormData){
    return this.http.delete(this.baseUrl, { body: data });
  }
}
