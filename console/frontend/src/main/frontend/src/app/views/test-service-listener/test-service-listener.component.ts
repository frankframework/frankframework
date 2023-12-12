import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';

type AlertState = {
  type: string,
  message: string
}

type ServiceListenerResult = {
  state: string,
  result: string
}

@Component({
  selector: 'app-test-service-listener',
  templateUrl: './test-service-listener.component.html',
  styleUrls: ['./test-service-listener.component.scss']
})
export class TestServiceListenerComponent implements OnInit {
  state: AlertState[] = [];
  file: File | null = null;
  services: string[] = [];
  processingMessage = false;
  result = "";

  form = {
    service: "",
    encoding: "",
    message: ""
  }

  constructor(
    private http: HttpClient,
    private appService: AppService
  ){ }

  ngOnInit() {
    this.http.get<{ services: string[] }>(this.appService.absoluteApiPath + "test-servicelistener").subscribe((data) => {
      this.services = data.services;
    });
  }

  addNote(type: string, message: string, removeQueue?: boolean) {
    this.state.push({ type: type, message: message });
  }

  updateFile(file: File | null) {
    this.file = file;
  }

  submit(event: SubmitEvent) {
    event.preventDefault();
    this.result = "";
    this.state = [];
    if (this.form.service === "") {
      this.addNote("warning", "Please specify a service and message!");
      return;
    }

    var fd = new FormData();
    if (this.form.service !== "")
      fd.append("service", this.form.service);
    if (this.form.encoding !== "")
      fd.append("encoding", this.form.encoding);
    if (this.form.message !== "") {
      var encoding = (this.form.encoding && this.form.encoding != "") ? ";charset=" + this.form.encoding : "";
      fd.append("message", new Blob([this.form.message], { type: "text/plain" + encoding }), 'message');
    }
    if (this.file)
      fd.append("file", this.file, this.file.name);

    if (this.form.message === "" && !this.file) {
      this.addNote("warning", "Please specify a file or message!");
      return;
    }

    this.processingMessage = true;
    this.http.post<ServiceListenerResult>("test-servicelistener", fd).subscribe({ next: (returnData) => {
      var warnLevel = "success";
      if (returnData.state == "ERROR") warnLevel = "danger";
      this.addNote(warnLevel, returnData.state);
      this.result = (returnData.result);
      this.processingMessage = false;
    }, error: (returnData) => {
      this.result = (returnData.result);
      this.processingMessage = false;
    }});
  }
}
