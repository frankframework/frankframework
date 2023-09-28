import { Component, OnInit } from '@angular/core';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { CookiesService } from 'src/angularjs/app/services/cookies.service';

interface Form {
  destination: string
  connectionFactory: string
  type: string
  rowNumbersOnly: boolean
  payload: boolean
  lookupDestination: boolean
}

interface Message {
  id: string
  correlationId: string
  text: string
  insertDate: string
}

@Component({
  selector: 'app-jms-browse-queue',
  templateUrl: './jms-browse-queue.component.html',
  styleUrls: ['./jms-browse-queue.component.scss']
})
export class JmsBrowseQueueComponent implements OnInit {
  destinationTypes: string[] = ["QUEUE", "TOPIC"];
  form: Form = {
    destination: "",
    connectionFactory: "",
    type: "",
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
    private apiService: ApiService,
    private cookiesService: CookiesService
  ) { };

  ngOnInit(): void {
    var browseJmsQueue = this.cookiesService.get("browseJmsQueue");
    if (browseJmsQueue) this.form = browseJmsQueue;

    this.apiService.Get("jms", (data) => {
      this.connectionFactories = data["connectionFactories"];
      angular.element("select[name='type']").val(this.destinationTypes[0]);
    });
  };

  submit(formData: Form) {
    this.processing = true;

    if (!formData || !formData.destination) {
      this.error = "Please specify a connection factory and destination!";
      return;
    };

    this.cookiesService.set("browseJmsQueue", formData);
    if (!formData.connectionFactory) formData.connectionFactory = this.connectionFactories[0] || "";
    if (!formData.type) formData.type = this.destinationTypes[0] || "";

    this.apiService.Post("jms/browse", JSON.stringify(formData), (data) => {
      this.connectionFactories = data["connectionFactories"];
      if (!data.messages) this.messages = [];
      this.error = "";
      this.processing = false;
    }, (errorData, status, errorMsg) => {
      this.error = (errorData && errorData.error) ? errorData.error : errorMsg;
      this.processing = false;
    });
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
