type ToastType = 'success' | 'error' | 'warning' | 'info';

const CONTAINER_ID = 'lexai-toast-container';

function ensureContainer(): HTMLElement {
  let container = document.getElementById(CONTAINER_ID);
  if (container) return container;

  container = document.createElement('div');
  container.id = CONTAINER_ID;
  container.className = 'lexai-toast-container';
  document.body.appendChild(container);
  return container;
}

export function toast(message: string, type: ToastType = 'info', durationMs = 2800) {
  if (!message) return;

  const container = ensureContainer();
  const el = document.createElement('div');
  el.className = `lexai-toast lexai-toast--${type}`;
  el.setAttribute('role', 'status');
  el.textContent = message;

  container.appendChild(el);

  const timer = window.setTimeout(() => {
    el.classList.add('lexai-toast--hide');
    window.setTimeout(() => {
      el.remove();
      if (container.childElementCount === 0) {
        container.remove();
      }
    }, 200);
  }, durationMs);

  el.addEventListener('click', () => {
    window.clearTimeout(timer);
    el.remove();
    if (container.childElementCount === 0) {
      container.remove();
    }
  });
}
