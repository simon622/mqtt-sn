var updateTime = 5000;
var fieldRefreshTime = 1000;


function getAllUrlParams(url) {

    // get query string from url (optional) or window
    var queryString = url ? url.split('?')[1] : window.location.search.slice(1);

    // we'll store the parameters here
    var obj = {};

    // if query string exists
    if (queryString) {

        // stuff after # is not part of query string, so get rid of it
        queryString = queryString.split('#')[0];

        // split our query string into its component parts
        var arr = queryString.split('&');

        for (var i = 0; i < arr.length; i++) {
            // separate the keys and the values
            var a = arr[i].split('=');

            // set parameter name and value (use 'true' if empty)
            var paramName = a[0];
            var paramValue = typeof (a[1]) === 'undefined' ? true : a[1];

            // (optional) keep case consistent
            paramName = paramName.toLowerCase();
            if (typeof paramValue === 'string') paramValue = paramValue.toLowerCase();

            // if the paramName ends with square brackets, e.g. colors[] or colors[2]
            if (paramName.match(/\[(\d+)?\]$/)) {

                // create key if it doesn't exist
                var key = paramName.replace(/\[(\d+)?\]/, '');
                if (!obj[key]) obj[key] = [];

                // if it's an indexed array e.g. colors[2]
                if (paramName.match(/\[\d+\]$/)) {
                    // get the index value and add the entry at the appropriate position
                    var index = /\[(\d+)\]/.exec(paramName)[1];
                    obj[key][index] = paramValue;
                } else {
                    // otherwise add the value to the end of the array
                    obj[key].push(paramValue);
                }
            } else {
                // we're dealing with a string
                if (!obj[paramName]) {
                    // if it doesn't exist, create property
                    obj[paramName] = paramValue;
                } else if (obj[paramName] && typeof obj[paramName] === 'string'){
                    // if property does exist and it's a string, convert it to an array
                    obj[paramName] = [obj[paramName]];
                    obj[paramName].push(paramValue);
                } else {
                    // otherwise add the property
                    obj[paramName].push(paramValue);
                }
            }
        }
    }

    return obj;
}

function removeChildren(el){
    if(el !== null && el !== undefined){
        while (el.firstChild !== undefined && el.firstChild) {
            el.firstChild.remove()
        }
    }
}

function renderMessage(msg){
    if(msg !== null && msg.hasOwnProperty("message")){
        toast(msg.title == null ? (msg.success ? "Success" : "Error") : msg.title, msg.message, msg.success ? "alert-success" : "alert-danger");
    }
}

function toast(title, msg, level){

    var msg = '<div class="toast-header '+level+'\">' +
        '<i class="fs-6 bi-check-circle-fill"></i>&nbsp;&nbsp;' +
        '<strong class="me-auto">'+title+'</strong>' +
        '<small></small>' +
        '<button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>' +
        '</div>' +
        '<div class="toast-body">' +
        msg +
        '</div>';

    $("#liveToast").html(msg);

    const toastLive = document.getElementById('liveToast')

    const toast = new bootstrap.Toast(toastLive)
    toast.show()
}

const formToJSON = (elements) =>
[].reduce.call(
    elements,
    (data, element) => {
        data[element.name] = element.value;
        return data;
    },
    {},
);


function isEmpty(obj) {
    return obj === undefined || Object.keys(obj).length === 0;
}