const colors = ['#55d6a4', '#7897ff', '#ad84ff', '#d9ad68', '#4fc5d8', '#ec829f', '#92c96a', '#dd8b60'];
const brNumber = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const qtyNumber = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 4 });
const compactNumber = new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 });
const brDate = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
let dashboardData = null;

fetch('/dashboard/dados', { headers: { Accept: 'application/json' } })
    .then(response => {
        if (!response.ok) throw new Error('Falha ao carregar o dashboard');
        return response.json();
    })
    .then(data => { dashboardData = data; renderDashboard(data); })
    .catch(() => {
        document.getElementById('ativosBody').innerHTML = '<tr><td colspan="10" class="table-empty">Não foi possível carregar os dados.</td></tr>';
    });

document.getElementById('positionCurrency').addEventListener('change', event => {
    if (dashboardData) drawPositionComparison(dashboardData.ativos || [], event.target.value);
});

document.getElementById('averagePriceCurrency').addEventListener('change', event => {
    if (dashboardData) drawAveragePrices(dashboardData.precosMediosAcumulados || [], event.target.value);
});

function renderDashboard(data) {
    setText('totalAtivos', data.totalAtivos);
    setText('totalCorretoras', data.totalCorretoras);
    setText('ativosBrasil', data.ativosBrasil);
    setText('ativosUsa', data.ativosEstadosUnidos);
    renderFinancial(data.resumosPorMoeda || []);
    renderTable(data.ativos || []);

    drawLineChart('lineChart', 'lineEmpty', 'lineLegend', data.series || [], 'valores', '');
    drawLineChart('performanceChart', 'performanceEmpty', 'performanceLegend', data.desempenhos || [], 'percentuais', '%', true);
    drawDonut('marketChart', 'marketTotal', 'marketLegend', data.distribuicaoMercados || {}, false);
    drawDonut('allocationBrlChart', 'allocationBrlTotal', 'allocationBrlLegend', data.alocacaoPorMoeda?.BRL || {}, true);
    drawDonut('allocationUsdChart', 'allocationUsdTotal', 'allocationUsdLegend', data.alocacaoPorMoeda?.USD || {}, true);
    drawBarChart('resultChart', 'resultEmpty', data.ativos || [], item => item.rentabilidade, '%');
    drawBarChart('dailyChart', 'dailyEmpty', data.ativos || [], item => item.variacao, '%');

    const averageSelected = document.getElementById('averagePriceCurrency');
    if (!(data.precosMediosAcumulados || []).some(s => s.moeda === averageSelected.value)) {
        const firstAverageCurrency = (data.precosMediosAcumulados || [])[0]?.moeda;
        if (firstAverageCurrency) averageSelected.value = firstAverageCurrency;
    }
    drawAveragePrices(data.precosMediosAcumulados || [], averageSelected.value);

    const selected = document.getElementById('positionCurrency');
    if (!(data.ativos || []).some(a => a.moeda === selected.value)) {
        const firstCurrency = (data.ativos || [])[0]?.moeda;
        if (firstCurrency) selected.value = firstCurrency;
    }
    drawPositionComparison(data.ativos || [], selected.value);
}

function drawAveragePrices(series, currency) {
    const filtered = series.filter(item => item.moeda === currency);
    const entries = filtered.map(item => ({
        ticker: item.ticker,
        moeda: item.moeda,
        value: Number(item.valores?.at(-1) || 0)
    }));
    document.getElementById('averagePriceEmpty').style.display = entries.length ? 'none' : 'grid';
    const { ctx, width, height } = setupCanvas('averagePriceChart');
    const legend = document.getElementById('averagePriceLegend');
    if (!entries.length) {
        legend.innerHTML = '';
        return;
    }

    const pad = { left: 58, right: 18, top: 28, bottom: 42 };
    const max = Math.max(1, ...entries.map(entry => entry.value)) * 1.12;
    const plotW = width - pad.left - pad.right;
    const plotH = height - pad.top - pad.bottom;
    drawGrid(ctx, width, height, pad, 0, max, '');
    const slot = plotW / entries.length;
    const barW = Math.min(54, slot * .56);

    entries.forEach((entry, index) => {
        const x = pad.left + slot * index + (slot - barW) / 2;
        const barHeight = entry.value / max * plotH;
        const gradient = ctx.createLinearGradient(0, height - pad.bottom - barHeight, 0, height - pad.bottom);
        gradient.addColorStop(0, colors[0]);
        gradient.addColorStop(1, colors[0] + '70');
        drawPositiveBar(ctx, x, height - pad.bottom, barW, barHeight, gradient);
        ctx.fillStyle = '#87959d';
        ctx.font = '10px Inter, system-ui';
        ctx.textAlign = 'center';
        ctx.fillText(entry.ticker, x + barW / 2, height - 15);
        ctx.fillStyle = '#dfe7ea';
        ctx.font = '600 10px Inter, system-ui';
        ctx.fillText(brNumber.format(entry.value), x + barW / 2, Math.max(13, height - pad.bottom - barHeight - 8));
    });
    ctx.textAlign = 'start';
    legend.innerHTML = `<span><i style="background:${colors[0]}"></i>${escapeHtml(currency)} · preço médio atual por ação</span>`;
}

