import { Component, Input } from '@angular/core';
import { PartialMessage, StorageService } from '../../storage.service';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HasAccessToLinkDirective } from '../../../../components/has-access-to-link.directive';
import { LaddaModule } from 'angular2-ladda';

@Component({
  selector: 'app-storage-list-dt',
  templateUrl: './storage-list-dt.component.html',
  styleUrls: ['./storage-list-dt.component.scss'],
  imports: [NgIf, FormsModule, RouterLink, HasAccessToLinkDirective, LaddaModule],
})
export class StorageListDtComponent {
  @Input({ required: true }) message!: PartialMessage;

  constructor(public storageService: StorageService) {}
}
