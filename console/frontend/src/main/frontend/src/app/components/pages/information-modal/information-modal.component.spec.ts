import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { InformationModalComponent } from './information-modal.component';

describe('InformationModalComponent', () => {
  let component: InformationModalComponent;
  let fixture: ComponentFixture<InformationModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, InformationModalComponent],
      providers: [
        {
          provide: NgbActiveModal,
          useValue: NgbActiveModal,
        },
      ],
      schemas: [NO_ERRORS_SCHEMA], // only for directives
    }).compileComponents();

    fixture = TestBed.createComponent(InformationModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
