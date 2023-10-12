import { Component, Input, OnInit } from '@angular/core';
import { Message, PartialMessage } from '../../storage.service';

@Component({
  selector: 'app-storage-list-dt',
  templateUrl: './storage-list-dt.component.html',
  styleUrls: ['./storage-list-dt.component.scss']
})
export class StorageListDtComponent implements OnInit {
  @Input() message!: PartialMessage;
  @Input() userData!: any;

  constructor() { }

  ngOnInit() {
    console.log(this.userData);
  }
}
