import { Component, inject, OnInit } from '@angular/core';
import { Message, Note, PartialMessage, StorageService } from '../storage.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { Base64Service } from '../../../services/base64.service';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';

import { OrderByPipe } from '../../../pipes/orderby.pipe';
import { ToDateDirective } from '../../../components/to-date.directive';
import { HasAccessToLinkDirective } from '../../../components/has-access-to-link.directive';
import { LaddaModule } from 'angular2-ladda';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowAltCircleDown, faArrowAltCircleLeft } from '@fortawesome/free-regular-svg-icons';
import { faRepeat, faTimes } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-storage-view',
  templateUrl: './storage-view.component.html',
  styleUrls: ['./storage-view.component.scss'],
  imports: [NgbAlert, OrderByPipe, ToDateDirective, HasAccessToLinkDirective, LaddaModule, FaIconComponent],
})
export class StorageViewComponent implements OnInit {
  protected message: PartialMessage = {
    id: '0', //this.$state.params["messageId"],
    processing: false,
  };
  protected data: Message = {
    id: 'Loading...',
  };
  protected extraProperties: string[] = [];
  protected storageParams;
  protected readonly faArrowAltCircleLeft = faArrowAltCircleLeft;
  protected readonly faRepeat = faRepeat;
  protected readonly faTimes = faTimes;
  protected readonly faArrowAltCircleDown = faArrowAltCircleDown;

  private readonly storageService: StorageService = inject(StorageService);
  private readonly base64Service: Base64Service = inject(Base64Service);
  private readonly router: Router = inject(Router);
  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly SweetAlert: SweetalertService = inject(SweetalertService);
  private readonly appService: AppService = inject(AppService);
  private skipProperties: string[] = ['id', 'insertDate', 'correlationId', 'comment', 'message'];

  constructor() {
    this.storageParams = this.storageService.storageParams;
  }

  // service bindings
  closeNote = (index: number): void => {
    this.storageService.closeNote(index);
  };
  downloadMessage = (messageId: string): void => {
    this.storageService.downloadMessage(messageId);
  };

  ngOnInit(): void {
    this.storageService.closeNotes();
    this.setBreadcrumbs();

    if (!this.storageParams.messageId) {
      this.SweetAlert.warning('Invalid URL', 'No message id provided!');
      return;
    }

    this.message.id = this.storageParams.messageId;

    this.storageService.getMessage(this.base64Service.encode(this.storageParams.messageId)).subscribe({
      next: (data) => {
        this.data = data;
        this.extraProperties = Object.keys(data).filter((key) => !this.skipProperties.includes(key));
      },
      error: (errorData: HttpErrorResponse) => {
        const error = errorData.error ? errorData.error.error : errorData.message;
        if (errorData.status == 500) {
          this.SweetAlert.warning(
            'An error occured while opening the message',
            `message id [${this.message.id}] error [${error}]`,
          );
        } else {
          this.SweetAlert.warning('Message not found', `message id [${this.message.id}] error [${error}]`);
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

  private setBreadcrumbs(): void {
    this.appService.customBreadcrumbs(
      `${this.storageParams['adapterName']} > ${
        this.storageParams['storageSource'] == 'pipes' ? 'Pipes' : 'Receivers'
      } > ${this.storageParams['storageSourceName']} > ${this.storageParams['processState']} List > View Message ${
        this.storageParams['messageId']
      }`,
    );
  }
}