function renderFinancial(resumos) {
    ['BRL', 'USD'].forEach(currency => {
        const summary = resumos.find(r => r.moeda === currency) || {};
        setText(`investido${currency}`, brNumber.format(summary.valorInvestido || 0));
        setText(`atual${currency}`, brNumber.format(summary.valorAtual || 0));
        setMoneyAndTone(`resultado${currency}`, currency, summary.resultado || 0);
        setPercentAndTone(`rentabilidade${currency}`, summary.rentabilidade || 0);
        setMoneyAndTone(`dia${currency}`, currency, summary.resultadoDia || 0);
    });
}

function renderTable(ativos) {
    const tbody = document.getElementById('ativosBody');
    if (!ativos.length) {
        tbody.innerHTML = '<tr><td colspan="10" class="table-empty">Nenhuma posição cadastrada.</td></tr>';
        return;
    }
    tbody.innerHTML = ativos.map(a => {
        const result = Number(a.resultado || 0), profitability = Number(a.rentabilidade || 0);
        const daily = Number(a.variacao || 0), dailyMoney = Number(a.resultadoDia || 0);
        return `<tr>
            <td><div class="asset-cell"><span class="ticker-mark">${escapeHtml(a.ticker[0])}</span><span><strong>${escapeHtml(a.ticker)}</strong><small>${escapeHtml(a.nome)}</small></span></div></td>
            <td><span class="market-tag">${escapeHtml(a.mercado)}</span><small class="table-sub">${escapeHtml(a.moeda)}</small></td>
            <td><strong>${qtyNumber.format(a.quantidade)}</strong></td>
            <td>${money(a.moeda, a.precoMedio)}</td>
            <td><strong>${money(a.moeda, a.cotacao)}</strong></td>
            <td>${money(a.moeda, a.valorInvestido)}</td>
            <td><strong>${money(a.moeda, a.valorAtual)}</strong></td>
            <td><span class="variation ${tone(result)}">${signedMoney(a.moeda, result)}</span><small class="table-sub ${tone(profitability)}">${signedPercent(profitability)}</small></td>
            <td><span class="variation ${tone(daily)}">${signedPercent(daily)}</span><small class="table-sub ${tone(dailyMoney)}">${signedMoney(a.moeda, dailyMoney)}</small></td>
            <td><small>${a.atualizadoEm ? brDate.format(new Date(a.atualizadoEm)) : '—'}</small></td>
        </tr>`;
    }).join('');
}

function setupCanvas(id) {
    const canvas = document.getElementById(id);
    const rect = canvas.getBoundingClientRect();
    const ratio = window.devicePixelRatio || 1;
    const width = Math.max(280, rect.width), height = Math.max(210, rect.height);
    canvas.width = width * ratio; canvas.height = height * ratio;
    const ctx = canvas.getContext('2d'); ctx.scale(ratio, ratio);
    return { ctx, width, height };
}

