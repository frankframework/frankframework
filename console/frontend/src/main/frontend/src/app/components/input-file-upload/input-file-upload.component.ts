import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';

@Component({
  selector: 'app-input-file-upload',
  templateUrl: './input-file-upload.component.html',
  styleUrls: ['./input-file-upload.component.scss']
})
export class InputFileUploadComponent {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @Output() onUpdateFile = new EventEmitter<File | null>();
  @Input() accept?: string;
  @Input() title?: string;

  constructor() { }

  onFilesChange(){
    this.handleFile(this.fileInput.nativeElement.files);
  }

  handleFile(files: FileList | null) {
    if (!files || files.length == 0) {
      this.onUpdateFile.emit(null);
      return;
    }
    this.onUpdateFile.emit(files[0]); //Can only parse 1 file!
  }
}
