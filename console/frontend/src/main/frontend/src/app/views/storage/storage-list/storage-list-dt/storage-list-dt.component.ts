import { Component, Input } from '@angular/core';
import { PartialMessage, StorageService } from '../../storage.service';

@Component({
  selector: 'app-storage-list-dt',
  templateUrl: './storage-list-dt.component.html',
  styleUrls: ['./storage-list-dt.component.scss'],
  standalone: false,
})
export class StorageListDtComponent {
  @Input({ required: true }) message!: PartialMessage;

  constructor(public storageService: StorageService) {}
}
