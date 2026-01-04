/*
    This file contains the application specific JavaScript for the Books application.
    Because the application is an MPA, there is only a little bit of JavaScript required
    to add some interactivity and integrate a few 3rd party JS components.

    In many ways (particularly to support "locality of behaviour"), it would have been nice
    to keep this JavaScript inline in the pages it is used. However, as one of the aims of
    this demo project is to show HTMX being used with a Content Security Policy that disallows
    inline scripts, it is all kept in this single external file.
*/

// Configuration constants
const CONFIG = {
    TOAST_DURATION: 5000,
    DATATABLE_PER_PAGE: 10,
    SWIPER_ROTATE: 20,
    SWIPER_STRETCH: 10,
    SWIPER_DEPTH: 30,
    SWIPER_MODIFIER: 1,
    TOAST_COLORS: {
        WARN: "linear-gradient(to right, #9c4f43, #e03419)",
        DEFAULT: "linear-gradient(to right, #00b09b, #96c93d)"
    }
};

// Global instances for cleanup management
const instances = {
    swiper: null,
    tomSelects: [],
    dataTables: []
};


// Utility function to initialize a Swiper instance
function initializeSwiper(evt) {
    try {
        const swiperElement = document.querySelector('.mySwiper');
        if (!swiperElement) {
            console.warn('Swiper element not found');
            return null;
        }

        // Clean up existing instance if it exists
        if (instances.swiper) {
            instances.swiper.destroy(true, true);
        }

        instances.swiper = new Swiper('.mySwiper', {
            effect: 'coverflow',
            grabCursor: true,
            centeredSlides: true,
            slidesPerView: 'auto',
            initialSlide: 0,
            coverflowEffect: {
                rotate: CONFIG.SWIPER_ROTATE,
                stretch: CONFIG.SWIPER_STRETCH,
                depth: CONFIG.SWIPER_DEPTH,
                modifier: CONFIG.SWIPER_MODIFIER,
                slideShadows: false,
            },
            loop: true,
            navigation: {
                nextEl: '.swiper-button-next',
                prevEl: '.swiper-button-prev',
            }
        });

        return instances.swiper;
    } catch (error) {
        console.error('Failed to initialize Swiper:', error);
        return null;
    }
}

// Initialize Swiper if the element exists
if (document.getElementById("swiper-slides")) {
    initializeSwiper();
}
document.addEventListener("initSwiper", initializeSwiper);

// Handle flash message display with Toastify
const handleFlashMessage = (evt) => {
    try {
        const level = evt.detail.level?.toLowerCase() || "info";
        const background = level === "warn"
            ? CONFIG.TOAST_COLORS.WARN
            : CONFIG.TOAST_COLORS.DEFAULT;

        Toastify({
            text: evt.detail.message,
            duration: CONFIG.TOAST_DURATION,
            newWindow: true,
            close: true,
            gravity: "top",
            position: "right",
            stopOnFocus: true,
            style: { background },
        }).showToast();
    } catch (error) {
        console.error('Failed to show flash message:', error);
    }
};

document.addEventListener("showFlashMessage", handleFlashMessage);

// Encapsulate TomSelect logic
(function() {

    // The following line wont play nicely with hx-boost.
    // See https://htmx.org/quirks/#some-people-don-t-like-hx-boost

    function initializeTomSelect() {
        htmx.onLoad((elt) => {
            const selectors = [
                { className: ".tom-select-control-readonly", options: { create: false, highlight: true, allowEmptyOption: false, maxItems: 1, items:[] } },
                { className: ".tom-select-control-create", options: { create: true } }
            ];

            selectors.forEach(({ className, options }) => {
                htmx.findAll(elt, className).forEach((el) => {
                    if (!el.tomselect) {
                        try {
                            const tomSelectInstance = new TomSelect(el, {
                                ...options,
                                sortField: [{ field: '$order' }, { field: '$score' }]
                            });
                            instances.tomSelects.push(tomSelectInstance);
                        } catch (err) {
                            console.error(`Error initializing Tom Select for ${className}:`, err);
                        }
                    }
                });
            });
        });
    }

    function clearSelects(targetId) {
        instances.tomSelects.forEach((control) => {
            try {
                if (control.inputId !== targetId) {
                    control.clear(true);
                }
            } catch (error) {
                console.error('Error clearing TomSelect:', error);
            }
        });
    }

    // Expose functions
    window.initializeTomSelect = initializeTomSelect;
    window.clearSelects = clearSelects;
})();

// Initialize TomSelect if specific elements exist
["createreviewform", "select-by-author"].forEach((id) => {
    if (document.getElementById(id)) initializeTomSelect();
});

// Event listeners for clearing selects
["Rating", "Author", "Genre", "Reviewer"].forEach((type) => {
    document.addEventListener(`clearSelect${type}`, () => clearSelects(`select-by-${type.toLowerCase()}`));
});

