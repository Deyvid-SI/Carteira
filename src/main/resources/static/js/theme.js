(() => {
    const storageKey = 'carteira-theme';
    const motionStorageKey = 'carteira-motion';
    const densityStorageKey = 'carteira-density';
    const savedTheme = localStorage.getItem(storageKey);
    const savedMotion = localStorage.getItem(motionStorageKey);
    const savedDensity = localStorage.getItem(densityStorageKey);
    const initialTheme = savedTheme === 'light' || savedTheme === 'dark' ? savedTheme : 'dark';
    const initialMotion = savedMotion === 'full' || savedMotion === 'reduced'
        ? savedMotion
        : (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'reduced' : 'full');
    const initialDensity = savedDensity === 'compact' ? 'compact' : 'comfortable';
    document.documentElement.dataset.theme = initialTheme;
    document.documentElement.dataset.motion = initialMotion;
    document.documentElement.dataset.density = initialDensity;

    function syncButtons(theme) {
        const light = theme === 'light';
        document.querySelectorAll('[data-theme-toggle]').forEach(button => {
            button.setAttribute('aria-pressed', String(light));
            const icon = button.querySelector('[data-theme-icon]');
            const label = button.querySelector('[data-theme-label]');
            if (icon) icon.textContent = light ? '☾' : '☀';
            if (label) label.textContent = light ? 'Modo escuro' : 'Modo claro';
            button.title = light ? 'Usar preto piano e dourado' : 'Usar branco e dourado';
        });
    }

    function applyTheme(theme, persist = true) {
        document.documentElement.dataset.theme = theme;
        if (persist) localStorage.setItem(storageKey, theme);
        syncButtons(theme);
        syncPreferenceButtons();
        window.dispatchEvent(new CustomEvent('carteira:themechange', { detail: { theme } }));
    }

    function syncPreferenceButtons() {
        const values = {
            theme: document.documentElement.dataset.theme,
            motion: document.documentElement.dataset.motion,
            density: document.documentElement.dataset.density
        };
        Object.entries(values).forEach(([preference, value]) => {
            document.querySelectorAll(`[data-${preference}-choice]`).forEach(button => {
                const active = button.dataset[`${preference}Choice`] === value;
                button.setAttribute('aria-pressed', String(active));
                button.classList.toggle('selected', active);
                const action = button.querySelector(':scope > b');
                if (action) action.textContent = active ? 'Em uso' : 'Selecionar';
            });
        });
    }

    function announcePreference(message) {
        const status = document.querySelector('[data-preference-status]');
        if (status) status.textContent = message;
    }

    function applyMotion(value, persist = true) {
        document.documentElement.dataset.motion = value;
        if (persist) localStorage.setItem(motionStorageKey, value);
        syncPreferenceButtons();
        announcePreference(value === 'reduced' ? 'Movimento reduzido foi ativado.' : 'Movimento suave foi ativado.');
    }

    function applyDensity(value, persist = true) {
        document.documentElement.dataset.density = value;
        if (persist) localStorage.setItem(densityStorageKey, value);
        syncPreferenceButtons();
        announcePreference(value === 'compact' ? 'Densidade compacta foi ativada.' : 'Densidade confortável foi ativada.');
    }

    function configureTheme() {
        syncButtons(document.documentElement.dataset.theme);
        syncPreferenceButtons();
        document.querySelectorAll('[data-theme-toggle]').forEach(button => {
            button.addEventListener('click', () => {
                const next = document.documentElement.dataset.theme === 'light' ? 'dark' : 'light';
                applyTheme(next);
            });
        });
        document.querySelectorAll('[data-theme-choice]').forEach(button => {
            button.addEventListener('click', () => {
                applyTheme(button.dataset.themeChoice);
                announcePreference('Tema atualizado e salvo neste navegador.');
            });
        });
        document.querySelectorAll('[data-motion-choice]').forEach(button => {
            button.addEventListener('click', () => applyMotion(button.dataset.motionChoice));
        });
        document.querySelectorAll('[data-density-choice]').forEach(button => {
            button.addEventListener('click', () => applyDensity(button.dataset.densityChoice));
        });
        document.querySelectorAll('[data-reset-preferences]').forEach(button => {
            button.addEventListener('click', () => {
                applyTheme('dark');
                applyMotion('full');
                applyDensity('comfortable');
                announcePreference('Preferências restauradas para o padrão.');
            });
        });

        const sectionLinks = [...document.querySelectorAll('[data-section-link]')];
        const sections = sectionLinks.map(link => document.getElementById(link.dataset.sectionLink)).filter(Boolean);
        if (sections.length && 'IntersectionObserver' in window) {
            const observer = new IntersectionObserver(entries => {
                const visible = entries.filter(entry => entry.isIntersecting)
                    .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
                if (!visible) return;
                sectionLinks.forEach(link => link.classList.toggle('active', link.dataset.sectionLink === visible.target.id));
            }, { rootMargin: '-18% 0px -62% 0px', threshold: [0, .15, .35] });
            sections.forEach(section => observer.observe(section));
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', configureTheme, { once: true });
    } else {
        configureTheme();
    }
})();
