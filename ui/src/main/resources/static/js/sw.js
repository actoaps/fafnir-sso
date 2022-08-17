self.addEventListener('fetch', event => event.respondWith(e => fetch(e.request, {
    mode: 'cors',
    credentials: 'omit',
    headers: {
        'Authorization': sessionStorage.getItem('jwt-token')
    }
})))