// Encapsulate DataTables logic
(function() {

    function initializeSimpleDataTables() {
        try {
            const tableElement = document.getElementById("users-table");
            if (!tableElement) {
                console.warn('DataTable element #users-table not found');
                return null;
            }

            // Clean up existing instances to prevent duplicates
            instances.dataTables.forEach((table) => {
                try {
                    table.destroy();
                } catch (error) {
                    console.error('Error destroying existing DataTable:', error);
                }
            });
            instances.dataTables.length = 0;

            const options = {
                searchable: true,
                perPage: CONFIG.DATATABLE_PER_PAGE
            };
            const table = new window.simpleDatatables.DataTable(tableElement, options);
            instances.dataTables.push(table);

            return table;
        } catch (error) {
            console.error('Failed to initialize DataTable:', error);
            return null;
        }
    }

    function refreshSimpleDataTables() {
        try {
            instances.dataTables.forEach((table) => {
                table.destroy();
            });
            instances.dataTables.length = 0;
            initializeSimpleDataTables();
        } catch (error) {
            console.error('Failed to refresh DataTables:', error);
        }
    }

    // Expose functions
    window.initializeSimpleDataTables = initializeSimpleDataTables;
    window.refreshSimpleDataTables = refreshSimpleDataTables;
})();

// Initialize DataTables if the element exists
if (document.getElementById("users-table")) {
    initializeSimpleDataTables();
}
document.addEventListener("refreshSimpleDataTables", refreshSimpleDataTables);

// Utility function to get a cookie value
function getCookieValue(name) {
    const cookies = document.cookie.split(";").map((cookie) => cookie.trim());
    const match = cookies.find((cookie) => cookie.startsWith(`${name}=`));
    return match ? decodeURIComponent(match.split("=")[1]) : "";
}

// Cleanup function for resource management
function cleanup() {
    try {
        // Clean up Swiper instance
        if (instances.swiper) {
            instances.swiper.destroy(true, true);
            instances.swiper = null;
        }

        // Clean up TomSelect instances
        instances.tomSelects.forEach((control) => {
            try {
                control.destroy();
            } catch (error) {
                console.error('Error destroying TomSelect:', error);
            }
        });
        instances.tomSelects.length = 0;

        // Clean up DataTable instances
        instances.dataTables.forEach((table) => {
            try {
                table.destroy();
            } catch (error) {
                console.error('Error destroying DataTable:', error);
            }
        });
        instances.dataTables.length = 0;

        console.log('Cleanup completed successfully');
    } catch (error) {
        console.error('Error during cleanup:', error);
    }
}

// Expose cleanup function globally for potential use
window.booksJsCleanup = cleanup;

// Encapsulate Google Books title and author validation logic that prevents unnecessary API calls
// unless both the title and author are supplied.
(function() {
    const validateGoogleBooksRequest = (evt) => {
        try {
            // Only apply to requests targeting the googlebooks endpoint from the author input
            if (evt.detail.requestConfig.path.startsWith("/googlebooks") &&
                evt.detail.elt && evt.detail.elt.id === "author") {

                const titleInput = document.getElementById("title");
                const authorInput = document.getElementById("author");

                const titleValue = titleInput ? titleInput.value.trim() : "";
                const authorValue = authorInput ? authorInput.value.trim() : "";

                // Cancel the request if either title or author is empty
                // This matches the comment above about "unless both are supplied"
                if (!titleValue || !authorValue) {
                    console.debug("Both book title and author required for Google Books API call");
                    evt.preventDefault();
                    return false;
                }
            }
            return true;
        } catch (error) {
            console.error('Error in Google Books validation:', error);
            return true; // Allow request to proceed if validation fails
        }
    };

    // Listen for htmx beforeRequest events
    document.body.addEventListener("htmx:beforeRequest", validateGoogleBooksRequest);
})();

// Add XSRF token to HTMX requests.
// This is the "naive double submit cookie pattern" that OWASP try to discourage but
// which I'm happy is strong enough CSRF protection for this site's limited functionality.
// See more at https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#naive-double-submit-cookie-pattern-discouraged
const handleXsrfToken = (evt) => {
    try {
        const xsrfToken = getCookieValue("XSRF-TOKEN");
        const isSafeVerb = ["get", "head", "options"].includes(evt.detail.verb.toLowerCase());

        if (!isSafeVerb && xsrfToken && !Object.keys(evt.detail.headers).some((key) => key.toLowerCase() === "x-xsrf-token")) {
            evt.detail.headers["X-XSRF-TOKEN"] = xsrfToken;
        }
    } catch (error) {
        console.error('Error handling XSRF token:', error);
    }
};

document.body.addEventListener("htmx:configRequest", handleXsrfToken);



