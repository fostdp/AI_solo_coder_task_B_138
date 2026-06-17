import { IceCrackScene } from './ice_crack_3d.js';
import { AestheticsPanel } from './aesthetics_panel.js';
import * as api from './api.js';
import { AlertWebSocket } from './websocket.js';
import { DashboardCharts } from './charts.js';
import { initPatternComparator } from './pattern_comparator.js';
import { initEraComparator } from './era_comparator.js';
import { initPedestrianSimulator } from './pedestrian_simulator.js';
import { initVrPavingDesigner } from './vr_paving_designer.js';

let scene = null;
let aestheticsPanel = null;
let charts = null;
let ws = null;
let pavements = [];
let currentPavement = null;

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        if (toast.parentNode) toast.parentNode.removeChild(toast);
    }, 5000);
}

function populateDropdown(pavementList) {
    const select = document.getElementById('pavement-selector');
    if (!select) return;
    select.innerHTML = '';
    for (const p of pavementList) {
        const option = document.createElement('option');
        option.value = p.id;
        option.textContent = p.name || `Pavement ${p.id}`;
        select.appendChild(option);
    }
}

function populatePavementChecklist(pavementList) {
    const container = document.getElementById('pavement-checklist');
    if (!container) return;
    container.innerHTML = '';
    for (const p of pavementList) {
        const label = document.createElement('label');
        label.className = 'check-item';
        const style = p.pavementStyle || 'ICE_CRACK';
        const era = p.era || 'ANCIENT';
        label.innerHTML = `
            <input type="checkbox" class="pavement-check" value="${p.id}">
            <span>${p.name || p.id}</span>
            <span class="check-meta">${style} / ${era}</span>
        `;
        container.appendChild(label);
    }
}

