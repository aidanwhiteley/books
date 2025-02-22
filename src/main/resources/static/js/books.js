
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
document.body.addEventListener("initSwiper", function(evt) {
    initialiseSwiper();
})

document.body.addEventListener("showFlashMessage", function(evt){
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
})

console.log('Initting globals');
var bookAppGlobals = {
    selectControls: [],
}

function initialiseTomSelect() {
    htmx.onLoad(function (elt) {
        const allSelects = htmx.findAll(elt, ".tom-select-control");
        allSelects.forEach((el) => {
            if (!el.tomselect) {
                try {
                    bookAppGlobals.selectControls.push(new TomSelect(el, {
                        create: false,
                        highlight: true,
                        allowEmptyOption: false,
                        maxItems: 1,
                        items: [],
                        sortField: [{ field: '$order' }, { field: '$score' }]
                    }));
                } catch (err) {
                    console.debug('Ignoring already initted error on Tom Select');
                }
            }
        }
        )
    });
}

function clearSelects(evt) {
    //const allSelects = htmx.findAll(".tom-select-control");
    const allSelects = bookAppGlobals.selectControls;
    allSelects.forEach((el) => {
        console.log(el);
        if (el.tomselect) {
            console.log(el);
            if (el.inputId === evt.id) {
                console.debug('Not clearing el.inputId ' + el.inputId + ' evt.id ' + evt.id);
            } else {
                console.debug('Clearing el.id ' + el.inputId + ' evt.id ' + evt.id);
                el.clear(true);
            }
        }
    });
}

if (document.getElementById("createreviewform")) {
    initialiseTomSelect();
}

if (document.getElementById("select-by-author")) {
    initialiseTomSelect();
}




