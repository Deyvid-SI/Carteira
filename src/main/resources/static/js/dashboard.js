let colors = chartPalette();
const brNumber = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const qtyNumber = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });
const compactNumber = new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 });
const brDate = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const animacao = window.CarteiraAnimacao;
let dashboardData = null;

window.addEventListener('carteira:themechange', () => {
    colors = chartPalette();
    if (dashboardData) renderDashboard(dashboardData);
});

fetch('/dashboard/dados', { headers: { Accept: 'application/json' } })
    .then(response => {
        if (!response.ok) throw new Error('Falha ao carregar o dashboard');
        return response.json();
    })
    .then(data => { dashboardData = data; renderDashboard(data, true); })
    .catch(() => {
        document.getElementById('ativosBody').innerHTML = '<tr><td colspan="10" class="table-empty">Não foi possível carregar os dados.</td></tr>';
    });

document.getElementById('positionCurrency').addEventListener('change', event => {
    if (dashboardData) drawPositionComparison(dashboardData.ativos || [], event.target.value, true);
});

document.getElementById('averagePriceCurrency').addEventListener('change', event => {
    if (dashboardData) renderAverageCenter(dashboardData, event.target.value, true);
});

document.getElementById('averageAssetSelect').addEventListener('change', event => {
    if (dashboardData) drawAverageHistory(dashboardData.precosMediosAcumulados || [], event.target.value, true);
});

// Ao voltar para o dashboard pelo histórico do navegador, os indicadores e gráficos sobem de novo.
window.addEventListener('pageshow', event => {
    if (event.persisted && dashboardData) renderDashboard(dashboardData, true);
});

// animate = true: indicadores e gráficos começam zerados e sobem até o valor real ao aparecer na tela.
// Redesenhos por tema ou tamanho da janela usam false e mostram o valor final.
function renderDashboard(data, animate = false) {
    setCount('totalAtivos', data.totalAtivos, animate);
    setCount('totalCorretoras', data.totalCorretoras, animate);
    setCount('ativosBrasil', data.ativosBrasil, animate);
    setCount('ativosUsa', data.ativosEstadosUnidos, animate);
    renderFinancial(data.resumosPorMoeda || [], animate);
    renderTable(data.ativos || []);

    drawLineChart('lineChart', 'lineEmpty', 'lineLegend', data.series || [], 'valores', '', false, animate);
    drawLineChart('performanceChart', 'performanceEmpty', 'performanceLegend', data.desempenhos || [], 'percentuais', '%', true, animate);
    drawDonut('marketChart', 'marketTotal', 'marketLegend', data.distribuicaoMercados || {}, false, animate);
    drawDonut('allocationBrlChart', 'allocationBrlTotal', 'allocationBrlLegend', data.alocacaoPorMoeda?.BRL || {}, true, animate);
    drawDonut('allocationUsdChart', 'allocationUsdTotal', 'allocationUsdLegend', data.alocacaoPorMoeda?.USD || {}, true, animate);
    drawBarChart('resultChart', 'resultEmpty', data.ativos || [], item => item.rentabilidade, '%', animate);
    drawBarChart('dailyChart', 'dailyEmpty', data.ativos || [], item => item.variacao, '%', animate);

    const averageSelected = document.getElementById('averagePriceCurrency');
    if (!(data.ativos || []).some(a => a.moeda === averageSelected.value)) {
        const firstAverageCurrency = (data.ativos || [])[0]?.moeda;
        if (firstAverageCurrency) averageSelected.value = firstAverageCurrency;
    }
    renderAverageCenter(data, averageSelected.value, animate);

    const selected = document.getElementById('positionCurrency');
    if (!(data.ativos || []).some(a => a.moeda === selected.value)) {
        const firstCurrency = (data.ativos || [])[0]?.moeda;
        if (firstCurrency) selected.value = firstCurrency;
    }
    drawPositionComparison(data.ativos || [], selected.value, animate);
}

