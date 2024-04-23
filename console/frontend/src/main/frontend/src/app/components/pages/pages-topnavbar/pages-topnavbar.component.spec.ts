import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { PagesTopnavbarComponent } from './pages-topnavbar.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('PagesTopnavbarComponent', () => {
  let component: PagesTopnavbarComponent;
  let fixture: ComponentFixture<PagesTopnavbarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, PagesTopnavbarComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(PagesTopnavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
