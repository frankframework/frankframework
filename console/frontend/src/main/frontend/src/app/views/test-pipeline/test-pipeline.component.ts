import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Adapter, AppService, Configuration } from 'src/app/app.service';
import { InputFileUploadComponent } from 'src/app/components/input-file-upload/input-file-upload.component';
import { Subscription } from 'rxjs';
import { Option } from '../../components/combobox/combobox.component';

type FormSessionKey = {
  key: string;
  value: string;
};

export type AlertState = {
  type: string;
  message: string;
};

type PipelineResult = {
  state: string;
  result: string;
  message: string;
};

@Component({
  selector: 'app-test-pipeline',
  templateUrl: './test-pipeline.component.html',
  styleUrls: ['./test-pipeline.component.scss'],
})
export class TestPipelineComponent implements OnInit, OnDestroy {
  @ViewChild(InputFileUploadComponent) formFile!: InputFileUploadComponent;
  protected configurations: Configuration[] = [];
  protected configurationOptions: Option[] = [];
  protected adapters: Record<string, Adapter> = {};
  protected state: AlertState[] = [];
  protected selectedConfiguration = '';
  protected processingMessage = false;
  protected result = '';

  protected formSessionKeys: FormSessionKey[] = [{ key: '', value: '' }];

  protected form = {
    adapter: '',
    encoding: '',
    message: '',
  };

  protected readonly editorActions = {
    ctrlEnter: {
      id: 'submit',
      label: 'Submit Form',
      run: (): void => this.submit(),
    },
  };

  private file: File | null = null;
  private subscriptions: Subscription = new Subscription();

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.setConfigurations();
    const configurationsSubscription = this.appService.configurations$.subscribe(() => {
      this.setConfigurations();
    });
    this.subscriptions.add(configurationsSubscription);

    this.adapters = this.appService.adapters;
    const adaptersSubscription = this.appService.adapters$.subscribe(() => {
      this.adapters = this.appService.adapters;
    });
    this.subscriptions.add(adaptersSubscription);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  private setConfigurations(): void {
    this.configurations = this.appService.configurations;
    this.configurationOptions = this.configurations.map((configuration) => ({
      label: configuration.name,
      description: configuration.type,
    }));
  }

  addNote(type: string, message: string): void {
    this.state.push({ type: type, message: message });
  }

  updateSessionKeys(sessionKey: FormSessionKey): void {
    if (sessionKey?.key != '' && sessionKey?.value != '') {
      const keyIndex = this.formSessionKeys.slice(0, -1).findIndex((f) => f.key === sessionKey.key);
      if (keyIndex > -1) {
        if (this.state.findIndex((f) => f.message === 'Session keys cannot have the same name!') < 0)
          //avoid adding it more than once
          this.addNote('warning', 'Session keys cannot have the same name!');
        return;
      }

      this.formSessionKeys.push({
        key: '',
        value: '',
      });
      this.state = [];
    }
  }

  updateFile(file: File | null): void {
    this.file = file;
  }

  submit(event?: SubmitEvent): void {
    event?.preventDefault();
    this.result = '';
    this.state = [];
    if (this.selectedConfiguration == '') {
      this.addNote('warning', 'Please specify a configuration');
      return;
    }

    const fd = new FormData();
    fd.append('configuration', this.selectedConfiguration);
    if (this.form.adapter && this.form.adapter != '') {
      fd.append('adapter', this.form.adapter);
    } else {
      this.addNote('warning', 'Please specify an adapter!');
      return;
    }
    if (this.form.encoding && this.form.encoding != '') fd.append('encoding', this.form.encoding);
    if (this.file) {
      fd.append('file', this.file, this.file.name);
    } else {
      const encoding = this.form.encoding && this.form.encoding != '' ? `;charset=${this.form.encoding}` : '';
      fd.append('message', new Blob([this.form.message], { type: `text/plain${encoding}` }), 'message');
    }

    if (this.formSessionKeys.length > 1) {
      this.formSessionKeys.pop();
      const incompleteKeyIndex = this.formSessionKeys.findIndex((f) => f.key === '' || f.value === '');

      if (incompleteKeyIndex < 0) {
        fd.append('sessionKeys', JSON.stringify(this.formSessionKeys));
      } else {
        this.addNote('warning', 'Please make sure all sessionkeys have name and value!');
        return;
      }

      this.formSessionKeys.push({
        key: '',
        value: '',
      });
    }

    this.processingMessage = true;
    this.http.post<PipelineResult>(`${this.appService.absoluteApiPath}test-pipeline`, fd).subscribe({
      next: (returnData) => {
        let warnLevel = 'success';
        if (returnData.state == 'ERROR') warnLevel = 'danger';
        this.addNote(warnLevel, returnData.state);
        this.result = returnData.result;
        this.processingMessage = false;
        if (this.file != null) {
          this.formFile.reset();
          this.file = null;
          this.form.message = returnData.message;
        }
      },
      error: (errorData) => {
        const error = errorData.error?.error ?? 'An error occured!';
        this.result = '';
        this.addNote('warning', error);
        this.processingMessage = false;
      },
    });
  }
}