function renderAverageCenter(data, currency, animate = false) {
    const ativos = data.ativos || [];
    const series = data.precosMediosAcumulados || [];
    const count = ativos.filter(item => item.moeda === currency).length;
    setText('averageAssetCount', `${count} ${count === 1 ? 'ativo' : 'ativos'} · ${currency}`);
    renderAverageLogoStrip(ativos, currency);
    drawAveragePrices(series, currency, animate);
    drawAverageComparison(ativos, currency, animate);
    drawAverageDistance(ativos, currency, animate);
    updateAverageAssetSelect(series, currency);
    drawAverageHistory(series, document.getElementById('averageAssetSelect').value, animate);
}

function renderAverageLogoStrip(ativos, currency) {
    const entries = ativos.filter(item => item.moeda === currency)
        .sort((a, b) => a.ticker.localeCompare(b.ticker, 'pt-BR'));
    document.getElementById('averageLogoStrip').innerHTML = entries.map(item => `
        <span class="company-chip">${assetLogoMarkup(item)}<span><strong>${escapeHtml(item.ticker)}</strong><small>${escapeHtml(item.nome)}</small></span></span>
    `).join('');
}

function drawAveragePrices(series, currency, animate = false) {
    const filtered = series.filter(item => item.moeda === currency)
        .sort((a, b) => a.ticker.localeCompare(b.ticker, 'pt-BR'));
    const entries = filtered.map(item => ({
        ticker: item.ticker,
        moeda: item.moeda,
        value: Number(item.valores?.at(-1) || 0)
    }));
    document.getElementById('averagePriceEmpty').style.display = entries.length ? 'none' : 'grid';
    prepareScrollableCanvas('averagePriceChartWrap', entries.length, 82);
    const surface = setupCanvas('averagePriceChart');
    const { ctx, width, height } = surface;
    const legend = document.getElementById('averagePriceLegend');
    if (!entries.length) {
        animacao.parar(surface.canvas);
        legend.innerHTML = '';
        return;
    }

    const pad = { left: 58, right: 18, top: 28, bottom: 42 };
    const max = Math.max(1, ...entries.map(entry => entry.value)) * 1.12;
    const plotW = width - pad.left - pad.right;
    const plotH = height - pad.top - pad.bottom;
    const slot = plotW / entries.length;
    const barW = Math.min(54, slot * .56);
    const muted = cssColor('--chart-muted'), text = cssColor('--chart-text');

    animateCanvas(surface, progress => {
        drawGrid(ctx, width, height, pad, 0, max, '');
        entries.forEach((entry, index) => {
            const x = pad.left + slot * index + (slot - barW) / 2;
            const barHeight = entry.value * progress / max * plotH;
            const gradient = ctx.createLinearGradient(0, height - pad.bottom - barHeight, 0, height - pad.bottom);
            gradient.addColorStop(0, colors[0]);
            gradient.addColorStop(1, colors[0] + '70');
            drawPositiveBar(ctx, x, height - pad.bottom, barW, barHeight, gradient);
            ctx.fillStyle = muted;
            ctx.font = '10px Inter, system-ui';
            ctx.textAlign = 'center';
            ctx.fillText(entry.ticker, x + barW / 2, height - 15);
            ctx.fillStyle = text;
            ctx.font = '600 10px Inter, system-ui';
            ctx.fillText(brNumber.format(animacao.interpolar(entry.value, progress)), x + barW / 2, Math.max(13, height - pad.bottom - barHeight - 8));
        });
        ctx.textAlign = 'start';
    }, animate);
    legend.innerHTML = `<span><i style="background:${colors[0]}"></i>${escapeHtml(currency)} · todos os preços médios atuais</span>`;
}

