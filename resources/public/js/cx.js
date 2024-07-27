// Additional utilties in the vein of HTMX, driven by HTML element attributes.
// cx-toggle : CSS class to toggle on click, either on clicked element, or on cx-target

function handle_cx_toggle(e) {
  let klass = e.currentTarget.getAttribute('cx-toggle');
  let selector = e.currentTarget.getAttribute('cx-target');
  let target = selector ? e.currentTarget.closest(selector) : e.currentTarget;
  target.classList.toggle(klass);
}

function add_handle_cx_toggle() {
  document.querySelectorAll('[cx-toggle]').forEach((el) => {
    el.removeEventListener('click', handle_cx_toggle);
    el.addEventListener('click', handle_cx_toggle);
  });
}

addEventListener('DOMContentLoaded', add_handle_cx_toggle);
addEventListener('htmx:afterSwap', add_handle_cx_toggle);
