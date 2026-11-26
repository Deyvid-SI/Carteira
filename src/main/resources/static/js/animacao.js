// Efeito de entrada dos indicadores e gráficos: começam zerados e sobem até o valor real
// quando aparecem na tela. Com "Movimento reduzido" (data-motion="reduced") o valor aparece direto.
(() => {
    const DURACAO = 1100;
    const SELETOR_INDICADORES = '.summary-card strong, .summary-card em';
    // Um único número em formato pt-BR (1.234,56 ou 1234,56), com texto antes e depois sem outros dígitos.
    const NUMERO = /^(\D*?)(-?(?:\d{1,3}(?:\.\d{3})+|\d+)(?:,\d+)?)(\D*)$/;
    const estados = new WeakMap();
    const textosOriginais = new WeakMap();

    function reduzido() {
        return document.documentElement.dataset.motion === 'reduced';
    }

    function suavizar(t) {
        return 1 - Math.pow(1 - t, 3);
    }

    function progressoEm(inicio, agora) {
        return Math.min(1, Math.max(0, (agora - inicio) / DURACAO));
    }

    function arredondar(valor, casas) {
        const fator = 10 ** casas;
        return Math.round(valor * fator) / fator + 0;
    }

    // Valor intermediário para exibir no progresso p, sem "-0,00" no começo da contagem.
    function interpolar(valor, progresso, casas = 2) {
        return progresso === 1 ? Number(valor) : arredondar(Number(valor) * progresso, casas);
    }

    // Considera visível quando pelo menos metade da altura aparece. A largura não entra na conta porque
    // alguns gráficos ficam dentro de áreas com rolagem horizontal e nunca aparecem inteiros.
    function quandoVisivel(elemento, callback) {
        if (!('IntersectionObserver' in window)) {
            callback();
            return () => {};
        }
        const observador = new IntersectionObserver(entradas => {
            const visivel = entradas.some(entrada => entrada.isIntersecting
                && entrada.intersectionRect.height >= entrada.boundingClientRect.height * .5);
            if (!visivel) return;
            observador.disconnect();
            callback();
        }, { threshold: Array.from({ length: 21 }, (_, i) => i / 20) });
        observador.observe(elemento);
        return () => observador.disconnect();
    }

    function parar(elemento) {
        estados.get(elemento)?.cancelar();
        estados.set(elemento, { concluido: true, cancelar() {} });
    }

    /**
     * Chama pintar(p) com p de 0 (zerado) a 1 (valor real) a partir do momento em que o elemento aparece.
     * Com animar = false, só desenha o valor final, exceto se a entrada anterior ainda não terminou
     * (um redesenho por tema ou tamanho da janela continua a animação em vez de cortá-la).
     */
    function animar(elemento, pintar, { animar: pedido = true } = {}) {
        const anterior = estados.get(elemento);
        anterior?.cancelar();
        const pendente = Boolean(anterior && !anterior.concluido);
        if (reduzido() || !(pedido || pendente)) {
            estados.set(elemento, { concluido: true, cancelar() {} });
            pintar(1);
            return;
        }

        const estado = { concluido: false, inicio: pedido ? null : anterior.inicio, quadro: 0, pararEspera: null };
        estado.cancelar = () => {
            estado.pararEspera?.();
            cancelAnimationFrame(estado.quadro);
        };
        estados.set(elemento, estado);

        const quadro = agora => {
            const t = progressoEm(estado.inicio, agora);
            pintar(t === 1 ? 1 : suavizar(t));
            if (t < 1) estado.quadro = requestAnimationFrame(quadro);
            else estado.concluido = true;
        };
        const comecar = () => {
            estado.pararEspera = null;
            if (estado.inicio == null) estado.inicio = performance.now();
            estado.quadro = requestAnimationFrame(quadro);
        };

        if (estado.inicio == null) {
            pintar(0);
            estado.pararEspera = quandoVisivel(elemento, comecar);
        } else {
            pintar(suavizar(progressoEm(estado.inicio, performance.now())));
            comecar();
        }
    }

    function contar(elemento, valorFinal, formatar, { animar: pedido = true, casas = 2 } = {}) {
        animar(elemento, progresso => {
            elemento.textContent = formatar(interpolar(valorFinal, progresso, casas));
        }, { animar: pedido });
    }

    // Indicadores escritos pelo servidor: lê o número do próprio texto e conta a partir de zero.
    function contarTexto(elemento) {
        if (!textosOriginais.has(elemento)) textosOriginais.set(elemento, elemento.textContent);
        const original = textosOriginais.get(elemento);
        const partes = NUMERO.exec(original);
        if (!partes) return;
        const [, prefixo, numero, sufixo] = partes;
        const casas = numero.includes(',') ? numero.split(',')[1].length : 0;
        const valor = Number(numero.replace(/\./g, '').replace(',', '.'));
        if (!Number.isFinite(valor) || valor === 0) return;
        const formato = new Intl.NumberFormat('pt-BR', {
            minimumFractionDigits: casas,
            maximumFractionDigits: casas,
            useGrouping: numero.includes('.')
        });
        animar(elemento, progresso => {
            elemento.textContent = progresso === 1
                ? original
                : `${prefixo}${formato.format(interpolar(valor, progresso, casas))}${sufixo}`;
        });
    }

    function iniciarIndicadores() {
        document.querySelectorAll(SELETOR_INDICADORES).forEach(contarTexto);
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', iniciarIndicadores);
    else iniciarIndicadores();

    // Ao voltar para a página pelo histórico do navegador, os indicadores aparecem de novo.
    window.addEventListener('pageshow', evento => {
        if (evento.persisted) iniciarIndicadores();
    });

    window.CarteiraAnimacao = { animar, contar, parar, interpolar, reduzido };
})();
