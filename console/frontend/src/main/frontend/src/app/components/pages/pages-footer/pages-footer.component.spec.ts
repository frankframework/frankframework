import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { PagesFooterComponent } from './pages-footer.component';

describe('PagesFooterComponent', () => {
  let component: PagesFooterComponent;
  let fixture: ComponentFixture<PagesFooterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PagesFooterComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(PagesFooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
