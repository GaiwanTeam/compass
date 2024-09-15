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
  window.modal.showModal()
}

function ensure_show_modal() {
  document.getElementById("modal").removeEventListener('htmx:afterSwap', showModal)
  document.getElementById("modal").addEventListener('htmx:afterSwap', showModal)
}

function apply_handlers() {
  ensure_cx_toggle()
  ensure_show_modal()
}

function handle_response_errors(err) {
  console.error("HTML response error: ", err)
  var error;
  if (err.detail.xhr.status === 413) {
    error = err.detail.xhr.statusText;
  } else {
    error = err.detail.xhr.response;
  }
  document.getElementById("modal").innerHTML = error;
  showModal();
}

addEventListener('DOMContentLoaded', apply_handlers);
addEventListener('htmx:afterSwap', (_) => apply_handlers());
addEventListener('popstate', () => setTimeout(apply_handlers, 0));
addEventListener("htmx:responseError", handle_response_errors)

// Local Variables:
// js-indent-level: 2
// End:
