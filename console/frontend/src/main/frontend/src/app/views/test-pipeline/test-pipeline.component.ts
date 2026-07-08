import { KeyValuePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, Signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { LaddaModule } from 'angular2-ladda';

import { Adapter, AppService, Configuration } from '../../app.service';
import { InputFileUploadComponent } from '../../components/input-file-upload/input-file-upload.component';
import { ComboboxComponent, Option } from '../../components/combobox/combobox.component';
import { ConfigurationFilter } from '../../pipes/configuration-filter.pipe';
import { QuickSubmitFormDirective } from '../../components/quick-submit-form.directive';
import { MonacoEditorComponent } from '../../components/monaco-editor/monaco-editor.component';
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
  sessionKeys: Record<string, string>;
};

@Component({
  selector: 'app-test-pipeline',
  templateUrl: './test-pipeline.component.html',
  styleUrls: ['./test-pipeline.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    NgbAlert,
    QuickSubmitFormDirective,
    ComboboxComponent,
    FormsModule,
    InputFileUploadComponent,
    MonacoEditorComponent,
    LaddaModule,
    KeyValuePipe,
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

  protected formSessionKeys: Record<string, string> = {};
  protected newSessionKey: FormSessionKey = { key: '', value: '' };

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

  protected configurations: Signal<Configuration[]>;

  private file: File | null = null;

  private http: HttpClient = inject(HttpClient);
  private webStorageService: WebStorageService = inject(WebStorageService);
  private appService: AppService = inject(AppService);

  constructor() {
    this.configurations = this.appService.configurations;
  }

  ngOnInit(): void {
    this.setTestPipelineSession();
  }

  protected addNewSessionKey(): void {
    const { key, value } = this.newSessionKey;
    if (Object.hasOwn(this.formSessionKeys, key)) {
      this.addNote('warning', 'Session keys cannot have the same name!');
    } else if (key != '') {
      this.formSessionKeys[key] = value;
      this.state = [];
      this.newSessionKey = { key: '', value: '' };
      setTimeout(() => {
        const sessionKeyElement = document.querySelector(`#sessionKeyValue${key}`) as HTMLInputElement;
        sessionKeyElement?.focus();
      });
    }
  }

  protected updateSessionKey(key: string): void {
    const input = document.querySelector(`#sessionKey${key}`) as HTMLInputElement;
    if (input && key !== input.value) {
      const newKey = input.value;
      if (newKey === '') {
        delete this.formSessionKeys[key];
        return;
      }
      if (Object.hasOwn(this.formSessionKeys, newKey)) {
        this.addNote('warning', 'Session keys cannot have the same name!');
        return;
      }

      this.formSessionKeys[newKey] = this.formSessionKeys[key];
      delete this.formSessionKeys[key];
    }
  }

  protected updateFile(file: File | null): void {
    this.file = file;
  }

  protected submit(event?: SubmitEvent): void {
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

    const sessionKeys = Object.entries(this.formSessionKeys);
    if (sessionKeys.length > 0) {
      const incompleteKeyIndex = sessionKeys.findIndex(([key, value]) => key === '' || value === '');

      if (incompleteKeyIndex === -1) {
        fd.append('sessionKeys', JSON.stringify(sessionKeys.map(([key, value]) => ({ key, value }))));
      } else {
        this.addNote('warning', 'Please make sure all sessionkeys have name and value!');
        return;
      }
    }

    this.processingMessage = true;
    this.webStorageService.set<TestPipelineSession>('testPipeline', {
      configuration: this.selectedConfiguration,
      form: this.form,
      sessionKeys: this.formSessionKeys,
    });
    this.http.post<PipelineResult>(`${this.appService.absoluteApiPath}test-pipeline`, fd).subscribe({
      next: (returnData) => {
        const warnLevel = returnData.state == 'ERROR' ? 'danger' : 'success';
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

  protected reset(): void {
    this.webStorageService.remove('testPipeline');
    this.selectedConfiguration = '';
    this.form = {
      adapter: '',
      encoding: '',
      message: '',
    };
    this.formSessionKeys = {};
    this.file = null;
    this.formFile.reset();
  }

  protected setSelectedConfiguration(): void {
    this.form.adapter = '';
    this.setAdapterOptions(this.selectedConfiguration, this.adapters());
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

  private addNote(type: string, message: string): void {
    this.state.push({ type: type, message: message });
  }
}
