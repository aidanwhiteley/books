function initialiseSwiper(evt) {
    new Swiper('.mySwiper', {
        effect: 'coverflow',
        grabCursor: true,
        centeredSlides: true,
        slidesPerView: 'auto',
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
if (document.getElementById("swiper-slides")) {
    initialiseSwiper();
}
document.addEventListener("initSwiper", function(evt) {
    initialiseSwiper();
});

document.addEventListener("showFlashMessage", function(evt){
    // Default to info level colours
    let background = "linear-gradient(to right, #00b09b, #96c93d)";
    if (evt.detail.level.toLowerCase() === "warn") {
        background = "linear-gradient(to right, #9c4f43, #e03419)";
     }
    Toastify({
        text: evt.detail.message,
        duration: 5000,
        newWindow: true,
        close: true,
        gravity: "top",
        position: "right",
        stopOnFocus: true,
        style: {
            background: background,
        }
    }).showToast();
});

(function() {
    let selectControls = [];

    function initialiseTomSelect() {
        htmx.onLoad(function (elt) {
            const readSelects = htmx.findAll(elt, ".tom-select-control-readonly");
            readSelects.forEach((el) => {
                if (!el.tomselect) {
                    try {
                        selectControls.push(new TomSelect(el, {
                            create: false,
                            highlight: true,
                            allowEmptyOption: false,
                            maxItems: 1,
                            items: [],
                            sortField: [{ field: '$order' }, { field: '$score' }]
                        }));
                        console.debug('Created a Tom Select read only');
                    } catch (err) {
                        console.error('Ignoring already initted error on Tom Select');
                    }
                }
            });

            const createSelects = htmx.findAll(elt, ".tom-select-control-create");
            createSelects.forEach((el) => {
                if (!el.tomselect) {
                    try {
                        selectControls.push(new TomSelect(el, {
                            create: true,
                            sortField: [{ field: '$order' }, { field: '$score' }]
                        }));
                        console.debug('Created a Tom Select create');
                    } catch (err) {
                        console.error('Ignoring already initted error on Tom Select');
                    }
                }
            });
        });
    }

    function clearSelects(evt) {
        const allSelects = selectControls;
        allSelects.forEach((el) => {
            if (el.inputId === evt.id) {
                console.debug('Not clearing el.inputId ' + el.inputId + ' evt.id ' + evt.id);
            } else {
                console.debug('Clearing el.id ' + el.inputId + ' evt.id ' + evt.id);
                el.clear(true);
            }
        });
    }

    // Expose functions needed elsewhere
    window.clearSelects = clearSelects;
    window.initialiseTomSelect = initialiseTomSelect;
})();

if (document.getElementById("createreviewform")) {
    initialiseTomSelect();
}

if (document.getElementById("select-by-author")) {
    initialiseTomSelect();
}

(function() {
    let userAdminTables = [];

    function initialiseSimpleDataTables(evt) {
        const options = {
            searchable: true,
            perPage: 10
        };
        userAdminTables.push(new window.simpleDatatables.DataTable("#users-table", options));
        console.debug('Should have initialised a DataTable')
    }

    function refreshSimpleDataTables(evt) {
            userAdminTables.forEach((el) => {
                el.destroy();
                console.debug('Should have destroyed a DataTable');
            });
            initialiseSimpleDataTables();
        }

    // Expose functions needed elsewhere
    window.initialiseSimpleDataTables = initialiseSimpleDataTables;
    window.refreshSimpleDataTables = refreshSimpleDataTables;
})();

if (document.getElementById("users-table")) {
    initialiseSimpleDataTables();
}
document.addEventListener("refreshSimpleDataTables", function(evt) {
    refreshSimpleDataTables();
});

// Function to read the value from a cookie named XSRF-TOKEN
function getXsrfToken() {
    const name = "XSRF-TOKEN=";
    const decodedCookie = decodeURIComponent(document.cookie);
    const ca = decodedCookie.split(';');
    for(let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) === 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

document.body.addEventListener('htmx:configRequest', function(evt) {

    const existingHeaderNames = Object.keys(evt.detail.headers);
    const existingHeaderNamesUc = existingHeaderNames.map(function(x){ return x.toUpperCase(); })
    const verb = evt.detail.verb.toLowerCase();
    const safeVerbs = ['get', 'head', 'options'];

    const xsrfCookieValue = getXsrfToken(); 

    if (xsrfCookieValue) {
        if (safeVerbs.indexOf(verb) === -1){
            if (!existingHeaderNamesUc.includes('X-XSRF-TOKEN')) {
                    evt.detail.headers['X-XSRF-TOKEN'] = xsrfCookieValue;
                    console.debug('Set X-XSRF-TOKEN header to ' + xsrfCookieValue);
            } else {
                    console.debug('Request headers already contained an X-XSRF-TOKEN header');
            }
        } else {
            console.debug('Request was for a safe HTTP verb so no X-XSRF-TOKEN added');
        }
    } else { 
        console.debug('No XSRF-TOKEN cookie found');
    }
});



