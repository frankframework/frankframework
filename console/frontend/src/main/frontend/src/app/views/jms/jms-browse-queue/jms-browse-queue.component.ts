import { Component, OnInit } from '@angular/core';
import { WebStorageService } from 'src/app/services/web-storage.service';
import { JmsBrowseForm, JmsService, Message } from '../jms.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ServerErrorResponse } from '../../../app.service';

@Component({
  selector: 'app-jms-browse-queue',
  templateUrl: './jms-browse-queue.component.html',
  styleUrls: ['./jms-browse-queue.component.scss'],
  standalone: false,
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
  protected numberOfMessages: number = -1;
  protected processing: boolean = false;
  protected error: string | null = null;
  protected connectionFactories: string[] = [];

  constructor(
    private jmsService: JmsService,
    private webStorageService: WebStorageService,
  ) {}

  ngOnInit(): void {
    const browseJmsQueue = this.webStorageService.get<JmsBrowseForm>('browseJmsQueue');
    if (browseJmsQueue) this.form = browseJmsQueue;

    this.jmsService.getJms().subscribe((data) => {
      this.connectionFactories = data['connectionFactories'];
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
