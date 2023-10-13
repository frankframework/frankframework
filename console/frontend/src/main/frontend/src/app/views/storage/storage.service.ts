import { Injectable } from '@angular/core';
import type { DataTableDirective } from 'angular-datatables';
import { type } from 'jquery';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { MiscService } from 'src/angularjs/app/services/misc.service';

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
  }
  selectedMessages: Record<string, boolean> = {};
  dtElement?: DataTableDirective | null;

  constructor(
    private Api: ApiService,
    private Misc: MiscService
  ) { }

  updateStorageParams(params: Partial<StorageParams>) {
    this.storageParams = { ...this.storageParams, ...params };
    this.baseUrl = "configurations/" + this.Misc.escapeURL(this.storageParams.configuration) +
      "/adapters/" + this.Misc.escapeURL(this.storageParams.adapterName) + "/" + this.storageParams.storageSource +
      "/" + this.Misc.escapeURL(this.storageParams.storageSourceName) + "/stores/" + this.storageParams.processState;
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
    let messageId = message.id;
    this.Api.Delete(this.baseUrl + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)), () => {
      if (callback != undefined && typeof callback == 'function')
        callback(messageId);
      this.addNote("success", "Successfully deleted message with ID: " + messageId);
      this.updateTable();
    }, () => {
      message.deleting = false;
      this.addNote("danger", "Unable to delete messages with ID: " + messageId);
      this.updateTable();
    });
  };

  downloadMessage(messageId: string) {
    window.open(this.Misc.getServerPath() + "iaf/api/" + this.baseUrl + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)) + "/download");
  };

  resendMessage(message: PartialMessage, callback?: (messageId: string) => void) {
    message.resending = true;
    let messageId = message.id;
    this.Api.Put(this.baseUrl + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)), false, () => {
      if (callback != undefined)
        callback(message.id);
      this.addNote("success", "Message with ID: " + messageId + " will be reprocessed");
      this.updateTable();
    }, (data) => {
      message.resending = false;
      data = (data.error) ? data.error : data;
      this.addNote("danger", "Unable to resend message [" + messageId + "]. " + data);
      this.updateTable();
    }, false);
  }

  updateTable() {
    this.dtElement?.dtInstance.then(table => table.draw());
    for(const index in this.selectedMessages){
      this.selectedMessages[index] = false;
    }
  }
}
