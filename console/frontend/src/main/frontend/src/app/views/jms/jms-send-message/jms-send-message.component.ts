import { Component, OnInit } from '@angular/core';
import { JmsService } from '../jms.service';
import { HttpErrorResponse } from '@angular/common/http';

interface Form {
  destination: string
  replyTo: string
  message: string
  persistent: boolean
  propertyValue: string
  propertyKey: string
  type: string
  connectionFactory: string
  synchronous: boolean
  lookupDestination: boolean
  encoding: string
}

@Component({
  selector: 'app-jms-send-message',
  templateUrl: './jms-send-message.component.html',
  styleUrls: ['./jms-send-message.component.scss']
})
export class JmsSendMessageComponent implements OnInit {
  destinationTypes: string[] = ["QUEUE", "TOPIC"];
  processing: boolean = false;
  file: File | null = null;
  connectionFactories: string[] = [];
  error: string = "";
  form: Form = {
    destination: "",
    replyTo: "",
    message: "",
    persistent: false,
    propertyValue: "",
    propertyKey: "",
    type: "QUEUE",
    connectionFactory: "",
    synchronous: false,
    lookupDestination: false,
    encoding: ""
  };

  constructor(
    private jmsService: JmsService
  ) { };

  ngOnInit(): void {
    this.jmsService.getJms().subscribe(data => { this.connectionFactories = data["connectionFactories"]; });
  };

  submit(formData: Form) {
    this.processing = true;
    if (!formData) return;

    var fd = new FormData();
    if (formData.connectionFactory && formData.connectionFactory != "")
      fd.append("connectionFactory", formData.connectionFactory);
    else
      fd.append("connectionFactory", this.connectionFactories[0]);
    if (formData.destination && formData.destination != "")
      fd.append("destination", formData.destination);
    if (formData.type && formData.type != "")
      fd.append("type", formData.type);
    else
      fd.append("type", this.destinationTypes[0]);
    if (formData.replyTo && formData.replyTo != "")
      fd.append("replyTo", formData.replyTo);
    if (formData.persistent)
      fd.append("persistent", formData.persistent.toString());
    if (formData.synchronous)
      fd.append("synchronous", formData.synchronous.toString());
    if (formData.lookupDestination)
      fd.append("lookupDestination", formData.lookupDestination.toString());

    if (formData.propertyKey && formData.propertyKey != "" && formData.propertyValue && formData.propertyValue != "")
      fd.append("property", formData.propertyKey + "," + formData.propertyValue);
    if (formData.message && formData.message != "") {
      var encoding = (formData.encoding && formData.encoding != "") ? ";charset=" + formData.encoding : "";
      fd.append("message", new Blob([formData.message], { type: "text/plain" + encoding }), 'message');
    }
    if (this.file)
      fd.append("file", this.file as any, this.file['name']);
    if (formData.encoding && formData.encoding != "")
      fd.append("encoding", formData.encoding);

    if (!formData.message && !this.file) {
      this.error = "Please specify a file or message!";
      this.processing = false;
      return;
    }

    this.jmsService.postJmsMessage(fd).subscribe({ next: (returnData) => {
      this.error = "";
      this.processing = false;
    }, error: (errorData: HttpErrorResponse) => {
      this.processing = false;
      const error = errorData.error ? errorData.error.error : "An unknown error occured, check the logs for more info.";
      this.error = typeof error === 'object' ? error.error : error;
    }});
  };

  reset() {
    this.error = "";
    if (!this.form) return;
    if (this.form["destination"])
      this.form["destination"] = "";
    if (this.form["replyTo"])
      this.form["replyTo"] = "";
    if (this.form["message"])
      this.form["message"] = "";
    if (this.form["persistent"])
      this.form["persistent"] = false;
    if (this.form["propertyValue"])
      this.form["propertyValue"] = "";
    if (this.form["propertyKey"])
      this.form["propertyKey"] = "";
    if (this.form["type"])
      this.form["type"] = this.destinationTypes[0];
  };
}
