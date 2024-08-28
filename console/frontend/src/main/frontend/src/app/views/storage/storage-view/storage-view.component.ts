import { Component, OnInit } from '@angular/core';
import { Message, StorageService, PartialMessage, Note } from '../storage.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-storage-view',
  templateUrl: './storage-view.component.html',
  styleUrls: ['./storage-view.component.scss'],
})
export class StorageViewComponent implements OnInit {
  protected message: PartialMessage = {
    id: '0', //this.$state.params["messageId"],
    resending: false,
    deleting: false,
  };
  protected metadata?: Message = {
    id: '',
    originalId: '',
    correlationId: '',
    type: '',
    host: '',
    insertDate: 0,
    comment: 'Loading...',
    message: 'Loading...',
  };

  // service bindings
  protected storageParams = this.storageService.storageParams;
  closeNote = (index: number): void => {
    this.storageService.closeNote(index);
  };
  downloadMessage = (messageId: string): void => {
    this.storageService.downloadMessage(messageId);
  };

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private SweetAlert: SweetalertService,
    private storageService: StorageService,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.storageService.closeNotes();

    this.appService.customBreadcrumbs(
      `Adapter > ${
        this.storageParams['storageSource'] == 'pipes' ? `Pipes > ${this.storageParams['storageSourceName']} > ` : ''
      }${this.storageParams['processState']} List > View Message ${this.storageParams['messageId']}`,
    );
    // this.$state.current.data.breadcrumbs = "Adapter > " + (this.$state.params["storageSource"] == 'pipes' ? "Pipes > " + this.$state.params["storageSourceName"] + " > " : "") + this.$state.params["processState"] + " List > View Message " + this.$state.params["messageId"];

    if (!this.storageParams.messageId) {
      this.SweetAlert.Warning('Invalid URL', 'No message id provided!');
      return;
    }

    this.message.id = this.storageParams.messageId;

    this.storageService.getMessage(encodeURIComponent(encodeURIComponent(this.storageParams.messageId))).subscribe({
      next: (data) => {
        this.metadata = data;
      },
      error: (errorData: HttpErrorResponse) => {
        const error = errorData.error ? errorData.error.error : errorData.message;
        if (errorData.status == 500) {
          this.SweetAlert.Warning(
            'An error occured while opening the message',
            `message id [${this.message.id}] error [${error}]`,
          );
        } else {
          this.SweetAlert.Warning('Message not found', `message id [${this.message.id}] error [${error}]`);
        }
        this.goBack();
      },
    });
  }

  getNotes(): Note[] {
    return this.storageService.notes;
  }

  resendMessage(message: PartialMessage): void {
    this.storageService.resendMessage(message, () => {
      //Go back to the storage list if successful
      this.goBack();
    });
  }

  deleteMessage(message: PartialMessage): void {
    this.storageService.deleteMessage(message, () => {
      //Go back to the storage list if successful
      this.goBack();
    });
  }

  goBack(): void {
    this.router.navigate(['../..'], { relativeTo: this.route });
  }
}