function populateSensorTable(data) {
    const tbody = document.querySelector('#sensor-table tbody');
    if (!tbody) return;
    tbody.innerHTML = '';
    if (!data || data.length === 0) {
        const tr = document.createElement('tr');
        tr.innerHTML = '<td colspan="3">暂无数据</td>';
        tbody.appendChild(tr);
        return;
    }
    const latest = data[0];
    const rows = [
        ['降雨量', latest.rainfallMm != null ? latest.rainfallMm.toFixed(2) : '-', 'mm/h'],
        ['积水深度', latest.waterDepthMm != null ? latest.waterDepthMm.toFixed(2) : '-', 'mm'],
        ['裂缝宽度', latest.crackWidthMm != null ? latest.crackWidthMm.toFixed(3) : '-', 'mm'],
        ['踩踏频率', latest.stepFrequency != null ? latest.stepFrequency.toFixed(1) : '-', '次/min'],
        ['温度', latest.temperature != null ? latest.temperature.toFixed(1) : '-', '°C'],
        ['湿度', latest.humidity != null ? latest.humidity.toFixed(1) : '-', '%'],
    ];
    for (const [label, value, unit] of rows) {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${label}</td><td>${value}</td><td>${unit}</td>`;
        tbody.appendChild(tr);
    }
}

function populateSimulationPanel(result) {
    const panel = document.getElementById('simulation-results');
    if (!panel) return;
    panel.innerHTML = '';
    if (!result) return;

    const metrics = [
        { label: '积水消退时间', value: `${result.recessionTimeSec.toFixed(1)} 秒`, highlight: result.recessionTimeSec > 1800 },
        { label: '峰值积水深度', value: `${(result.peakWaterDepth * 1000).toFixed(2)} mm` },
        { label: '排水速率', value: `${result.drainageRate.toFixed(6)} m/s` },
        { label: '渗透速率', value: `${result.infiltrationRate.toFixed(6)} m/s` },
        { label: '地表径流速率', value: `${result.surfaceRunoffRate.toFixed(6)} m/s` },
        { label: '告警触发', value: result.alertTriggered ? '是' : '否', highlight: result.alertTriggered },
    ];

    const grid = document.createElement('div');
    grid.className = 'metric-grid';
    for (const m of metrics) {
        const card = document.createElement('div');
        card.className = 'result-card';
        if (m.highlight) card.style.borderLeftColor = 'var(--accent-red)';
        card.innerHTML = `<div class="label">${m.label}</div><div class="value">${m.value}</div>`;
        grid.appendChild(card);
    }
    panel.appendChild(grid);

    if (result.timeSeries) {
        charts.drawTimeSeries(result.timeSeries, 'time-series-canvas');
    }
}

function populateAlertsPanel(alerts) {
    const panel = document.getElementById('alerts-panel');
    if (!panel) return;
    panel.innerHTML = '';
    if (!alerts || alerts.length === 0) {
        panel.innerHTML = '<div style="color:var(--text-muted);font-size:12px">暂无告警</div>';
        return;
    }
    for (const alert of alerts) {
        const div = document.createElement('div');
        const severity = (alert.severity || 'WARNING').toLowerCase();
        div.className = `alert-item ${severity}`;
        const timeStr = alert.createdAt ? new Date(alert.createdAt).toLocaleString('zh-CN') : '';
        div.innerHTML = `
            <div class="alert-time">${timeStr} | ${alert.pavementName || ''}</div>
            <div class="alert-msg"><strong>[${alert.alertType || '告警'}]</strong> ${alert.message || ''}</div>
            ${!alert.acknowledged ? `<button class="btn-primary ack-btn" data-id="${alert.id}" style="margin-top:6px;padding:4px 12px;width:auto;font-size:11px">确认</button>` : ''}
        `;
        panel.appendChild(div);
    }
    panel.querySelectorAll('.ack-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-id');
            try {
                await api.acknowledgeAlert(id);
                showToast('告警已确认', 'info');
                loadAlerts();
            } catch (e) {
                showToast('确认告警失败', 'error');
            }
        });
    });
}

async function loadAlerts() {
    try {
        const alerts = await api.fetchUnacknowledgedAlerts();
        populateAlertsPanel(alerts);
    } catch (e) {
        console.error('Failed to load alerts', e);
    }
}

async function selectPavement(id) {
    try {
        const pavement = await api.fetchPavement(id);
        currentPavement = pavement;
        const sensorData = await api.fetchLatestSensorData(id, 10);
        populateSensorTable(sensorData);

        if (scene) {
            scene.clearScene();
            const length = pavement.areaLength || 10;
            const width = pavement.areaWidth || 10;
            scene.initPavement(length, width);

            let pattern = pavement.crackPattern;
            if (pattern && typeof pattern === 'string') {
                try { pattern = JSON.parse(pattern); } catch (e) { pattern = null; }
            }
            const style = pavement.pavementStyle || 'ICE_CRACK';
            scene.loadCrackStyle(style, pattern);
        }

        loadAlerts();
    } catch (e) {
        showToast('加载铺地数据失败', 'error');
    }
}

async function handleRunSimulation() {
    if (!currentPavement) {
        showToast('请先选择铺地', 'warning');
        return;
    }

    const rainfall = parseFloat(document.getElementById('rainfall-slider').value) || 50;
    const crackWidth = parseFloat(document.getElementById('crack-width').value) || 5;
    const stepFreq = parseFloat(document.getElementById('step-frequency').value) || 30;
    const gridRes = parseInt(document.getElementById('grid-resolution').value) || 20;
    const simDuration = parseFloat(document.getElementById('sim-duration').value) || 3600;
    const initDepth = parseFloat(document.getElementById('initial-water-depth').value) || 10;

    try {
        const result = await api.runSimulation({
            pavementId: currentPavement.id,
            rainfallMm: rainfall,
            initialWaterDepthMm: initDepth,
            crackWidthMm: crackWidth,
            stepFrequency: stepFreq,
            simulationDurationSec: simDuration,
            gridResolution: gridRes
        });
        populateSimulationPanel(result);
        showToast('排水仿真完成', 'info');

        if (scene && result.gridData) {
            try {
                const gridData = typeof result.gridData === 'string' ? JSON.parse(result.gridData) : result.gridData;
                scene.updateWaterSurface(gridData);
            } catch (e) {
                console.error('Failed to update water surface', e);
            }
        }
    } catch (e) {
        showToast('排水仿真失败', 'error');
    }
}

async function handleAnalyzeAesthetic() {
    if (!currentPavement) {
        showToast('请先选择铺地', 'warning');
        return;
    }
    try {
        const result = await aestheticsPanel.runAnalysis(currentPavement.id);
        showToast('美学分析完成', 'info');

        if (scene && result.crackSegments) {
            try {
                const segments = typeof result.crackSegments === 'string' ? JSON.parse(result.crackSegments) : result.crackSegments;
                const indices = segments.map((_, i) => i).filter(i => i % 3 === 0);
                scene.highlightCracks(indices);
            } catch (e) {
                console.error('Failed to highlight cracks', e);
            }
        }
    } catch (e) {
        showToast('美学分析失败', 'error');
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    const container = document.getElementById('three-container');
    if (container) {
        scene = new IceCrackScene(container);
    }

    aestheticsPanel = new AestheticsPanel();
    charts = new DashboardCharts();

    ws = new AlertWebSocket((alert) => {
        showToast(`[${alert.alertType || '告警'}] ${alert.message || ''}`, alert.severity === 'HIGH' ? 'error' : 'warning');
        loadAlerts();
    });

    const rainfallSlider = document.getElementById('rainfall-slider');
    const rainfallValue = document.getElementById('rainfall-value');
    if (rainfallSlider && rainfallValue) {
        rainfallSlider.addEventListener('input', () => {
            rainfallValue.textContent = rainfallSlider.value;
        });
    }

    try {
        pavements = await api.fetchPavements();
        populateDropdown(pavements);
        populatePavementChecklist(pavements);
        if (pavements.length > 0) {
            await selectPavement(pavements[0].id);
        }
    } catch (e) {
        showToast('加载铺地列表失败', 'error');
    }

    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            btn.classList.add('active');
            const tab = btn.getAttribute('data-tab');
            const content = document.getElementById(`tab-${tab}`);
            if (content) content.classList.add('active');
        });
    });

    const selectEl = document.getElementById('pavement-selector');
    if (selectEl) {
        selectEl.addEventListener('change', (e) => {
            selectPavement(e.target.value);
        });
    }

    const simBtn = document.getElementById('run-simulation');
    if (simBtn) {
        simBtn.addEventListener('click', handleRunSimulation);
    }

    const aestheticBtn = document.getElementById('run-aesthetic');
    if (aestheticBtn) {
        aestheticBtn.addEventListener('click', handleAnalyzeAesthetic);
    }

    const compareStyleBtn = document.getElementById('compare-styles');
    if (compareStyleBtn) {
        compareStyleBtn.addEventListener('click', async () => {
            const selected = Array.from(document.querySelectorAll('.pavement-check:checked')).map(cb => cb.value);
            if (selected.length < 2) {
                showToast('请至少选择2个铺地进行对比', 'warning');
                return;
            }
            const rainfall = parseFloat(document.getElementById('rainfall-slider')?.value) || 50;
            try {
                const result = await api.compareStyles(selected, { rainfallMm: rainfall });
                populateComparisonPanel(result);
                showToast('样式对比分析完成', 'info');
            } catch (e) {
                showToast('样式对比失败', 'error');
            }
        });
    }

    const compareEraBtn = document.getElementById('compare-eras');
    if (compareEraBtn) {
        compareEraBtn.addEventListener('click', async () => {
            const selected = pavements.filter(p => p.era === 'ANCIENT' || p.era === 'MODERN').map(p => p.id);
            const rainfall = parseFloat(document.getElementById('rainfall-slider')?.value) || 50;
            try {
                const result = await api.compareEras(selected, { rainfallMm: rainfall });
                populateComparisonPanel(result);
                showToast('跨时代对比分析完成', 'info');
            } catch (e) {
                showToast('跨时代对比失败', 'error');
            }
        });
    }

    const propBtn = document.getElementById('run-propagation');
    if (propBtn) {
        propBtn.addEventListener('click', async () => {
            if (!currentPavement) {
                showToast('请先选择铺地', 'warning');
                return;
            }
            const params = {
                pavementId: currentPavement.id,
                initialCrackWidthMm: parseFloat(document.getElementById('init-crack-width')?.value) || 2.0,
                stepFrequency: parseFloat(document.getElementById('step-freq-prop')?.value) || 30.0,
                totalSteps: parseInt(document.getElementById('total-steps')?.value) || 10000,
                simulationHours: parseFloat(document.getElementById('sim-hours')?.value) || 8760
            };
            try {
                const result = await api.simulateCrackPropagation(params);
                populatePropagationPanel(result);
                if (scene && result.segmentPropagation) {
                    scene.applyPropagation(result.segmentPropagation);
                }
                showToast('裂缝扩展模拟完成', 'info');
            } catch (e) {
                showToast('裂缝扩展模拟失败', 'error');
            }
        });
    }

    const designSubmitBtn = document.getElementById('submit-design');
    if (designSubmitBtn) {
        designSubmitBtn.addEventListener('click', async () => {
            const designName = document.getElementById('design-name')?.value || '我的冰裂纹设计';
            const areaLength = parseFloat(document.getElementById('design-length')?.value) || 10;
            const areaWidth = parseFloat(document.getElementById('design-width')?.value) || 10;
            const slopeAngle = parseFloat(document.getElementById('design-slope')?.value) || 2;
            const basePermeability = parseFloat(document.getElementById('design-perm')?.value) || 0.001;
            const rainfall = parseFloat(document.getElementById('design-rainfall')?.value) || 50;
            const crackWidth = parseFloat(document.getElementById('design-crack-width')?.value) || 3;
            const stepFreq = parseFloat(document.getElementById('design-step-freq')?.value) || 30;

            let segments = (window._userDesignSegments || []);
            if (segments.length === 0) {
                showToast('请先在画布上绘制冰裂纹', 'warning');
                return;
            }

            try {
                const result = await api.submitUserDesign({
                    designName,
                    crackPattern: JSON.stringify(segments),
                    areaLength,
                    areaWidth,
                    slopeAngle,
                    basePermeability,
                    rainfallMm: rainfall,
                    crackWidthMm: crackWidth,
                    stepFrequency: stepFreq,
                    runAesthetic: true,
                    runDrainage: true
                });
                populateDesignResultPanel(result);
                if (scene && segments.length > 0) {
                    scene.clearScene();
                    scene.initPavement(areaLength, areaWidth);
                    scene.loadCrackStyle('CUSTOM', segments);
                }
                showToast('设计提交成功，已完成美学与排水分析', 'info');
            } catch (e) {
                showToast('设计提交失败', 'error');
            }
        });
    }

    initUserDesignCanvas();

    initPatternComparator(pavements, scene);
    initEraComparator(pavements, scene);
    initPedestrianSimulator(pavements, scene);
    initVrPavingDesigner(scene);

    loadAlerts();
});

function populateComparisonPanel(result) {
    const panel = document.getElementById('comparison-panel');
    if (!panel) return;
    panel.innerHTML = '';
    if (!result) return;
    if (result.summary) {
        const s = document.createElement('div');
        s.className = 'comparison-summary';
        s.textContent = result.summary;
        panel.appendChild(s);
    }
    const grid = document.createElement('div');
    grid.className = 'comparison-grid';
    const aes = result.aestheticResults || [];
    const drain = result.drainageResults || [];
    const all = aes.map((a, i) => ({ ...a, ...(drain[i] || {}) }));
    for (const r of all) {
        const card = document.createElement('div');
        card.className = 'comparison-card';
        card.innerHTML = `
            <h4>${r.pavementName || ''} <span style="font-size:10px;color:var(--text-muted)">${r.pavementStyle || ''} / ${r.era || ''}</span></h4>
            <div class="compare-row"><span>视觉复杂度</span><b>${(r.visualComplexity || 0).toFixed(3)}</b></div>
            <div class="compare-row"><span>分形维数</span><b>${(r.fractalDimension || 0).toFixed(3)}</b></div>
            <div class="compare-row"><span>退水时间</span><b>${(r.recessionTimeSec || 0).toFixed(0)}s</b></div>
            <div class="compare-row"><span>渗透速率</span><b>${(r.infiltrationRate || 0).toFixed(6)}</b></div>
            ${r.alertTriggered ? '<div style="color:var(--accent-red);font-size:11px;margin-top:4px">⚠ 触发告警</div>' : ''}
        `;
        grid.appendChild(card);
    }
    panel.appendChild(grid);
}

function populatePropagationPanel(result) {
    const panel = document.getElementById('propagation-panel');
    if (!panel) return;
    panel.innerHTML = '';
    if (!result) return;
    const rows = [
        ['初始裂缝宽度', `${(result.initialCrackWidthMm || 0).toFixed(2)} mm`],
        ['最终平均宽度', `${(result.finalCrackWidthMm || 0).toFixed(2)} mm`],
        ['扩展增量', `${((result.finalCrackWidthMm || 0) - (result.initialCrackWidthMm || 0)).toFixed(2)} mm`],
        ['踩踏频率', `${(result.stepFrequency || 0).toFixed(1)} 次/min`],
        ['模拟踩踏总次', `${(result.totalSteps || 0).toLocaleString()}`],
        ['模拟时长', `${(result.simulationHours || 0).toFixed(0)} 小时`],
        ['损伤指数', `${(result.damageIndex || 0).toFixed(3)} ${(result.damageIndex || 0) > 0.5 ? '⚠ 高风险' : ''}`]
    ];
    const tbl = document.createElement('table');
    tbl.className = 'simple-table';
    for (const [k, v] of rows) {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${k}</td><td><b>${v}</b></td>`;
        tbl.appendChild(tr);
    }
    panel.appendChild(tbl);
    if (result.widthHistory) {
        const cvs = document.createElement('canvas');
        cvs.id = 'propagation-history';
        cvs.width = 500;
        cvs.height = 150;
        panel.appendChild(cvs);
        try {
            const history = typeof result.widthHistory === 'string' ? JSON.parse(result.widthHistory) : result.widthHistory;
            if (charts) charts.drawTimeSeries(history.map(p => ({ time: p.hour, avgDepth: p.avgWidthMm, maxDepth: p.maxWidthMm })), 'propagation-history');
        } catch (e) {}
    }
}

function populateDesignResultPanel(result) {
    const panel = document.getElementById('design-result-panel');
    if (!panel) return;
    panel.innerHTML = '';
    if (!result) return;
    const h = document.createElement('h4');
    h.textContent = result.designName || '设计结果';
    panel.appendChild(h);
    if (result.aestheticResult) {
        const a = result.aestheticResult;
        const div = document.createElement('div');
        div.className = 'design-result-section';
        div.innerHTML = `
            <h5>美学分析</h5>
            <div class="compare-row"><span>分形维数</span><b>${(a.fractalDimension || 0).toFixed(3)}</b></div>
            <div class="compare-row"><span>信息熵</span><b>${(a.infoEntropy || 0).toFixed(3)}</b></div>
            <div class="compare-row"><span>视觉复杂度</span><b>${(a.visualComplexity || 0).toFixed(3)}</b></div>
            <div class="compare-row"><span>裂缝数</span><b>${a.crackCount || 0}</b></div>
            <div class="compare-row"><span>裂缝密度</span><b>${(a.crackDensity || 0).toFixed(3)}</b></div>
            <div class="compare-row"><span>对称性</span><b>${(a.patternSymmetry || 0).toFixed(3)}</b></div>
        `;
        panel.appendChild(div);
    }
    if (result.drainageResult) {
        const d = result.drainageResult;
        const div = document.createElement('div');
        div.className = 'design-result-section';
        div.innerHTML = `
            <h5>排水仿真</h5>
            <div class="compare-row"><span>退水时间</span><b>${(d.recessionTimeSec || 0).toFixed(0)} s</b></div>
            <div class="compare-row"><span>峰值水深</span><b>${((d.peakWaterDepth || 0) * 1000).toFixed(2)} mm</b></div>
            <div class="compare-row"><span>排水速率</span><b>${(d.drainageRate || 0).toFixed(6)}</b></div>
            <div class="compare-row"><span>渗透速率</span><b>${(d.infiltrationRate || 0).toFixed(6)}</b></div>
            ${d.alertTriggered ? '<div style="color:var(--accent-red);font-size:11px">⚠ 排水告警</div>' : '<div style="color:var(--accent-green);font-size:11px">✓ 排水正常</div>'}
        `;
        panel.appendChild(div);
    }
}

