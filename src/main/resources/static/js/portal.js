(() => {
    const rows = [...document.querySelectorAll('[data-history-row]')];
    const search = document.querySelector('[data-history-search]');
    const type = document.querySelector('[data-history-type]');
    const market = document.querySelector('[data-history-market]');
    const count = document.querySelector('[data-history-count]');
    const empty = document.querySelector('[data-history-empty]');

    function normalize(value) {
        return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
    }

    function filterHistory() {
        const term = normalize(search?.value);
        const selectedType = type?.value || '';
        const selectedMarket = market?.value || '';
        let visible = 0;
        rows.forEach(row => {
            const match = (!term || normalize(row.dataset.search).includes(term))
                && (!selectedType || row.dataset.type === selectedType)
                && (!selectedMarket || row.dataset.market === selectedMarket);
            row.hidden = !match;
            if (match) visible += 1;
        });
        if (count) count.textContent = String(visible);
        if (empty) empty.hidden = visible !== 0 || rows.length === 0;
    }

    [search, type, market].filter(Boolean).forEach(control => {
        control.addEventListener(control === search ? 'input' : 'change', filterHistory);
    });

    document.querySelectorAll('[data-auto-submit]').forEach(control => {
        control.addEventListener('change', () => control.form?.requestSubmit());
    });
})();
