import { Component, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-flow-modal',
  templateUrl: './flow-modal.component.html',
  styleUrls: ['./flow-modal.component.scss']
})
export class FlowModalComponent {

  @Input() adapterName = "";
  @Input() flow = "";

  constructor(
    private activeModal: NgbActiveModal,
  ){ }

  close() {
    this.activeModal.close();
  };
}