function drawLineChart(canvasId, emptyId, legendId, series, key, suffix, includeZero = false) {
    const valid = series.filter(s => s[key] && s[key].length);
    document.getElementById(emptyId).style.display = valid.length ? 'none' : 'grid';
    const { ctx, width, height } = setupCanvas(canvasId);
    if (!valid.length) return;
    const pad = { left: 52, right: 18, top: 20, bottom: 34 };
    const values = valid.flatMap(s => s[key].map(Number));
    let min = Math.min(...values), max = Math.max(...values);
    if (includeZero) { min = Math.min(0, min); max = Math.max(0, max); }
    if (min === max) { min -= Math.max(1, Math.abs(min * .05)); max += Math.max(1, Math.abs(max * .05)); }
    const plotW = width - pad.left - pad.right, plotH = height - pad.top - pad.bottom;
    drawGrid(ctx, width, height, pad, min, max, suffix);
    valid.forEach((serie, index) => {
        const values = serie[key].map(Number), color = colors[index % colors.length];
        const coords = values.map((value, i) => ({
            x: pad.left + (values.length === 1 ? plotW / 2 : i * plotW / (values.length - 1)),
            y: pad.top + (max - value) * plotH / (max - min)
        }));
        const gradient = ctx.createLinearGradient(0, pad.top, 0, height - pad.bottom);
        gradient.addColorStop(0, color + '30'); gradient.addColorStop(1, color + '00');
        if (coords.length > 1) {
            ctx.beginPath(); coords.forEach((p, i) => i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y));
            ctx.lineTo(coords.at(-1).x, height - pad.bottom); ctx.lineTo(coords[0].x, height - pad.bottom); ctx.closePath(); ctx.fillStyle = gradient; ctx.fill();
        }
        ctx.beginPath(); coords.forEach((p, i) => i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y));
        ctx.strokeStyle = color; ctx.lineWidth = 2.3; ctx.stroke();
        coords.forEach(p => { ctx.beginPath(); ctx.arc(p.x, p.y, 2.7, 0, Math.PI * 2); ctx.fillStyle = '#111820'; ctx.fill(); ctx.strokeStyle = color; ctx.lineWidth = 1.8; ctx.stroke(); });
        if (index === 0) drawDateLabels(ctx, serie.labels || [], coords, height);
    });
    document.getElementById(legendId).innerHTML = valid.map((s, i) => `<span><i style="background:${colors[i % colors.length]}"></i>${escapeHtml(s.ticker)}${s.moeda ? ` · ${escapeHtml(s.moeda)}` : ''}</span>`).join('');
}

function drawGrid(ctx, width, height, pad, min, max, suffix) {
    const plotH = height - pad.top - pad.bottom;
    ctx.font = '10px Inter, system-ui'; ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
        const y = pad.top + (plotH / 4) * i, value = max - ((max - min) / 4) * i;
        ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(width - pad.right, y); ctx.strokeStyle = 'rgba(231, 241, 245, .075)'; ctx.stroke();
        ctx.fillStyle = '#74828b'; ctx.textAlign = 'right'; ctx.fillText(`${brNumber.format(value)}${suffix}`, pad.left - 7, y + 4);
    }
    ctx.textAlign = 'start';
}

function drawDateLabels(ctx, labels, coords, height) {
    if (!labels.length || !coords.length) return;
    ctx.fillStyle = '#74828b'; ctx.textAlign = 'center';
    [0, Math.floor((labels.length - 1) / 2), labels.length - 1].filter((v, i, a) => a.indexOf(v) === i)
        .forEach(i => ctx.fillText(labels[i], coords[i].x, height - 10));
    ctx.textAlign = 'start';
}

function drawDonut(canvasId, totalId, legendId, distribution, monetary) {
    const entries = Object.entries(distribution).filter(([, value]) => Number(value) > 0).slice(0, 8);
    const total = entries.reduce((sum, [, value]) => sum + Number(value), 0);
    const { ctx, width, height } = setupCanvas(canvasId);
    const radius = Math.min(width, height) * .34, inner = radius * .65, cx = width / 2, cy = height / 2;
    let angle = -Math.PI / 2;
    if (!total) {
        ctx.beginPath(); ctx.arc(cx, cy, radius, 0, Math.PI * 2); ctx.arc(cx, cy, inner, 0, Math.PI * 2, true); ctx.fillStyle = '#202a34'; ctx.fill();
    }
    entries.forEach(([, value], index) => {
        const slice = (Number(value) / total) * Math.PI * 2;
        ctx.beginPath(); ctx.arc(cx, cy, radius, angle, angle + slice); ctx.arc(cx, cy, inner, angle + slice, angle, true); ctx.closePath(); ctx.fillStyle = colors[index]; ctx.fill(); angle += slice;
    });
    setText(totalId, monetary ? compactNumber.format(total) : total);
    document.getElementById(legendId).innerHTML = entries.length ? entries.map(([label, value], i) => {
        const percent = total ? Number(value) / total * 100 : 0;
        return `<div><span><i style="background:${colors[i]}"></i>${escapeHtml(label)}</span><strong>${monetary ? compactNumber.format(value) : value} <small>${brNumber.format(percent)}%</small></strong></div>`;
    }).join('') : '<div class="legend-empty">Sem posições nesta moeda</div>';
}

