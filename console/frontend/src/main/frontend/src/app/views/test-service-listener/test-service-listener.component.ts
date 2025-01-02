import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { Option } from '../../components/combobox/combobox.component';

type AlertState = {
  type: string;
  message: string;
};

type ServiceListenerResult = {
  state: string;
  result: string;
};

@Component({
  selector: 'app-test-service-listener',
  templateUrl: './test-service-listener.component.html',
  styleUrls: ['./test-service-listener.component.scss'],
})
export class TestServiceListenerComponent implements OnInit {
  protected state: AlertState[] = [];
  protected services: Option[] = [];
  protected processingMessage = false;
  protected result = '';

  protected form = {
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

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.http
      .get<{
        services: string[];
      }>(`${this.appService.absoluteApiPath}test-servicelistener`)
      .subscribe((data) => {
        this.services = data.services.map((service) => ({ label: service }));
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
}