function drawAverageComparison(ativos, currency, animate = false) {
    const entries = ativos.filter(item => item.moeda === currency)
        .sort((a, b) => a.ticker.localeCompare(b.ticker, 'pt-BR'))
        .map(item => ({ ticker: item.ticker, average: Number(item.precoMedio || 0), quote: Number(item.cotacao || 0) }));
    document.getElementById('averageComparisonEmpty').style.display = entries.length ? 'none' : 'grid';
    prepareScrollableCanvas('averageComparisonChartWrap', entries.length, 108);
    const surface = setupCanvas('averageComparisonChart');
    const { ctx, width, height } = surface;
    if (!entries.length) {
        animacao.parar(surface.canvas);
        return;
    }

    const pad = { left: 58, right: 18, top: 34, bottom: 42 };
    const max = Math.max(1, ...entries.flatMap(entry => [entry.average, entry.quote])) * 1.16;
    const plotW = width - pad.left - pad.right;
    const plotH = height - pad.top - pad.bottom;
    const slot = plotW / entries.length;
    const barW = Math.min(26, slot * .27);
    const muted = cssColor('--chart-muted');

    animateCanvas(surface, progress => {
        drawGrid(ctx, width, height, pad, 0, max, '');
        entries.forEach((entry, index) => {
            const center = pad.left + slot * index + slot / 2;
            const averageHeight = entry.average * progress / max * plotH;
            const quoteHeight = entry.quote * progress / max * plotH;
            drawPositiveBar(ctx, center - barW - 2, height - pad.bottom, barW, averageHeight, colors[1]);
            drawPositiveBar(ctx, center + 2, height - pad.bottom, barW, quoteHeight, colors[0]);
            ctx.fillStyle = muted;
            ctx.font = '10px Inter, system-ui';
            ctx.textAlign = 'center';
            ctx.fillText(entry.ticker, center, height - 14);
            ctx.font = '600 8px Inter, system-ui';
            ctx.fillStyle = colors[1];
            ctx.fillText(brNumber.format(animacao.interpolar(entry.average, progress)), center - barW / 2 - 2, Math.max(11, height - pad.bottom - averageHeight - 7));
            ctx.fillStyle = colors[0];
            ctx.fillText(brNumber.format(animacao.interpolar(entry.quote, progress)), center + barW / 2 + 2, Math.max(11, height - pad.bottom - quoteHeight - 7));
        });
        ctx.textAlign = 'start';
    }, animate);
}

function drawAverageDistance(ativos, currency, animate = false) {
    const entries = ativos.filter(item => item.moeda === currency)
        .sort((a, b) => a.ticker.localeCompare(b.ticker, 'pt-BR'))
        .map(item => {
            const average = Number(item.precoMedio || 0);
            const quote = Number(item.cotacao || 0);
            return { ticker: item.ticker, value: average > 0 ? (quote - average) * 100 / average : 0 };
        });
    document.getElementById('averageDistanceEmpty').style.display = entries.length ? 'none' : 'grid';
    prepareScrollableCanvas('averageDistanceChartWrap', entries.length, 90);
    const surface = setupCanvas('averageDistanceChart');
    const { ctx, width, height } = surface;
    if (!entries.length) {
        animacao.parar(surface.canvas);
        return;
    }

    const pad = { left: 58, right: 18, top: 30, bottom: 42 };
    let min = Math.min(0, ...entries.map(entry => entry.value));
    let max = Math.max(0, ...entries.map(entry => entry.value));
    if (min === max) max = 1;
    const margin = Math.max(1, (max - min) * .12);
    if (max > 0) max += margin;
    if (min < 0) min -= margin;
    const plotW = width - pad.left - pad.right;
    const plotH = height - pad.top - pad.bottom;
    const zeroY = pad.top + max * plotH / (max - min);
    const slot = plotW / entries.length;
    const barW = Math.min(42, slot * .52);
    const positive = cssColor('--positive'), danger = cssColor('--danger');
    const muted = cssColor('--chart-muted'), axis = cssColor('--line-strong');

    animateCanvas(surface, progress => {
        drawGrid(ctx, width, height, pad, min, max, '%');
        ctx.beginPath();
        ctx.moveTo(pad.left, zeroY);
        ctx.lineTo(width - pad.right, zeroY);
        ctx.strokeStyle = axis;
        ctx.stroke();
        entries.forEach((entry, index) => {
            const x = pad.left + slot * index + (slot - barW) / 2;
            const y = pad.top + (max - entry.value * progress) * plotH / (max - min);
            const top = Math.min(y, zeroY);
            const barHeight = Math.max(2, Math.abs(zeroY - y));
            const color = entry.value >= 0 ? positive : danger;
            const shown = animacao.interpolar(entry.value, progress);
            ctx.fillStyle = color;
            ctx.beginPath();
            ctx.roundRect(x, top, barW, barHeight, 4);
            ctx.fill();
            ctx.fillStyle = muted;
            ctx.font = '10px Inter, system-ui';
            ctx.textAlign = 'center';
            ctx.fillText(entry.ticker, x + barW / 2, height - 14);
            ctx.fillStyle = color;
            ctx.font = '600 9px Inter, system-ui';
            const labelY = entry.value >= 0 ? Math.max(11, top - 7) : Math.min(height - pad.bottom + 14, top + barHeight + 12);
            ctx.fillText(`${shown > 0 ? '+' : ''}${brNumber.format(shown)}%`, x + barW / 2, labelY);
        });
        ctx.textAlign = 'start';
    }, animate);
}

