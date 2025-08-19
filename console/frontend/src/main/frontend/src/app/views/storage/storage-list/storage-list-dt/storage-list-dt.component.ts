import { Component, inject, input } from '@angular/core';
import { PartialMessage, StorageService } from '../../storage.service';

import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HasAccessToLinkDirective } from '../../../../components/has-access-to-link.directive';
import { LaddaModule } from 'angular2-ladda';
import { SweetalertService } from '../../../../services/sweetalert.service';

@Component({
  selector: 'app-storage-list-dt',
  templateUrl: './storage-list-dt.component.html',
  styleUrls: ['./storage-list-dt.component.scss'],
  imports: [FormsModule, RouterLink, HasAccessToLinkDirective, LaddaModule],
})
export class StorageListDtComponent {
  public readonly message = input<PartialMessage>();

  protected readonly storageService: StorageService = inject(StorageService);
  protected resendAction = false;

  private readonly sweetAlert: SweetalertService = inject(SweetalertService);

  moveMessage(message: PartialMessage): void {
    this.sweetAlert
      .warning({
        title: 'Move Message State',
        text: 'The message might still be processing in the background. Are you sure you want to move it to Error?',
        confirmButtonText: 'Move to Error',
        cancelButtonText: 'Cancel',
        showCancelButton: true,
      })
      .then((value) => {
        if (value.isConfirmed) {
          this.storageService.moveMessage(message);
        }
      });
  }

  resendMessage(message: PartialMessage): void {
    this.resendAction = true;
    this.storageService.resendMessage(message);
  }

  deleteMessage(message: PartialMessage): void {
    this.resendAction = false;
    this.storageService.deleteMessage(message);
  }
}
