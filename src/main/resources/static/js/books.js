/*
    This file contains the application specific JavaScript for the Books application.
    Because the application is an MPA, there is only a little bit of JavaScript required
    to add some interactivity and integrate a few 3rd party JS components.

    In many ways (particularly to support "locality of behaviour"), it would have been nice
    to keep this JavaScript inline in the pages it is used. However, as one of the aims of
    this demo project is to show HTMX being used with a Content Security Policy that disallows
    inline scripts, it is all kept in this single external file.
*/


// Utility function to initialize a Swiper instance
function initialiseSwiper(evt) {
    const swiper = new Swiper('.mySwiper', {
        effect: 'coverflow',
        grabCursor: true,
        centeredSlides: true,
        slidesPerView: 'auto',
        initialSlide: 0,
        coverflowEffect: {
            rotate: 20,
            stretch: 10,
            depth: 30,
            modifier: 1,
            slideShadows: false,
        },
        loop: true,
        navigation: {
            nextEl: '.swiper-button-next',
            prevEl: '.swiper-button-prev',
        }
    });
}

// Initialize Swiper if the element exists
if (document.getElementById("swiper-slides")) {
    initialiseSwiper();
}
document.addEventListener("initSwiper", initialiseSwiper);

// Simplify Toastify initialization
document.addEventListener("showFlashMessage", function(evt) {
    const level = evt.detail.level?.toLowerCase() || "info";
    const background = level === "warn" 
        ? "linear-gradient(to right, #9c4f43, #e03419)" 
        : "linear-gradient(to right, #00b09b, #96c93d)";
    
    Toastify({
        text: evt.detail.message,
        duration: 5000,
        newWindow: true,
        close: true,
        gravity: "top",
        position: "right",
        stopOnFocus: true,
        style: { background },
    }).showToast();
});

// Encapsulate TomSelect logic
(function() {

    // The following line wont play nicely with hx-boost.
    // See https://htmx.org/quirks/#some-people-don-t-like-hx-boost
    const selectControls = [];

    function initialiseTomSelect() {
        htmx.onLoad((elt) => {
            const selectors = [
                { className: ".tom-select-control-readonly", options: { create: false, highlight: true, allowEmptyOption: false, maxItems: 1, items:[] } },
                { className: ".tom-select-control-create", options: { create: true } }
            ];

            selectors.forEach(({ className, options }) => {
                htmx.findAll(elt, className).forEach((el) => {
                    if (!el.tomselect) {
                        try {
                            selectControls.push(new TomSelect(el, { ...options, sortField: [{ field: '$order' }, { field: '$score' }] }));
                            console.debug(`Created a Tom Select for ${className}`);
                        } catch (err) {
                            console.error(`Error initializing Tom Select for ${className}:`, err);
                        }
                    }
                });
            });
        });
    }

    function clearSelects(targetId) {
        selectControls.forEach((control) => {
            if (control.inputId !== targetId) {
                control.clear(true);
                console.debug(`Cleared select control with ID: ${control.inputId}`);
            }
        });
    }

    // Expose functions
    window.initialiseTomSelect = initialiseTomSelect;
    window.clearSelects = clearSelects;
})();

// Initialize TomSelect if specific elements exist
["createreviewform", "select-by-author"].forEach((id) => {
    if (document.getElementById(id)) initialiseTomSelect();
});

// Event listeners for clearing selects
["Rating", "Author", "Genre", "Reviewer"].forEach((type) => {
    document.addEventListener(`clearSelect${type}`, () => clearSelects(`select-by-${type.toLowerCase()}`));
});

// Encapsulate DataTables logic
(function() {
    const userAdminTables = [];

    function initialiseSimpleDataTables() {
        const options = { searchable: true, perPage: 10 };
        const table = new window.simpleDatatables.DataTable("#users-table", options);
        userAdminTables.push(table);
        console.debug("Initialized DataTable");
    }

    function refreshSimpleDataTables() {
        userAdminTables.forEach((table) => {
            table.destroy();
            console.debug("Destroyed DataTable");
        });
        initialiseSimpleDataTables();
    }

    // Expose functions
    window.initialiseSimpleDataTables = initialiseSimpleDataTables;
    window.refreshSimpleDataTables = refreshSimpleDataTables;
})();

// Initialize DataTables if the element exists
if (document.getElementById("users-table")) {
    initialiseSimpleDataTables();
}
document.addEventListener("refreshSimpleDataTables", refreshSimpleDataTables);

// Utility function to get a cookie value
function getCookieValue(name) {
    const cookies = document.cookie.split(";").map((cookie) => cookie.trim());
    const match = cookies.find((cookie) => cookie.startsWith(`${name}=`));
    return match ? decodeURIComponent(match.split("=")[1]) : "";
}

// Add XSRF token to HTMX requests.
// This is the "naive double submit cookie pattern" that OWASP try to discourage but
// which I'm happy is strong enough CSRF protection for this site's limited functionality.
// See more at https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#naive-double-submit-cookie-pattern-discouraged
document.body.addEventListener("htmx:configRequest", function(evt) {
    const xsrfToken = getCookieValue("XSRF-TOKEN");
    const isSafeVerb = ["get", "head", "options"].includes(evt.detail.verb.toLowerCase());

    if (!isSafeVerb && xsrfToken && !Object.keys(evt.detail.headers).some((key) => key.toLowerCase() === "x-xsrf-token")) {
        evt.detail.headers["X-XSRF-TOKEN"] = xsrfToken;
        console.debug("Added X-XSRF-TOKEN header:", xsrfToken);
    }
});



