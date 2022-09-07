There are several ways of fetching resources from the web:
- Using the good ol' [XMLHttpRequest](https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest), which was the only way for a long time since the SPAs and dynamic web sites made their appearance and started to make massive use of this feature to load fragments of data without reloading the entire web.
- Using the new [Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API), which implements a cleaner way to get a resource. It also has the advantage of being compatible with the ES6 Promises, but the disadvantage of [not working with IE](http://caniuse.com/#search=fetch).
- Using framework or library implementations, such as [jQuery.ajax](http://api.jquery.com/jQuery.ajax/). Most of them are just wrappers around XHR to improve cross-browser compatibility and offer a more friendly API, given the XMLHttpRequest API is kinda messy.

Here we'll use the Fetch API. The following is a basic example:

    const HOST = 'http://myapihost.com';
    const NAMESPACE = 'v1';

    const headers = new Headers({
      'X-Auth-Token': localStorage.getItem('token'),
      'Content-Type': 'json',
    });

    const request = new Request(`${HOST}/${NAMESPACE}/endpoint`, {
      method: 'GET',
      headers,
    });

    fetch(request)
      .then(res => res.json())
      .then(resJSON => {
        // If everything goes OK, do something with the response, such as binding some DOM elements with the data
      })
      .catch(error => {
        // Otherwhise, do something with the error, such as showing an error message or an alert of any kind
      });

In some cases the browser makes a [cross-origin HTTP request](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) (CORS). This means it requests a resource from a different domain, protocol, or port than the one from which the current document originated. In this case, a OPTIONS request (preflight) will be automatically made prior to the main request (GET, POST, etc). If it responds with headers carrying the appropriate permissions, the initial request will work properly.