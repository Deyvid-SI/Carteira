(() => {
    const asset = document.getElementById('acaoId');
    const type = document.getElementById('tipo');
    const quantity = document.getElementById('quantidade');
    const executionPrice = document.getElementById('precoUnitario');
    const submit = document.getElementById('tradeSubmit');
    if (!asset || !type || !quantity || !executionPrice || !submit) return;

    const text = id => document.getElementById(id);
    const number = value => Number.parseFloat(value || '0') || 0;
    const quantityFormat = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });
    const moneyFormat = currency => new Intl.NumberFormat('pt-BR', {
        style: 'currency', currency: currency === 'USD' ? 'USD' : 'BRL'
    });

    function updatePreview() {
        const option = asset.selectedOptions[0];
        const selected = option?.value;
        const currentQuote = number(option?.dataset.price);
        const price = number(executionPrice.value);
        const balance = number(option?.dataset.balance);
        const currency = option?.dataset.currency || 'BRL';
        const ticker = option?.dataset.ticker || '';
        const units = number(quantity.value);
        const operationFees = 0;
        const operation = type.value;
        const sale = operation === 'VENDA';
        const gross = units * price;
        const total = sale ? gross - operationFees : gross + operationFees;
        const finalBalance = sale ? balance - units : balance + units;
        const formatter = moneyFormat(currency);
        const invalidSale = Boolean(selected && sale && units > balance);
        const invalidPrice = Boolean(selected && price <= 0);

        text('quotePrice').textContent = selected ? formatter.format(currentQuote) : 'Selecione um ativo';
        text('quoteBalance').textContent = selected ? quantityFormat.format(balance) : '—';
        text('quoteTicker').textContent = ticker ? `${ticker} · unidades disponíveis` : 'em unidades';
        text('previewGross').textContent = selected && units > 0 && price > 0 ? formatter.format(gross) : '—';
        text('previewFees').textContent = selected ? formatter.format(0) : '—';
        text('previewTotalLabel').textContent = sale ? 'Total estimado a receber' : 'Total estimado a pagar';
        text('previewTotal').textContent = selected && units > 0 && price > 0 ? formatter.format(Math.max(0, total)) : '—';
        text('previewBalance').textContent = selected && units > 0 ? quantityFormat.format(finalBalance) : '—';

        const warning = text('tradeWarning');
        warning.classList.toggle('negative', invalidSale || invalidPrice);
        if (invalidPrice) warning.textContent = 'Informe um preço maior que zero para a operação.';
        else if (invalidSale) warning.textContent = `Venda recusada: o saldo disponível é ${quantityFormat.format(balance)}.`;
        else if (!selected || !operation) warning.textContent = 'Selecione o ativo e a operação para visualizar os valores.';
        else if (units <= 0) warning.textContent = 'Informe uma quantidade maior que zero.';
        else warning.textContent = `A operação de ${ticker} será registrada por ${formatter.format(price)} por ação.`;

        submit.disabled = invalidSale || invalidPrice;
        submit.firstChild.textContent = sale ? 'Registrar venda ' : operation === 'COMPRA' ? 'Registrar compra ' : 'Registrar operação ';
    }

    function fillPriceFromCurrentQuote() {
        const quote = number(asset.selectedOptions[0]?.dataset.price);
        executionPrice.value = quote > 0 ? String(quote) : '';
        updatePreview();
    }

    asset.addEventListener('change', fillPriceFromCurrentQuote);
    [type, quantity, executionPrice].forEach(element => {
        element.addEventListener('input', updatePreview);
        element.addEventListener('change', updatePreview);
    });
    if (!executionPrice.value && asset.value) fillPriceFromCurrentQuote();
    else updatePreview();
})();
