import { Component, OnInit } from '@angular/core';
import { JmsService } from '../jms.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ServerErrorResponse } from '../../../app.service';

interface Form {
  destination: string;
  replyTo: string;
  message: string;
  persistent: boolean;
  propertyValue: string;
  propertyKey: string;
  type: string;
  connectionFactory: string;
  synchronous: boolean;
  lookupDestination: boolean;
  encoding: string;
}

@Component({
  selector: 'app-jms-send-message',
  templateUrl: './jms-send-message.component.html',
  styleUrls: ['./jms-send-message.component.scss'],
})
export class JmsSendMessageComponent implements OnInit {
  protected destinationTypes: string[] = ['QUEUE', 'TOPIC'];
  protected processing: boolean = false;
  protected file: File | null = null;
  protected connectionFactories: string[] = [];
  protected error: string | null = null;
  protected successMessage: string = '';
  protected form: Form = {
    destination: '',
    replyTo: '',
    message: '',
    persistent: false,
    propertyValue: '',
    propertyKey: '',
    type: this.destinationTypes[0],
    connectionFactory: '',
    synchronous: false,
    lookupDestination: false,
    encoding: '',
  };

  protected readonly editorActions = {
    ctrlEnter: {
      id: 'submit',
      label: 'Submit Form',
      run: (): void => this.submit(this.form),
    },
  };

  constructor(private jmsService: JmsService) {}

  ngOnInit(): void {
    this.jmsService.getJms().subscribe((data) => {
      this.connectionFactories = data['connectionFactories'];
    });
  }

  submit(formData: Form): void {
    this.processing = true;
    this.successMessage = '';
    if (!formData) return;

    const fd = new FormData();
    if (formData.connectionFactory && formData.connectionFactory != '')
      fd.append('connectionFactory', formData.connectionFactory);
    else fd.append('connectionFactory', this.connectionFactories[0]);
    if (formData.destination && formData.destination != '') fd.append('destination', formData.destination);
    if (formData.type && formData.type != '') fd.append('type', formData.type);
    else fd.append('type', this.destinationTypes[0]);
    if (formData.replyTo && formData.replyTo != '') fd.append('replyTo', formData.replyTo);
    if (formData.persistent) fd.append('persistent', formData.persistent.toString());
    if (formData.synchronous) fd.append('synchronous', formData.synchronous.toString());
    if (formData.lookupDestination) fd.append('lookupDestination', formData.lookupDestination.toString());

    if (formData.propertyKey && formData.propertyKey != '' && formData.propertyValue && formData.propertyValue != '')
      fd.append('property', `${formData.propertyKey},${formData.propertyValue}`);
    if (formData.message && formData.message != '') {
      const encoding = formData.encoding && formData.encoding != '' ? `;charset=${formData.encoding}` : '';
      fd.append('message', new Blob([formData.message], { type: `text/plain${encoding}` }), 'message');
    }
    if (this.file) fd.append('file', this.file as unknown as Blob, this.file['name']);
    if (formData.encoding && formData.encoding != '') fd.append('encoding', formData.encoding);

    if (!formData.message && !this.file) {
      this.error = 'Please specify a file or message!';
      this.processing = false;
      return;
    }

    this.jmsService.postJmsMessage(fd).subscribe({
      next: () => {
        this.error = null;
        this.processing = false;
        this.successMessage = 'JMS Message sent successfully';
      },
      error: (errorData: HttpErrorResponse) => {
        this.processing = false;
        try {
          const errorResponse = JSON.parse(errorData.error) as ServerErrorResponse | undefined;
          this.error = errorResponse ? errorResponse.error : 'An unknown error occured, check the logs for more info.';
        } catch {
          this.error = errorData.message;
        }
      },
    });
  }

  reset(): void {
    this.error = null;
    this.successMessage = '';
    this.form = {
      destination: '',
      replyTo: '',
      message: '',
      persistent: false,
      propertyValue: '',
      propertyKey: '',
      type: this.destinationTypes[0],
      connectionFactory: '',
      synchronous: false,
      lookupDestination: false,
      encoding: '',
    };
  }
}