function updateAverageAssetSelect(series, currency) {
    const select = document.getElementById('averageAssetSelect');
    const previous = select.value;
    const filtered = series.filter(item => item.moeda === currency)
        .sort((a, b) => a.ticker.localeCompare(b.ticker, 'pt-BR'));
    select.replaceChildren(...filtered.map(item => new Option(item.ticker, item.ticker)));
    if (filtered.some(item => item.ticker === previous)) select.value = previous;
    select.disabled = filtered.length === 0;
}

function drawAverageHistory(series, ticker, animate = false) {
    const selected = series.find(item => item.ticker === ticker);
    drawLineChart('averageHistoryChart', 'averageHistoryEmpty', 'averageHistoryLegend',
        selected ? [selected] : [], 'valores', '', false, animate);
    if (!selected) return;
    const current = Number(selected.valores?.at(-1) || 0);
    document.getElementById('averageHistoryLegend').innerHTML = `
        <span><i style="background:${colors[0]}"></i>${escapeHtml(selected.ticker)} · atual: ${escapeHtml(money(selected.moeda, current))}</span>
        <span class="average-rule-note">Vendas não alteram esta linha</span>`;
}

function prepareScrollableCanvas(wrapperId, itemCount, slotWidth) {
    const wrapper = document.getElementById(wrapperId);
    const viewportWidth = wrapper.parentElement.clientWidth || 280;
    wrapper.style.width = `${Math.max(viewportWidth, 76 + itemCount * slotWidth)}px`;
}

function renderFinancial(resumos, animate = false) {
    ['BRL', 'USD'].forEach(currency => {
        const summary = resumos.find(r => r.moeda === currency) || {};
        setNumber(`investido${currency}`, summary.valorInvestido || 0, value => brNumber.format(value), animate);
        setNumber(`atual${currency}`, summary.valorAtual || 0, value => brNumber.format(value), animate);
        setMoneyAndTone(`resultado${currency}`, currency, summary.resultado || 0, animate);
        setPercentAndTone(`rentabilidade${currency}`, summary.rentabilidade || 0, animate);
        setMoneyAndTone(`dia${currency}`, currency, summary.resultadoDia || 0, animate);
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
            <td><div class="asset-cell">${assetLogoMarkup(a)}<span><strong>${escapeHtml(a.ticker)}</strong><small>${escapeHtml(a.nome)}</small></span></div></td>
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
    return { canvas, ctx, width, height };
}

// Desenha o gráfico no progresso p (0 = zerado, 1 = valor real), limpando o quadro anterior.
function animateCanvas({ canvas, ctx, width, height }, draw, animate) {
    animacao.animar(canvas, progress => {
        ctx.clearRect(0, 0, width, height);
        draw(progress);
    }, { animar: animate });
}

function drawLineChart(canvasId, emptyId, legendId, series, key, suffix, includeZero = false, animate = false) {
    const valid = series.filter(s => s[key] && s[key].length);
    document.getElementById(emptyId).style.display = valid.length ? 'none' : 'grid';
    const surface = setupCanvas(canvasId);
    const { ctx, width, height } = surface;
    if (!valid.length) {
        animacao.parar(surface.canvas);
        return;
    }
    const pad = { left: 52, right: 18, top: 20, bottom: 34 };
    const values = valid.flatMap(s => s[key].map(Number));
    let min = Math.min(...values), max = Math.max(...values);
    if (includeZero) { min = Math.min(0, min); max = Math.max(0, max); }
    if (min === max) { min -= Math.max(1, Math.abs(min * .05)); max += Math.max(1, Math.abs(max * .05)); }
    const plotW = width - pad.left - pad.right, plotH = height - pad.top - pad.bottom;
    // As linhas sobem a partir do zero (ou da borda do gráfico mais próxima dele).
    const baseline = Math.min(height - pad.bottom, Math.max(pad.top, pad.top + max * plotH / (max - min)));
    const pointColor = cssColor('--chart-point');
    const lines = valid.map((serie, index) => {
        const points = serie[key].map(Number);
        return {
            serie,
            color: colors[index % colors.length],
            coords: points.map((value, i) => ({
                x: pad.left + (points.length === 1 ? plotW / 2 : i * plotW / (points.length - 1)),
                y: pad.top + (max - value) * plotH / (max - min)
            }))
        };
    });

    animateCanvas(surface, progress => {
        drawGrid(ctx, width, height, pad, min, max, suffix);
        lines.forEach(({ serie, color, coords: finalCoords }, index) => {
            const coords = finalCoords.map(p => ({ x: p.x, y: baseline + (p.y - baseline) * progress }));
            const gradient = ctx.createLinearGradient(0, pad.top, 0, height - pad.bottom);
            gradient.addColorStop(0, color + '30'); gradient.addColorStop(1, color + '00');
            if (coords.length > 1) {
                ctx.beginPath(); coords.forEach((p, i) => i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y));
                ctx.lineTo(coords.at(-1).x, height - pad.bottom); ctx.lineTo(coords[0].x, height - pad.bottom); ctx.closePath(); ctx.fillStyle = gradient; ctx.fill();
            }
            ctx.beginPath(); coords.forEach((p, i) => i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y));
            ctx.strokeStyle = color; ctx.lineWidth = 2.3; ctx.stroke();
            coords.forEach(p => { ctx.beginPath(); ctx.arc(p.x, p.y, 2.7, 0, Math.PI * 2); ctx.fillStyle = pointColor; ctx.fill(); ctx.strokeStyle = color; ctx.lineWidth = 1.8; ctx.stroke(); });
            if (index === 0) drawDateLabels(ctx, serie.labels || [], coords, height);
        });
    }, animate);
    document.getElementById(legendId).innerHTML = valid.map((s, i) => `<span><i style="background:${colors[i % colors.length]}"></i>${escapeHtml(s.ticker)}${s.moeda ? ` · ${escapeHtml(s.moeda)}` : ''}</span>`).join('');
}

function drawGrid(ctx, width, height, pad, min, max, suffix) {
    const plotH = height - pad.top - pad.bottom;
    ctx.font = '10px Inter, system-ui'; ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
        const y = pad.top + (plotH / 4) * i, value = max - ((max - min) / 4) * i;
        ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(width - pad.right, y); ctx.strokeStyle = cssColor('--chart-grid'); ctx.stroke();
        ctx.fillStyle = cssColor('--chart-muted'); ctx.textAlign = 'right'; ctx.fillText(`${brNumber.format(value)}${suffix}`, pad.left - 7, y + 4);
    }
    ctx.textAlign = 'start';
}

function drawDateLabels(ctx, labels, coords, height) {
    if (!labels.length || !coords.length) return;
    ctx.fillStyle = cssColor('--chart-muted'); ctx.textAlign = 'center';
    [0, Math.floor((labels.length - 1) / 2), labels.length - 1].filter((v, i, a) => a.indexOf(v) === i)
        .forEach(i => ctx.fillText(labels[i], coords[i].x, height - 10));
    ctx.textAlign = 'start';
}

function drawDonut(canvasId, totalId, legendId, distribution, monetary, animate = false) {
    const entries = Object.entries(distribution).filter(([, value]) => Number(value) > 0).slice(0, 8);
    const total = entries.reduce((sum, [, value]) => sum + Number(value), 0);
    const surface = setupCanvas(canvasId);
    const { ctx, width, height } = surface;
    const radius = Math.min(width, height) * .34, inner = radius * .65, cx = width / 2, cy = height / 2;
    if (!total) {
        animacao.parar(surface.canvas);
        ctx.beginPath(); ctx.arc(cx, cy, radius, 0, Math.PI * 2); ctx.arc(cx, cy, inner, 0, Math.PI * 2, true); ctx.fillStyle = cssColor('--surface-hover'); ctx.fill();
    } else {
        // As fatias crescem juntas no sentido horário, a partir do topo.
        animateCanvas(surface, progress => {
            let angle = -Math.PI / 2;
            entries.forEach(([, value], index) => {
                const slice = (Number(value) / total) * Math.PI * 2 * progress;
                ctx.beginPath(); ctx.arc(cx, cy, radius, angle, angle + slice); ctx.arc(cx, cy, inner, angle + slice, angle, true); ctx.closePath(); ctx.fillStyle = colors[index]; ctx.fill(); angle += slice;
            });
        }, animate);
    }
    animacao.contar(document.getElementById(totalId), total,
        value => monetary ? compactNumber.format(value) : String(value), { animar: animate, casas: monetary ? 2 : 0 });
    document.getElementById(legendId).innerHTML = entries.length ? entries.map(([label, value], i) => {
        const percent = total ? Number(value) / total * 100 : 0;
        return `<div><span><i style="background:${colors[i]}"></i>${escapeHtml(label)}</span><strong>${monetary ? compactNumber.format(value) : value} <small>${brNumber.format(percent)}%</small></strong></div>`;
    }).join('') : '<div class="legend-empty">Sem posições nesta moeda</div>';
}

