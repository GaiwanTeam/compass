// Additional utilties in the vein of HTMX, driven by HTML element attributes.

addEventListener("DOMContentLoaded", (_) => {
    // cx-toggle : CSS class to toggle on click, either on clicked element, or on cx-target
    [...(document.querySelectorAll('[cx-toggle]'))].forEach((el) => {
        let klass    = el.getAttribute('cx-toggle')
        let selector = el.getAttribute('cx-target')
        let target   = selector ? document.querySelector(selector) : el;
        el.addEventListener("click", (_) => target.classList.toggle(klass))
    })
});
