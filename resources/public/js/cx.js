// Additional utilties in the vein of HTMX, driven by HTML element attributes.
// cx-toggle : CSS class to toggle on click, either on clicked element, or on cx-target

function handle_cx_toggle(e) {
  let klass = e.currentTarget.getAttribute('cx-toggle');
  let selector = e.currentTarget.getAttribute('cx-target');
  let target = selector ? e.currentTarget.closest(selector) : e.currentTarget;
  target.classList.toggle(klass);
}

function ensure_cx_toggle() {
  document.querySelectorAll('[cx-toggle]').forEach((el) => {
    el.removeEventListener('click', handle_cx_toggle);
    el.addEventListener('click', handle_cx_toggle);
  });
}

function showModal(e) {
  window.modal.showModal();
}

function ensure_show_modal() {
  document.getElementById('modal').removeEventListener('htmx:afterSwap', showModal);
  document.getElementById('modal').addEventListener('htmx:afterSwap', showModal);
}

function stopPropagate(e) {
  e.stopPropagation();
}

function preventDefault(e) {
  e.preventDefault();
}

function apply_stop_propagate_handler() {
  document.querySelectorAll('.stop-propagate').forEach((el) => el.removeEventListener('click', stopPropagate));
  document.querySelectorAll('.stop-propagate').forEach((el) => el.addEventListener('click', stopPropagate));
}

function apply_prevent_default_handler() {
  document.querySelectorAll('.prevent-default').forEach((el) => el.removeEventListener('click', preventDefault));
  document.querySelectorAll('.prevent-default').forEach((el) => el.addEventListener('click', preventDefault));
}

function apply_handlers() {
  ensure_cx_toggle();
  ensure_show_modal();
  apply_stop_propagate_handler();
  apply_prevent_default_handler();
}

addEventListener('DOMContentLoaded', apply_handlers);
addEventListener('htmx:afterSwap', (_) => apply_handlers());
addEventListener('popstate', () => setTimeout(apply_handlers, 0));

// Local Variables:
// js-indent-level: 2
// End:
