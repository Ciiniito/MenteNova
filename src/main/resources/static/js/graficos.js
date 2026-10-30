window.MenteNovaGraficos = (function () {
    'use strict';

    const COLOR_PRINCIPAL = '#5b47c7';
    const COLOR_PRINCIPAL_HOVER = '#4a38a8';
    const COLORES_SERIES = ['#2a78d6', '#eb6834'];
    const REJILLA = '#ecebf1';
    const EJE = '#d9d8e0';

    const numero = new Intl.NumberFormat('es-PE');

    function configurar() {
        Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
        Chart.defaults.font.size = 12;
        Chart.defaults.color = '#5b6170';
        Chart.defaults.animation.duration = 300;
        Chart.defaults.plugins.tooltip.backgroundColor = '#161f3d';
        Chart.defaults.plugins.tooltip.padding = 10;
        Chart.defaults.plugins.tooltip.cornerRadius = 8;
        Chart.defaults.plugins.tooltip.boxPadding = 4;
        Chart.defaults.plugins.legend.labels.boxWidth = 10;
        Chart.defaults.plugins.legend.labels.boxHeight = 10;
        Chart.defaults.plugins.legend.labels.useBorderRadius = true;
        Chart.defaults.plugins.legend.labels.borderRadius = 2;
    }

    const lineaGuia = {
        id: 'lineaGuia',
        afterDatasetsDraw(chart) {
            const activos = chart.tooltip && chart.tooltip.getActiveElements();
            if (!activos || !activos.length) {
                return;
            }
            const x = activos[0].element.x;
            const area = chart.chartArea;
            const ctx = chart.ctx;
            ctx.save();
            ctx.beginPath();
            ctx.moveTo(x, area.top);
            ctx.lineTo(x, area.bottom);
            ctx.lineWidth = 1;
            ctx.strokeStyle = EJE;
            ctx.stroke();
            ctx.restore();
        }
    };

    function barrasHorizontales(idCanvas, etiquetas, valores, opciones) {
        const canvas = document.getElementById(idCanvas);
        if (!canvas || !window.Chart) {
            return null;
        }
        const formato = (opciones && opciones.formato) || (v => numero.format(v));
        const formatoEje = (opciones && opciones.formatoEje) || formato;
        return new Chart(canvas, {
            type: 'bar',
            data: {
                labels: etiquetas,
                datasets: [{
                    label: (opciones && opciones.serie) || 'Citas',
                    data: valores,
                    backgroundColor: COLOR_PRINCIPAL,
                    hoverBackgroundColor: COLOR_PRINCIPAL_HOVER,
                    borderRadius: 4,
                    borderSkipped: 'start',
                    barPercentage: 0.7,
                    categoryPercentage: 0.85,
                    maxBarThickness: 18
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {display: false},
                    tooltip: {callbacks: {label: ctx => ' ' + formato(ctx.parsed.x)}}
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        grid: {color: REJILLA},
                        border: {display: false},
                        ticks: {precision: 0, callback: v => formatoEje(v)}
                    },
                    y: {
                        grid: {display: false},
                        border: {color: EJE}
                    }
                }
            }
        });
    }

    function lineasPorcentaje(idCanvas, etiquetas, series) {
        const canvas = document.getElementById(idCanvas);
        if (!canvas || !window.Chart) {
            return null;
        }
        return new Chart(canvas, {
            type: 'line',
            data: {
                labels: etiquetas,
                datasets: series.map((serie, i) => ({
                    label: serie.nombre,
                    data: serie.valores,
                    borderColor: COLORES_SERIES[i],
                    backgroundColor: COLORES_SERIES[i],
                    borderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    pointBorderColor: '#ffffff',
                    pointBorderWidth: 2,
                    cubicInterpolationMode: 'monotone',
                    spanGaps: false
                }))
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {mode: 'index', intersect: false},
                plugins: {
                    legend: {position: 'top', align: 'start'},
                    tooltip: {
                        callbacks: {
                            label: ctx => ' ' + ctx.dataset.label + ': '
                                + (ctx.parsed.y === null ? 'sin datos' : numero.format(ctx.parsed.y) + ' %')
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        suggestedMax: 25,
                        grid: {color: REJILLA},
                        border: {display: false},
                        ticks: {callback: v => v + ' %'}
                    },
                    x: {
                        grid: {display: false},
                        border: {color: EJE},
                        ticks: {maxRotation: 0, autoSkip: true}
                    }
                }
            },
            plugins: [lineaGuia]
        });
    }

    if (window.Chart) {
        configurar();
    }

    return {barrasHorizontales, lineasPorcentaje};
})();
