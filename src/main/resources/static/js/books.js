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
    Toastify({
        text: evt.detail.value,
        duration: 5000,
        newWindow: true,
        close: true,
        gravity: "top", // `top` or `bottom`
        position: "right", // `left`, `center` or `right`
        stopOnFocus: true, // Prevents dismissing of toast on hover
        style: {
            background: "linear-gradient(to right, #00b09b, #96c93d)",
        }
    }).showToast();
});

(function() {
    let selectControls = [];

    function initialiseTomSelect() {
        htmx.onLoad(function (elt) {
            const allSelects = htmx.findAll(elt, ".tom-select-control");
            allSelects.forEach((el) => {
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
                        console.debug('Created a Tom Select');
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

function initialiseSimpleDataTables(evt) {
    const options = {
        searchable: true,
        perPage: 10
    };
    new window.simpleDatatables.DataTable("#users-table", options);
}
if (document.getElementById("users-table")) {
    initialiseSimpleDataTables();
}
document.addEventListener("initSimpleDataTables", function(evt) {
    console.log('Should be initing data table');
    initialiseSimpleDataTables();
});



