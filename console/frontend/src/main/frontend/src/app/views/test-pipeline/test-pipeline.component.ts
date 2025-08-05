import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, Signal, ViewChild } from '@angular/core';
import { Adapter, AppService, Configuration } from 'src/app/app.service';
import { InputFileUploadComponent } from 'src/app/components/input-file-upload/input-file-upload.component';
import { ComboboxComponent, Option } from '../../components/combobox/combobox.component';
import { ConfigurationFilter } from '../../pipes/configuration-filter.pipe';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';

import { QuickSubmitFormDirective } from '../../components/quick-submit-form.directive';
import { FormsModule } from '@angular/forms';
import { MonacoEditorComponent } from '../../components/monaco-editor/monaco-editor.component';
import { LaddaModule } from 'angular2-ladda';
import { WebStorageService } from '../../services/web-storage.service';

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

type Form = {
  adapter: string;
  encoding: string;
  message: string;
};

type TestPipelineSession = {
  configuration: string;
  form: Form;
  sessionKeys: FormSessionKey[];
};

@Component({
  selector: 'app-test-pipeline',
  templateUrl: './test-pipeline.component.html',
  styleUrls: ['./test-pipeline.component.scss'],
  imports: [
    NgbAlert,
    QuickSubmitFormDirective,
    ComboboxComponent,
    FormsModule,
    InputFileUploadComponent,
    MonacoEditorComponent,
    LaddaModule,
  ],
})
export class TestPipelineComponent implements OnInit {
  @ViewChild(InputFileUploadComponent) formFile!: InputFileUploadComponent;
  protected configurationOptions: Signal<Option[]> = computed(() =>
    this.configurations().map((configuration) => ({ label: configuration.name })),
  );
  protected adapters: Signal<Record<string, Adapter>> = computed(() => {
    const adapters = this.appService.adapters();
    if (this.selectedConfiguration) this.setAdapterOptions(this.selectedConfiguration, adapters);
    return adapters;
  });
  protected adapterOptions: Option[] = [];
  protected state: AlertState[] = [];
  protected selectedConfiguration = '';
  protected processingMessage = false;
  protected result = '';

  protected formSessionKeys: FormSessionKey[] = [{ key: '', value: '' }];

  protected form: Form = {
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

  private http: HttpClient = inject(HttpClient);
  private webStorageService: WebStorageService = inject(WebStorageService);
  private appService: AppService = inject(AppService);
  protected configurations: Signal<Configuration[]> = this.appService.configurations;

  ngOnInit(): void {
    this.setTestPipelineSession();
  }

  private setTestPipelineSession(): void {
    const testPipelineSession = this.webStorageService.get<TestPipelineSession>('testPipeline');
    if (testPipelineSession) {
      this.selectedConfiguration = testPipelineSession.configuration;
      this.form = testPipelineSession.form;
      this.formSessionKeys = testPipelineSession.sessionKeys;
    }
  }

  private setAdapterOptions(selectedConfiguration: string, adapters: Record<string, Adapter>): void {
    const filteredAdapters = ConfigurationFilter(adapters, selectedConfiguration);
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
      if (keyIndex !== -1) {
        if (!this.state.some((f) => f.message === 'Session keys cannot have the same name!'))
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

      if (incompleteKeyIndex === -1) {
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
    this.webStorageService.set<TestPipelineSession>('testPipeline', {
      configuration: this.selectedConfiguration,
      form: this.form,
      sessionKeys: this.formSessionKeys,
    });
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

  reset(): void {
    this.webStorageService.remove('testPipeline');
    this.selectedConfiguration = '';
    this.form = {
      adapter: '',
      encoding: '',
      message: '',
    };
    this.formSessionKeys = [{ key: '', value: '' }];
    this.file = null;
    this.formFile.reset();
  }

  protected setSelectedConfiguration(): void {
    this.form.adapter = '';
    this.setAdapterOptions(this.selectedConfiguration, this.adapters());
  }
}
