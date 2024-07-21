// Additional utilties in the vein of HTMX, driven by HTML element attributes.

// cx-toggle : CSS class to toggle on click, either on clicked element, or on cx-target
function handle_cx_toggle(e) {
    [...(document.querySelectorAll('[cx-toggle]'))].forEach((el) => {
        let klass    = el.getAttribute('cx-toggle')
        let selector = el.getAttribute('cx-target')
        let target   = selector ? el.closest(selector) : el;
        el.addEventListener("click", (_) => target.classList.toggle(klass))
    })
}
addEventListener("DOMContentLoaded", handle_cx_toggle);
addEventListener("htmx:afterSwap", handle_cx_toggle);
