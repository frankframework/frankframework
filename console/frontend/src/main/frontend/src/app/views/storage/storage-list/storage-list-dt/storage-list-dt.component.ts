import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-storage-list-dt',
  templateUrl: './storage-list-dt.component.html',
  styleUrls: ['./storage-list-dt.component.scss']
})
export class StorageListDtComponent implements OnInit {
  @Input() userData: any;

  constructor() { }

  ngOnInit() {
    console.log(this.userData);
  }
}