function initUserDesignCanvas() {
    const canvas = document.getElementById('design-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let drawing = false;
    let startPoint = null;
    window._userDesignSegments = window._userDesignSegments || [];

    function resizeCanvas() {
        const wrap = canvas.parentElement;
        if (!wrap) return;
        const size = Math.min(wrap.clientWidth, 400);
        canvas.width = size;
        canvas.height = size;
        canvas.style.width = size + 'px';
        canvas.style.height = size + 'px';
        redraw();
    }

    function updateSegmentCount() {
        const el = document.getElementById('segment-count');
        if (el) el.textContent = window._userDesignSegments.length;
    }

    function redraw() {
        ctx.fillStyle = '#e8dfc8';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.strokeStyle = '#d4c9a8';
        ctx.lineWidth = 0.5;
        const gridSize = canvas.width / 10;
        for (let x = 0; x <= canvas.width; x += gridSize) {
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, canvas.height);
            ctx.stroke();
        }
        for (let y = 0; y <= canvas.height; y += gridSize) {
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(canvas.width, y);
            ctx.stroke();
        }
        ctx.strokeStyle = '#2a2520';
        ctx.lineWidth = 1.5;
        for (let i = 0; i < window._userDesignSegments.length; i++) {
            const seg = window._userDesignSegments[i];
            const sx = (seg[0][0] / 10) * canvas.width;
            const sy = (seg[0][1] / 10) * canvas.height;
            const ex = (seg[1][0] / 10) * canvas.width;
            const ey = (seg[1][1] / 10) * canvas.height;
            ctx.strokeStyle = '#2a2520';
            ctx.lineWidth = 1.5;
            ctx.beginPath();
            ctx.moveTo(sx, sy);
            ctx.lineTo(ex, ey);
            ctx.stroke();
            if (i === window._userDesignSegments.length - 1) {
                ctx.fillStyle = '#5ba88c';
                ctx.beginPath();
                ctx.arc(ex, ey, 3, 0, Math.PI * 2);
                ctx.fill();
            }
        }
        updateSegmentCount();
    }

    function drawPreview(p) {
        if (!startPoint) return;
        ctx.strokeStyle = 'rgba(42,37,32,0.4)';
        ctx.lineWidth = 1;
        ctx.setLineDash([4, 4]);
        const sx = (startPoint.x / 10) * canvas.width;
        const sy = (startPoint.y / 10) * canvas.height;
        const ex = (p.x / 10) * canvas.width;
        const ey = (p.y / 10) * canvas.height;
        ctx.beginPath();
        ctx.moveTo(sx, sy);
        ctx.lineTo(ex, ey);
        ctx.stroke();
        ctx.setLineDash([]);
        const dx = p.x - startPoint.x;
        const dy = p.y - startPoint.y;
        const len = Math.sqrt(dx * dx + dy * dy);
        if (len > 0.2) {
            ctx.fillStyle = 'rgba(91,168,140,0.8)';
            ctx.font = '11px sans-serif';
            ctx.fillText(len.toFixed(2) + 'm', (sx + ex) / 2 + 4, (sy + ey) / 2 - 4);
        }
    }

    const pos = (e) => {
        const r = canvas.getBoundingClientRect();
        const cx = (e.touches ? e.touches[0].clientX : e.clientX) - r.left;
        const cy = (e.touches ? e.touches[0].clientY : e.clientY) - r.top;
        return { x: (cx / canvas.width) * 10, y: (cy / canvas.height) * 10 };
    };

    canvas.addEventListener('mousedown', (e) => {
        drawing = true;
        startPoint = pos(e);
    });
    canvas.addEventListener('mousemove', (e) => {
        if (!drawing || !startPoint) return;
        redraw();
        drawPreview(pos(e));
    });
    canvas.addEventListener('mouseup', (e) => {
        if (!drawing || !startPoint) return;
        const p = pos(e);
        const dx = p.x - startPoint.x;
        const dy = p.y - startPoint.y;
        if (Math.sqrt(dx * dx + dy * dy) > 0.3) {
            window._userDesignSegments.push([[startPoint.x, startPoint.y], [p.x, p.y]]);
        }
        drawing = false;
        startPoint = null;
        redraw();
    });
    canvas.addEventListener('mouseleave', () => {
        drawing = false;
        startPoint = null;
        redraw();
    });
    canvas.addEventListener('touchstart', (e) => {
        e.preventDefault();
        drawing = true;
        startPoint = pos(e);
    });
    canvas.addEventListener('touchmove', (e) => {
        e.preventDefault();
        if (!drawing || !startPoint) return;
        redraw();
        drawPreview(pos(e));
    });
    canvas.addEventListener('touchend', (e) => {
        if (!drawing || !startPoint) return;
        const touches = e.changedTouches;
        const r = canvas.getBoundingClientRect();
        const cx = touches[0].clientX - r.left;
        const cy = touches[0].clientY - r.top;
        const p = { x: (cx / canvas.width) * 10, y: (cy / canvas.height) * 10 };
        const dx = p.x - startPoint.x;
        const dy = p.y - startPoint.y;
        if (Math.sqrt(dx * dx + dy * dy) > 0.3) {
            window._userDesignSegments.push([[startPoint.x, startPoint.y], [p.x, p.y]]);
        }
        drawing = false;
        startPoint = null;
        redraw();
    });

    const clearBtn = document.getElementById('clear-design');
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            window._userDesignSegments = [];
            redraw();
        });
    }

    const undoBtn = document.getElementById('undo-design');
    if (undoBtn) {
        undoBtn.addEventListener('click', () => {
            window._userDesignSegments.pop();
            redraw();
        });
    }

    const randBtn = document.getElementById('random-design');
    if (randBtn) {
        randBtn.addEventListener('click', () => {
            if (!scene) return;
            const segs = scene.generateIceCracks({ seed: Math.floor(Math.random() * 10000), segments: 30 });
            window._userDesignSegments = segs.map(s => [[s.x1 + 5, s.z1 + 5], [s.x2 + 5, s.z2 + 5]]);
            redraw();
        });
    }

    window.addEventListener('resize', resizeCanvas);
    resizeCanvas();
}
