export function computeServerPath(): string {
  let path = window.location.pathname;

  if (path.includes('/iaf/gui'))
    path = path.slice(0, Math.max(0, path.indexOf('/iaf/gui') + 1));
  else if (path.includes('/', 1))
    path = path.slice(0, Math.max(0, path.indexOf('/', 1) + 1));
  return path;
}

export function getProcessStateIcon(
  processState: string,
):
  | 'fa-server'
  | 'fa-gears'
  | 'fa-sign-in'
  | 'fa-pause-circle'
  | 'fa-times-circle' {
  switch (processState) {
    case 'Available': {
      return 'fa-server';
    }
    case 'InProcess': {
      return 'fa-gears';
    }
    case 'Done': {
      return 'fa-sign-in';
    }
    case 'Hold': {
      return 'fa-pause-circle';
    }
    // case 'Error':
    default: {
      return 'fa-times-circle';
    }
  }
}

export function getProcessStateIconColor(
  processState: string,
): 'success' | 'warning' | 'danger' {
  switch (processState) {
    case 'Available': {
      return 'success';
    }
    case 'InProcess': {
      return 'success';
    }
    case 'Done': {
      return 'success';
    }
    case 'Hold': {
      return 'warning';
    }
    // case 'Error':
    default: {
      return 'danger';
    }
  }
}

export function copyToClipboard(text: string): void {
  const element = document.createElement('textarea');
  element.value = text;
  element.setAttribute('readonly', '');
  element.style.position = 'absolute';
  element.style.left = '-9999px';
  document.body.append(element);
  element.select();
  document.execCommand('copy'); // TODO: soon deprecated but no real solution yet
  element.remove();
}
