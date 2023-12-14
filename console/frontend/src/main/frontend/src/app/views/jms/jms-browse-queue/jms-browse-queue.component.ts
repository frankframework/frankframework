import { Component, OnInit } from '@angular/core';
import { WebStorageService } from 'src/app/services/web-storage.service';
import { JmsBrowseForm, JmsService, Message } from '../jms.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-jms-browse-queue',
  templateUrl: './jms-browse-queue.component.html',
  styleUrls: ['./jms-browse-queue.component.scss']
})
export class JmsBrowseQueueComponent implements OnInit {
  destinationTypes: string[] = ["QUEUE", "TOPIC"];
  form: JmsBrowseForm = {
    destination: "",
    connectionFactory: "",
    type: "QUEUE",
    rowNumbersOnly: false,
    payload: false,
    lookupDestination: false
  };
  messages: Message[] = [];
  numberOfMessages: number = -1;
  processing: boolean = false;
  error: string = "";
  connectionFactories: string[] = [];

  constructor(
    private jmsService: JmsService,
    private webStorageService: WebStorageService
  ) { };

  ngOnInit(): void {
    var browseJmsQueue = this.webStorageService.get("browseJmsQueue");
    if (browseJmsQueue) this.form = browseJmsQueue;

    this.jmsService.getJms().subscribe(data => {
      this.connectionFactories = data["connectionFactories"];
      $("select[name='type']").val(this.destinationTypes[0]);
    });
  };

  submit(formData: JmsBrowseForm) {
    if (!formData || !formData.destination) {
      this.error = "Please specify a connection factory and destination!";
      return;
    };

    this.processing = true;
    this.webStorageService.set("browseJmsQueue", formData);
    if (!formData.connectionFactory) formData.connectionFactory = this.connectionFactories[0] || "";
    if (!formData.type) formData.type = this.destinationTypes[0] || "";

    this.jmsService.postJmsBrowse(formData).subscribe({ next: data => {
      //this.connectionFactories = data["connectionFactories"]; // doesnt exist in the result?
      this.messages = !data.messages ? [] : data.messages;
      this.numberOfMessages = data.numberOfMessages;
      this.error = "";
      this.processing = false;
    }, error: (errorData: HttpErrorResponse) => {
      const error = errorData.error ? errorData.error.error : "";
      this.error = typeof error === 'object' ? error.error : error;
      this.processing = false;
    }});
  };

  reset() {
    console.log(this.form)
    this.error = "";
    if (!this.form) return;
    if (this.form["destination"]) this.form["destination"] = "";
    if (this.form["rowNumbersOnly"]) this.form["rowNumbersOnly"] = false;
    if (this.form["payload"]) this.form["payload"] = false;
    if (this.form["lookupDestination"]) this.form["lookupDestination"] = false;
    if (this.form["type"]) this.form["type"] = this.destinationTypes[0];

    this.messages = [];
    this.numberOfMessages = -1;
    this.processing = false;
  };
}
