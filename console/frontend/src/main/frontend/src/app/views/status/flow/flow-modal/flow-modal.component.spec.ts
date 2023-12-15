import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FlowModalComponent } from './flow-modal.component';

describe('FlowModalComponent', () => {
  let component: FlowModalComponent;
  let fixture: ComponentFixture<FlowModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ FlowModalComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FlowModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
