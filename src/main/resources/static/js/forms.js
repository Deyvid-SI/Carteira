document.querySelectorAll('[data-uppercase]').forEach(input => input.addEventListener('input', () => {
    input.value = input.value.toUpperCase().replace(/[^A-Z0-9.-]/g, '');
}));

document.querySelectorAll('[data-mask="cnpj"]').forEach(input => input.addEventListener('input', () => {
    const d = input.value.replace(/\D/g, '').slice(0, 14);
    input.value = d.replace(/^(\d{2})(\d)/, '$1.$2').replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
        .replace(/\.(\d{3})(\d)/, '.$1/$2').replace(/(\d{4})(\d)/, '$1-$2');
}));

document.querySelectorAll('[data-mask="cep"]').forEach(input => input.addEventListener('input', () => {
    const d = input.value.replace(/\D/g, '').slice(0, 8);
    input.value = d.replace(/^(\d{5})(\d)/, '$1-$2');
}));

document.querySelectorAll('[data-numeric]').forEach(input => input.addEventListener('input', () => {
    if (input.dataset.numeric === 'integer') {
        input.value = input.value.replace(/\D/g, '');
        return;
    }
    const normalized = input.value.replace(',', '.').replace(/[^0-9.]/g, '');
    const [integer = '', ...decimalParts] = normalized.split('.');
    const decimal = decimalParts.join('').slice(0, 4);
    input.value = decimalParts.length ? `${integer}.${decimal}` : integer;
}));
