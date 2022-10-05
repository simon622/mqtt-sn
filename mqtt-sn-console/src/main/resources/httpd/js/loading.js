// This code depends on jQuery

//create 1 global variable for namespacing purposes.
if (typeof tinyfly === 'undefined') {
    tinyfly = {};
}

/* Handles adding and removing loading animations for ajax calls */
tinyfly.loading = (function($) {
    var defaults = {},
        add, remove;

    // default options
    defaults = {
        size: 'large',
        position: 'inside',
        display: 'inline',
        timeout: 100
    };

    /*
        Function: add
        Adds loading animation markup into the DOM. Typically called on AJAX beforeSend.

        Parameters:

            (object) $el - jQuery object of the element that you want to insert the markup beside or into.
            (object) options - The optional configuration settings.
            (string) options.size {large, small} - determines the size of the animated gif spinner.
            (string) options.position {before, after, inside} - Determines the jQuery insertion method.
            (integer) options.timeout - number of miliseconds to wait before showing the spinner.
    */
    add = function($el, options) {
        var settings = $.extend(defaults, options),
            $markup, insertMarkup;

        $markup = $('<span class="loader">loading&hellip;</span>')
            .addClass(settings.size + ' ' + settings.position + ' ' + settings.display);

        insertMarkup = function () {
            //decide which DOM insertion to use
            switch (settings.position) {
                case 'before':
                    $el.before($markup);
                    break;
                case 'after':
                    $el.after($markup);
                    break;
                case 'inside':
                default:
                    $el.prepend($markup);
            }

            //add timerId to the data blob so we can cancel it if the ajax call is quick
            $el.data('loading').timerId = settings.timerId;
        };

        //only show the loading animation if the ajax call takes longer the specified miliseconds
        settings.timerId = window.setTimeout(insertMarkup, settings.timeout);
        $el.data('loading', settings);
    };

    /*
        Function: remove
        Removes the markup inserted by the add function. Typically called on AJAX Complete.

        Parameters:

            (object) $el - jQuery object of the element on which to start DOM traversal
                            and find the loading animation markup to remove

        See Also:

        <add>
    */
    remove = function($el) {
        var settings = $el.data('loading'),
            selector = 'span.loader',
            $span;

        //check if there is a timer, if so clear it so the spinner
        //doesn't get added after the page has loaded
        if (settings.timerId) {
            window.clearTimeout(settings.timerId);
        }

        //decide which DOM traversal to use
        switch (settings.position) {
            case 'before':
                $span = $el.prev(selector)
                break;
            case 'after':
                $span = $el.next(selector)
                break;
            case 'inside':
            default:
                $span = $el.children(selector)
        }

        $el.removeData('loading');
        $span.remove();
    };

    //public methods
    return {
        add: add,
        remove: remove
    }
})(jQuery);

