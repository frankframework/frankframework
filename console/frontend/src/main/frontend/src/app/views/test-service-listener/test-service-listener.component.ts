import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { ComboboxComponent, Option } from '../../components/combobox/combobox.component';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';

import { QuickSubmitFormDirective } from '../../components/quick-submit-form.directive';
import { InputFileUploadComponent } from '../../components/input-file-upload/input-file-upload.component';
import { FormsModule } from '@angular/forms';
import { MonacoEditorComponent } from '../../components/monaco-editor/monaco-editor.component';
import { LaddaModule } from 'angular2-ladda';
import { WebStorageService } from '../../services/web-storage.service';

type AlertState = {
  type: string;
  message: string;
};

type ServiceListenerResult = {
  state: string;
  result: string;
};

type Form = {
  service: string;
  encoding: string;
  message: string;
};

@Component({
  selector: 'app-test-service-listener',
  templateUrl: './test-service-listener.component.html',
  styleUrls: ['./test-service-listener.component.scss'],
  imports: [
    NgbAlert,
    QuickSubmitFormDirective,
    ComboboxComponent,
    InputFileUploadComponent,
    FormsModule,
    MonacoEditorComponent,
    LaddaModule,
  ],
})
export class TestServiceListenerComponent implements OnInit {
  protected state: AlertState[] = [];
  protected services: Option[] = [];
  protected processingMessage = false;
  protected result = '';

  protected form: Form = {
    service: '',
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
  private appService: AppService = inject(AppService);
  private webStorageService: WebStorageService = inject(WebStorageService);

  ngOnInit(): void {
    this.http
      .get<{
        services: string[];
      }>(`${this.appService.absoluteApiPath}test-servicelistener`)
      .subscribe((data) => {
        this.services = data.services.map((service) => ({ label: service }));

        const testServiceListenerSession = this.webStorageService.get<Form>('testServiceListener');
        if (testServiceListenerSession) this.form = testServiceListenerSession;
      });
  }

  addNote(type: string, message: string): void {
    this.state.push({ type: type, message: message });
  }

  updateFile(file: File | null): void {
    this.file = file;
  }

  submit(event?: SubmitEvent): void {
    event?.preventDefault();
    this.result = '';
    this.state = [];
    if (this.form.service === '') {
      this.addNote('warning', 'Please specify a service and message!');
      return;
    }

    const fd = new FormData();
    if (this.form.service !== '') fd.append('service', this.form.service);
    if (this.form.encoding !== '') fd.append('encoding', this.form.encoding);
    if (this.form.message !== '') {
      const encoding = this.form.encoding && this.form.encoding != '' ? `;charset=${this.form.encoding}` : '';
      fd.append('message', new Blob([this.form.message], { type: `text/plain${encoding}` }), 'message');
    }
    if (this.file) fd.append('file', this.file, this.file.name);

    if (this.form.message === '' && !this.file) {
      this.addNote('warning', 'Please specify a file or message!');
      return;
    }

    this.processingMessage = true;
    this.webStorageService.set<Form>('testServiceListener', this.form);
    this.http.post<ServiceListenerResult>(`${this.appService.absoluteApiPath}test-servicelistener`, fd).subscribe({
      next: (returnData) => {
        let warnLevel = 'success';
        if (returnData.state == 'ERROR') warnLevel = 'danger';
        this.addNote(warnLevel, returnData.state);
        this.result = returnData.result;
        this.processingMessage = false;
      },
      error: (returnData) => {
        this.result = returnData.result;
        this.processingMessage = false;
      },
    });
  }

  reset(): void {
    this.webStorageService.remove('testServiceListener');
    this.form = {
      service: '',
      encoding: '',
      message: '',
    };
    this.file = null;
  }
}
