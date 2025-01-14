import { Component, inject, Input } from '@angular/core';
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
  @Input({ required: true }) message!: PartialMessage;

  protected storageService: StorageService = inject(StorageService);
  private sweetAlert: SweetalertService = inject(SweetalertService);

  moveMessage(): void {
    this.sweetAlert
      .Warning({
        title: 'Move Message State',
        text: 'Are you sure you want to move this message to Error?',
        confirmButtonText: 'Move to Error',
        cancelButtonText: 'Cancel',
        showCancelButton: true,
      })
      .then((value) => {
        if (value.isConfirmed) {
          this.storageService.moveMessage(this.message);
        }
      });
  }
}
