import { Component, OnInit } from '@angular/core';
import { Adapter, AppService, Configuration } from 'src/app/app.service';

type FormSessionKey = {
  name?: string,
  value?: string
}

type SessionKey = {
  index: number,
  key: string,
  value: string
}

@Component({
  selector: 'app-test-pipeline',
  templateUrl: './test-pipeline.component.html',
  styleUrls: ['./test-pipeline.component.scss']
})
export class TestPipelineComponent implements OnInit {
  configurations: Configuration[] = [];
  adapters: Record<string, Adapter> = {};
  state: { type: string, message: string }[] = [];
  file: File | null = null;
  selectedConfiguration = "";
  processingMessage = false;
  sessionKeyIndex = 1;
  sessionKeyIndices = [this.sessionKeyIndex];
  sessionKeys = [] as SessionKey[];
  result = "";

  form = {
    adapter: "",
    sessionKeys: [] as FormSessionKey[],
    encoding: "",
    message: ""
  }

  constructor(
    private appService: AppService
  ){ }

  ngOnInit() {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

    this.adapters = this.appService.adapters;
    this.appService.adapters$.subscribe(() => { this.adapters = this.appService.adapters; });


  };

  addNote(type: string, message: string, removeQueue?: boolean) {
    this.state.push({ type: type, message: message });
  };

  initSessionKeys(){
    for(const index in this.sessionKeyIndices){
      this.form.sessionKeys[index] = {};
    }
  }

  updateSessionKeys(sessionKey: FormSessionKey, index: number) {
    let sessionKeyIndex = this.sessionKeys.findIndex(f => f.index === index); // find by index
    if (sessionKeyIndex >= 0) {
      if (sessionKey.name == "" && sessionKey.value == "") { // remove row if row is empty
        this.sessionKeys.splice(sessionKeyIndex, 1);
        this.sessionKeyIndices.splice(sessionKeyIndex, 1);
      } else { // update existing key value pair
        this.sessionKeys[sessionKeyIndex].key = sessionKey.name!;
        this.sessionKeys[sessionKeyIndex].value = sessionKey.value!;
      }
      this.state = [];
    } else if (sessionKey.name && sessionKey.name != "" && sessionKey.value && sessionKey.value != "") {
      let keyIndex = this.sessionKeys.findIndex(f => f.key === sessionKey.name);	// find by key
      // add new key
      if (keyIndex < 0) {
        this.sessionKeyIndex += 1;
        this.sessionKeyIndices.push(this.sessionKeyIndex);
        this.sessionKeys.push({ index: index, key: sessionKey.name, value: sessionKey.value });
        this.state = [];
      } else { // key with the same name already exists show warning
        if (this.state.findIndex(f => f.message === "Session keys cannot have the same name!") < 0) //avoid adding it more than once
          this.addNote("warning", "Session keys cannot have the same name!");
      }
    }
  }

  updateFile(file: File | null) {
    this.file = file;
  };

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

    if (this.sessionKeys.length > 0) {
      let incompleteKeyIndex = this.sessionKeys.findIndex(f => (f.key === "" || f.value === ""));
      if (incompleteKeyIndex < 0) {
        fd.append("sessionKeys", JSON.stringify(this.sessionKeys));
      } else {
        this.addNote("warning", "Please make sure all sessionkeys have name and value!");
        return;
      }
    }

    this.processingMessage = true;
    Api.Post("test-pipeline", fd, function (returnData) {
      let warnLevel = "success";
      if (returnData.state == "ERROR") warnLevel = "danger";
      this.addNote(warnLevel, returnData.state);
      this.result = (returnData.result);
      this.processingMessage = false;
      if (this.file != null) {
        angular.element(".form-file")[0].value = null;
        this.file = null;
        formData.message = returnData.message;
      }
    }, function (errorData) {
      let error = (errorData && errorData.error) ? errorData.error : "An error occured!";
      this.result = "";
      this.addNote("warning", error);
      this.processingMessage = false;
    });
  }
}
