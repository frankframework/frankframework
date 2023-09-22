import { Injectable } from '@angular/core';
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

export type MessageRuntime = Message & {
  deleting?: boolean;
  resending?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class StorageService {
  baseUrl = "";
  notes: { type: string, message: string }[] = [];
  storageParams = {
    adapterName: "",
    configuration: "",
    processState: "",
    storageSource: "",
    storageSourceName: "",
  }

  constructor(
    private Api: ApiService,
    private Misc: MiscService
  ) { }

  addNote(type: string, message: string) {
    this.notes.push({ type: type, message: message });
  }

  closeNote(index: number) {
    this.notes.splice(index, 1);
  }

  closeNotes() {
    this.notes = [];
  }

  doDeleteMessage(message: MessageRuntime, callback: (messageId: string) => void) {
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

  doResendMessage(message: MessageRuntime, callback?: (messageId: string) => void) {
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
    throw new Error("Method not working yet.");
    // var table = $('#datatable').DataTable();
    // if (table)
    //   table.draw();
  }
}