function drawBarChart(canvasId, emptyId, items, valueGetter, suffix, animate = false) {
    const entries = items.slice(0, 10).map(item => ({ label: item.ticker, value: Number(valueGetter(item) || 0) }));
    document.getElementById(emptyId).style.display = entries.length ? 'none' : 'grid';
    const surface = setupCanvas(canvasId);
    const { ctx, width, height } = surface;
    if (!entries.length) {
        animacao.parar(surface.canvas);
        return;
    }
    const pad = { left: 40, right: 12, top: 18, bottom: 38 };
    let min = Math.min(0, ...entries.map(e => e.value)), max = Math.max(0, ...entries.map(e => e.value));
    if (min === max) max = 1;
    const plotW = width - pad.left - pad.right, plotH = height - pad.top - pad.bottom;
    const zeroY = pad.top + max * plotH / (max - min);
    const slot = plotW / entries.length, barW = Math.min(34, slot * .58);
    const positive = cssColor('--positive'), danger = cssColor('--danger');
    const muted = cssColor('--chart-muted'), axis = cssColor('--line-strong');

    animateCanvas(surface, progress => {
        ctx.beginPath(); ctx.moveTo(pad.left, zeroY); ctx.lineTo(width - pad.right, zeroY); ctx.strokeStyle = axis; ctx.stroke();
        entries.forEach((entry, index) => {
            const y = pad.top + (max - entry.value * progress) * plotH / (max - min), top = Math.min(y, zeroY), barH = Math.max(2, Math.abs(zeroY - y));
            const color = entry.value >= 0 ? positive : danger;
            const shown = animacao.interpolar(entry.value, progress);
            ctx.fillStyle = color; ctx.beginPath(); ctx.roundRect(pad.left + slot * index + (slot - barW) / 2, top, barW, barH, 4); ctx.fill();
            ctx.fillStyle = muted; ctx.font = '10px Inter, system-ui'; ctx.textAlign = 'center'; ctx.fillText(entry.label, pad.left + slot * index + slot / 2, height - 14);
            ctx.fillStyle = color; ctx.fillText(`${shown > 0 ? '+' : ''}${brNumber.format(shown)}${suffix}`, pad.left + slot * index + slot / 2, entry.value >= 0 ? Math.max(11, top - 5) : Math.min(height - pad.bottom + 14, top + barH + 12));
        });
        ctx.textAlign = 'start';
    }, animate);
}

