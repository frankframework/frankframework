import { HttpClient } from '@angular/common/http';
import { Component, OnInit, ViewChild } from '@angular/core';
import { Adapter, AppService, Configuration } from 'src/app/app.service';
import { InputFileUploadComponent } from 'src/app/components/input-file-upload/input-file-upload.component';

type FormSessionKey = {
  name: string,
  value: string
}

type AlertState = {
  type: string,
  message: string
}

type PipelineResult = {
  state: string,
  result: string,
  message: string
}

@Component({
  selector: 'app-test-pipeline',
  templateUrl: './test-pipeline.component.html',
  styleUrls: ['./test-pipeline.component.scss']
})
export class TestPipelineComponent implements OnInit {
  configurations: Configuration[] = [];
  adapters: Record<string, Adapter> = {};
  state: AlertState[] = [];
  file: File | null = null;
  selectedConfiguration = "";
  processingMessage = false;
  result = "";

  formSessionKeys: FormSessionKey[] = [
    { name: "", value: "" }
  ];

  form = {
    adapter: "",
    encoding: "",
    message: ""
  }

  @ViewChild(InputFileUploadComponent) formFile!: InputFileUploadComponent;

  constructor(
    private http: HttpClient,
    private appService: AppService
  ){ }

  ngOnInit() {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

    this.adapters = this.appService.adapters;
    this.appService.adapters$.subscribe(() => { this.adapters = this.appService.adapters; });
  }

  addNote(type: string, message: string, removeQueue?: boolean) {
    this.state.push({ type: type, message: message });
  }

  updateSessionKeys(sessionKey: FormSessionKey){
    if (sessionKey.name && sessionKey.name != "" && sessionKey.value && sessionKey.value != ""){
      const keyIndex = this.formSessionKeys.slice(0, -1).findIndex(f => f.name === sessionKey.name);
      if(keyIndex > -1){
        if (this.state.findIndex(f => f.message === "Session keys cannot have the same name!") < 0) //avoid adding it more than once
          this.addNote("warning", "Session keys cannot have the same name!");
        return;
      }

      this.formSessionKeys.push({ name: "", value: "" });
      this.state = [];
    }
  }

  updateFile(file: File | null) {
    this.file = file;
  }

  submit(event: SubmitEvent) {
    event.preventDefault();
    this.result = "";
    this.state = [];
    if (this.selectedConfiguration == "") {
      this.addNote("warning", "Please specify a configuration");
      return;
    }

    let fd = new FormData();
    fd.append("configuration", this.selectedConfiguration);
    if (this.form.adapter && this.form.adapter != "") {
      fd.append("adapter", this.form.adapter);
    } else {
      this.addNote("warning", "Please specify an adapter!");
      return;
    }
    if (this.form.encoding && this.form.encoding != "")
      fd.append("encoding", this.form.encoding);
    if (this.form.message && this.form.message != "") {
      let encoding = (this.form.encoding && this.form.encoding != "") ? ";charset=" + this.form.encoding : "";
      fd.append("message", new Blob([this.form.message], { type: "text/plain" + encoding }), 'message');
    }
    if (this.file)
      fd.append("file", this.file, this.file.name);

    if (this.formSessionKeys.length > 1) {
      this.formSessionKeys.pop();
      const incompleteKeyIndex = this.formSessionKeys.findIndex(f => (f.name === "" || f.value === ""));
      if (incompleteKeyIndex < 0) {
        fd.append("sessionKeys", JSON.stringify(this.formSessionKeys));
      } else {
        this.addNote("warning", "Please make sure all sessionkeys have name and value!");
        return;
      }
    }

    this.processingMessage = true;
    this.http.post<PipelineResult>(this.appService.absoluteApiPath + "test-pipeline", fd).subscribe({ next: (returnData) => {
      let warnLevel = "success";
      if (returnData.state == "ERROR") warnLevel = "danger";
      this.addNote(warnLevel, returnData.state);
      this.result = (returnData.result);
      this.processingMessage = false;
      if (this.file != null) {
        this.formFile.reset();
        this.file = null;
        this.form.message = returnData.message;
      }
    }, error: (errorData) => {
      let error = (errorData && errorData.error) ? errorData.error : "An error occured!";
      this.result = "";
      this.addNote("warning", error);
      this.processingMessage = false;
    }});
  }
}
