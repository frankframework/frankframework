import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { InformationModalComponent } from './information-modal.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('InformationModalComponent', () => {
  let component: InformationModalComponent;
  let fixture: ComponentFixture<InformationModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      schemas: [NO_ERRORS_SCHEMA],
      imports: [InformationModalComponent],
      providers: [
        {
          provide: NgbActiveModal,
          useValue: NgbActiveModal,
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InformationModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
