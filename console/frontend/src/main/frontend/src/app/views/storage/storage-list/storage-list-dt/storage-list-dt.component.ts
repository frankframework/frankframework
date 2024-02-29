import { Component, Input } from '@angular/core';
import { PartialMessage, StorageService } from '../../storage.service';

@Component({
  selector: 'app-storage-list-dt',
  templateUrl: './storage-list-dt.component.html',
  styleUrls: ['./storage-list-dt.component.scss'],
})
export class StorageListDtComponent {
  @Input() message!: PartialMessage;
  @Input() userData!: unknown;

  constructor(public storageService: StorageService) {}
}
