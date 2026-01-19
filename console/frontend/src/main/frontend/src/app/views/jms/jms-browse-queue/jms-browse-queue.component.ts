import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LaddaModule } from 'angular2-ladda';
import { WebStorageService } from 'src/app/services/web-storage.service';
import { ServerErrorResponse } from '../../../app.service';

import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';
import { ToDateDirective } from '../../../components/to-date.directive';
import { JmsBrowseForm, JmsService, Message } from '../jms.service';

@Component({
  selector: 'app-jms-browse-queue',
  imports: [FormsModule, LaddaModule, QuickSubmitFormDirective, ToDateDirective],
  templateUrl: './jms-browse-queue.component.html',
  styleUrls: ['./jms-browse-queue.component.scss'],
})
export class JmsBrowseQueueComponent implements OnInit {
  protected destinationTypes: string[] = ['QUEUE', 'TOPIC'];
  protected form: JmsBrowseForm = {
    destination: '',
    connectionFactory: '',
    type: this.destinationTypes[0],
    rowNumbersOnly: false,
    payload: false,
    lookupDestination: false,
  };
  protected messages: Message[] = [];
  protected numberOfMessages = -1;
  protected processing = false;
  protected error: string | null = null;
  protected connectionFactories: string[] = [];

  private jmsService: JmsService = inject(JmsService);
  private webStorageService: WebStorageService = inject(WebStorageService);

  ngOnInit(): void {
    const browseJmsQueue = this.webStorageService.get<JmsBrowseForm>('browseJmsQueue');
    if (browseJmsQueue) this.form = browseJmsQueue;

    this.jmsService.getJms().subscribe((data) => {
      this.connectionFactories = data['connectionFactories'];
      this.form.connectionFactory = this.connectionFactories[0] ?? '';
    });
  }

  submit(formData: JmsBrowseForm): void {
    if (formData.destination === '') {
      this.error = 'Please specify a connection factory and destination!';
      return;
    }

    this.processing = true;
    this.webStorageService.set('browseJmsQueue', formData);

    this.jmsService.postJmsBrowse(formData).subscribe({
      next: (data) => {
        this.messages = data.messages ?? [];
        this.numberOfMessages = data.numberOfMessages;
        this.error = null;
        this.processing = false;
      },
      error: (errorData: HttpErrorResponse) => {
        try {
          const errorResponse = errorData.error as ServerErrorResponse | undefined;
          this.error = errorResponse ? errorResponse.error : errorData.message;
        } catch {
          this.error = errorData.message;
        }
        this.processing = false;
      },
    });
  }

  reset(): void {
    this.error = null;
    this.webStorageService.remove('browseJmsQueue');
    this.form = {
      destination: '',
      connectionFactory: '',
      type: this.destinationTypes[0],
      rowNumbersOnly: false,
      payload: false,
      lookupDestination: false,
    };
    this.messages = [];
    this.numberOfMessages = -1;
    this.processing = false;
  }
}
