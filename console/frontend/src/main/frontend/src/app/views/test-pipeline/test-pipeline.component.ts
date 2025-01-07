import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Adapter, AppService, Configuration } from 'src/app/app.service';
import { InputFileUploadComponent } from 'src/app/components/input-file-upload/input-file-upload.component';
import { Subscription } from 'rxjs';
import { ComboboxComponent, Option } from '../../components/combobox/combobox.component';
import { ConfigurationFilter } from '../../pipes/configuration-filter.pipe';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';
import { NgForOf, NgIf } from '@angular/common';
import { QuickSubmitFormDirective } from '../../components/quick-submit-form.directive';
import { FormsModule } from '@angular/forms';
import { MonacoEditorComponent } from '../../components/monaco-editor/monaco-editor.component';
import { LaddaModule } from 'angular2-ladda';

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
  imports: [
    NgbAlert,
    NgForOf,
    QuickSubmitFormDirective,
    ComboboxComponent,
    FormsModule,
    InputFileUploadComponent,
    MonacoEditorComponent,
    LaddaModule,
    NgIf,
  ],
})
export class TestPipelineComponent implements OnInit, OnDestroy {
  @ViewChild(InputFileUploadComponent) formFile!: InputFileUploadComponent;
  protected configurations: Configuration[] = [];
  protected configurationOptions: Option[] = [];
  protected adapters: Record<string, Adapter> = {};
  protected adapterOptions: Option[] = [];
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

    this.setAdapters();
    const adaptersSubscription = this.appService.adapters$.subscribe(() => {
      this.setAdapters();
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
    }));
  }

  private setAdapters(): void {
    this.adapters = this.appService.adapters;
  }

  private setAdapterOptions(selectedConfiguration: string): void {
    const filteredAdapters = ConfigurationFilter(this.adapters, selectedConfiguration);
    this.adapterOptions = Object.entries(filteredAdapters).map(([, adapter]) => ({
      label: adapter.name,
      description: adapter.description ?? '',
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

  protected setSelectedConfiguration(): void {
    this.form.adapter = '';
    this.setAdapterOptions(this.selectedConfiguration);
  }
}
