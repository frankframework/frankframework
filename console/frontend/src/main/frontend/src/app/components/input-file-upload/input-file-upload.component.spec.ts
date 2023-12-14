import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InputFileUploadComponent } from './input-file-upload.component';

describe('InputFileUploadComponent', () => {
  let component: InputFileUploadComponent;
  let fixture: ComponentFixture<InputFileUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ InputFileUploadComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InputFileUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