function drawPositionComparison(ativos, currency, animate = false) {
    const entries = ativos.filter(a => a.moeda === currency).slice(0, 10);
    document.getElementById('positionEmpty').style.display = entries.length ? 'none' : 'grid';
    const surface = setupCanvas('positionChart');
    const { ctx, width, height } = surface;
    if (!entries.length) {
        animacao.parar(surface.canvas);
        return;
    }
    const pad = { left: 56, right: 18, top: 24, bottom: 42 };
    const max = Math.max(1, ...entries.flatMap(a => [Number(a.valorInvestido), Number(a.valorAtual)]));
    const plotW = width - pad.left - pad.right, plotH = height - pad.top - pad.bottom;
    const slot = plotW / entries.length, barW = Math.min(24, slot * .28);
    const grid = cssColor('--chart-grid'), muted = cssColor('--chart-muted');

    animateCanvas(surface, progress => {
        for (let i = 0; i <= 4; i++) {
            const y = pad.top + i * plotH / 4;
            ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(width - pad.right, y); ctx.strokeStyle = grid; ctx.stroke();
            ctx.fillStyle = muted; ctx.font = '10px Inter, system-ui'; ctx.textAlign = 'right'; ctx.fillText(compactNumber.format(max - i * max / 4), pad.left - 7, y + 4);
        }
        entries.forEach((a, index) => {
            const x = pad.left + slot * index + slot / 2, invested = Number(a.valorInvestido), current = Number(a.valorAtual);
            drawPositiveBar(ctx, x - barW - 2, height - pad.bottom, barW, invested * progress / max * plotH, muted);
            drawPositiveBar(ctx, x + 2, height - pad.bottom, barW, current * progress / max * plotH, colors[0]);
            ctx.fillStyle = muted; ctx.textAlign = 'center'; ctx.fillText(a.ticker, x, height - 14);
        });
        ctx.textAlign = 'start';
    }, animate);
}

function drawPositiveBar(ctx, x, baseline, width, height, color) {
    ctx.fillStyle = color; ctx.beginPath(); ctx.roundRect(x, baseline - height, width, Math.max(2, height), 4); ctx.fill();
}

function assetLogoMarkup(asset) {
    const initial = escapeHtml((asset.ticker || '?').charAt(0));
    if (!asset.logoUrl) {
        return `<span class="ticker-mark logo-mark"><b class="logo-fallback">${initial}</b></span>`;
    }
    return `<span class="ticker-mark logo-mark"><img src="${escapeHtml(asset.logoUrl)}" alt="Logo da ${escapeHtml(asset.nome || asset.ticker)}" loading="lazy" referrerpolicy="no-referrer" onerror="this.hidden=true;this.nextElementSibling.hidden=false"><b class="logo-fallback" hidden>${initial}</b></span>`;
}

function cssColor(variable) {
    return getComputedStyle(document.documentElement).getPropertyValue(variable).trim();
}

function chartPalette() {
    const light = document.documentElement.dataset.theme === 'light';
    return [
        cssColor('--accent') || (light ? '#a87913' : '#d6ad45'),
        cssColor('--blue') || (light ? '#957126' : '#c8a45a'),
        light ? '#6f5a2c' : '#ead494',
        light ? '#bd7b32' : '#d9974d',
        light ? '#54778a' : '#6ca6bd',
        cssColor('--danger') || '#ec7782',
        light ? '#6f813e' : '#9aae61',
        light ? '#8d6046' : '#bc8060'
    ];
}

function money(currency, value) { return `${currency === 'BRL' ? 'R$' : 'US$'} ${brNumber.format(Number(value || 0))}`; }
function signedMoney(currency, value) { return `${Number(value) > 0 ? '+' : ''}${money(currency, value)}`; }
function signedPercent(value) { return `${Number(value) > 0 ? '+' : ''}${brNumber.format(Number(value || 0))}%`; }
function tone(value) { return Number(value) >= 0 ? 'positive' : 'negative'; }
function setNumber(id, value, format, animate) { animacao.contar(document.getElementById(id), value, format, { animar: animate }); }
function setMoneyAndTone(id, currency, value, animate) { const el = document.getElementById(id); el.className = tone(value); animacao.contar(el, value, v => signedMoney(currency, v), { animar: animate }); }
function setPercentAndTone(id, value, animate) { const el = document.getElementById(id); el.className = tone(value); animacao.contar(el, value, v => signedPercent(v), { animar: animate }); }
function setCount(id, value, animate) {
    const el = document.getElementById(id);
    if (value == null) { animacao.parar(el); el.textContent = '—'; return; }
    animacao.contar(el, value, v => String(v), { animar: animate, casas: 0 });
}
function setText(id, value) { document.getElementById(id).textContent = value ?? '—'; }
function escapeHtml(value) { const div = document.createElement('div'); div.textContent = value ?? ''; return div.innerHTML; }

let resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => { if (dashboardData) renderDashboard(dashboardData); }, 180);
});