function drawBarChart(canvasId, emptyId, items, valueGetter, suffix) {
    const entries = items.slice(0, 10).map(item => ({ label: item.ticker, value: Number(valueGetter(item) || 0) }));
    document.getElementById(emptyId).style.display = entries.length ? 'none' : 'grid';
    const { ctx, width, height } = setupCanvas(canvasId);
    if (!entries.length) return;
    const pad = { left: 40, right: 12, top: 18, bottom: 38 };
    let min = Math.min(0, ...entries.map(e => e.value)), max = Math.max(0, ...entries.map(e => e.value));
    if (min === max) max = 1;
    const plotW = width - pad.left - pad.right, plotH = height - pad.top - pad.bottom;
    const zeroY = pad.top + max * plotH / (max - min);
    ctx.beginPath(); ctx.moveTo(pad.left, zeroY); ctx.lineTo(width - pad.right, zeroY); ctx.strokeStyle = 'rgba(231, 241, 245, .16)'; ctx.stroke();
    const slot = plotW / entries.length, barW = Math.min(34, slot * .58);
    entries.forEach((entry, index) => {
        const y = pad.top + (max - entry.value) * plotH / (max - min), top = Math.min(y, zeroY), barH = Math.max(2, Math.abs(zeroY - y));
        ctx.fillStyle = entry.value >= 0 ? '#55d6a4' : '#ec7782'; ctx.beginPath(); ctx.roundRect(pad.left + slot * index + (slot - barW) / 2, top, barW, barH, 4); ctx.fill();
        ctx.fillStyle = '#74828b'; ctx.font = '10px Inter, system-ui'; ctx.textAlign = 'center'; ctx.fillText(entry.label, pad.left + slot * index + slot / 2, height - 14);
        ctx.fillStyle = entry.value >= 0 ? '#72e9bb' : '#ff9aa3'; ctx.fillText(`${entry.value > 0 ? '+' : ''}${brNumber.format(entry.value)}${suffix}`, pad.left + slot * index + slot / 2, entry.value >= 0 ? Math.max(11, top - 5) : Math.min(height - pad.bottom + 14, top + barH + 12));
    });
    ctx.textAlign = 'start';
}

function drawPositionComparison(ativos, currency) {
    const entries = ativos.filter(a => a.moeda === currency).slice(0, 10);
    document.getElementById('positionEmpty').style.display = entries.length ? 'none' : 'grid';
    const { ctx, width, height } = setupCanvas('positionChart');
    if (!entries.length) return;
    const pad = { left: 56, right: 18, top: 24, bottom: 42 };
    const max = Math.max(1, ...entries.flatMap(a => [Number(a.valorInvestido), Number(a.valorAtual)]));
    const plotW = width - pad.left - pad.right, plotH = height - pad.top - pad.bottom;
    for (let i = 0; i <= 4; i++) {
        const y = pad.top + i * plotH / 4;
        ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(width - pad.right, y); ctx.strokeStyle = 'rgba(231, 241, 245, .075)'; ctx.stroke();
        ctx.fillStyle = '#74828b'; ctx.font = '10px Inter, system-ui'; ctx.textAlign = 'right'; ctx.fillText(compactNumber.format(max - i * max / 4), pad.left - 7, y + 4);
    }
    const slot = plotW / entries.length, barW = Math.min(24, slot * .28);
    entries.forEach((a, index) => {
        const x = pad.left + slot * index + slot / 2, invested = Number(a.valorInvestido), current = Number(a.valorAtual);
        drawPositiveBar(ctx, x - barW - 2, height - pad.bottom, barW, invested / max * plotH, '#65737d');
        drawPositiveBar(ctx, x + 2, height - pad.bottom, barW, current / max * plotH, '#55d6a4');
        ctx.fillStyle = '#839199'; ctx.textAlign = 'center'; ctx.fillText(a.ticker, x, height - 14);
    });
    ctx.textAlign = 'start';
}

function drawPositiveBar(ctx, x, baseline, width, height, color) {
    ctx.fillStyle = color; ctx.beginPath(); ctx.roundRect(x, baseline - height, width, Math.max(2, height), 4); ctx.fill();
}

function money(currency, value) { return `${currency === 'BRL' ? 'R$' : 'US$'} ${brNumber.format(Number(value || 0))}`; }
function signedMoney(currency, value) { return `${Number(value) > 0 ? '+' : ''}${money(currency, value)}`; }
function signedPercent(value) { return `${Number(value) > 0 ? '+' : ''}${brNumber.format(Number(value || 0))}%`; }
function tone(value) { return Number(value) >= 0 ? 'positive' : 'negative'; }
function setMoneyAndTone(id, currency, value) { const el = document.getElementById(id); el.textContent = signedMoney(currency, value); el.className = tone(value); }
function setPercentAndTone(id, value) { const el = document.getElementById(id); el.textContent = signedPercent(value); el.className = tone(value); }
function setText(id, value) { document.getElementById(id).textContent = value ?? '—'; }
function escapeHtml(value) { const div = document.createElement('div'); div.textContent = value ?? ''; return div.innerHTML; }

let resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => { if (dashboardData) renderDashboard(dashboardData); }, 180);
});
