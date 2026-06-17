import { submitVrDesign, fetchVrDesignsBySession } from './api.js';

export function initVrPavingDesigner(scene) {
  const panel = document.getElementById('design-panel');
  if (!panel) return;

  initUserDesignCanvas(scene);

  const submitBtn = document.getElementById('submit-design');
  if (submitBtn) {
    submitBtn.addEventListener('click', async () => {
      const segments = window._userDesignSegments || [];
      if (segments.length === 0) {
        alert('请先绘制裂缝图案');
        return;
      }
      const params = {
        userSessionId: window._designSessionId || ('session_' + Date.now()),
        designName: document.getElementById('design-name')?.value || '未命名设计',
        crackPattern: JSON.stringify(segments),
        areaLength: parseFloat(document.getElementById('design-area-length')?.value || 10),
        areaWidth: parseFloat(document.getElementById('design-area-width')?.value || 10),
        slopeAngle: parseFloat(document.getElementById('design-slope')?.value || 2),
        basePermeability: parseFloat(document.getElementById('design-permeability')?.value || 0.001),
        runAesthetic: true,
        runDrainage: true,
        rainfallMm: parseFloat(document.getElementById('design-rainfall')?.value || 50),
        initialWaterDepthMm: parseFloat(document.getElementById('design-water-depth')?.value || 10),
        crackWidthMm: parseFloat(document.getElementById('design-crack-width')?.value || 3),
        stepFrequency: parseFloat(document.getElementById('design-step-freq')?.value || 30)
      };
      try {
        const result = await submitVrDesign(params);
        renderDesignResult(result);
        if (scene && segments.length > 0) {
          const customSegs = segments.map(s => ({
            x1: s[0][0] - 5, z1: s[0][1] - 5,
            x2: s[1][0] - 5, z2: s[1][1] - 5
          }));
          scene.loadCrackStyle('CUSTOM', null, customSegs);
        }
      } catch (e) {
        document.getElementById('design-result-panel').innerHTML =
          `<div class="error-msg">提交失败: ${e.message}</div>`;
      }
    });
  }
}

function initUserDesignCanvas(scene) {
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

  canvas.addEventListener('mousedown', (e) => { drawing = true; startPoint = pos(e); });
  canvas.addEventListener('mousemove', (e) => {
    if (!drawing || !startPoint) return;
    redraw();
    drawPreview(pos(e));
  });
  canvas.addEventListener('mouseup', (e) => {
    if (!drawing || !startPoint) return;
    const p = pos(e);
    const dx = p.x - startPoint.x, dy = p.y - startPoint.y;
    if (Math.sqrt(dx * dx + dy * dy) > 0.3) {
      window._userDesignSegments.push([[startPoint.x, startPoint.y], [p.x, p.y]]);
    }
    drawing = false; startPoint = null; redraw();
  });
  canvas.addEventListener('mouseleave', () => { drawing = false; startPoint = null; redraw(); });
  canvas.addEventListener('touchstart', (e) => { e.preventDefault(); drawing = true; startPoint = pos(e); });
  canvas.addEventListener('touchmove', (e) => {
    e.preventDefault();
    if (!drawing || !startPoint) return;
    redraw(); drawPreview(pos(e));
  });
  canvas.addEventListener('touchend', (e) => {
    if (!drawing || !startPoint) return;
    const touches = e.changedTouches;
    const r = canvas.getBoundingClientRect();
    const cx = touches[0].clientX - r.left, cy = touches[0].clientY - r.top;
    const p = { x: (cx / canvas.width) * 10, y: (cy / canvas.height) * 10 };
    const dx = p.x - startPoint.x, dy = p.y - startPoint.y;
    if (Math.sqrt(dx * dx + dy * dy) > 0.3) {
      window._userDesignSegments.push([[startPoint.x, startPoint.y], [p.x, p.y]]);
    }
    drawing = false; startPoint = null; redraw();
  });

  const clearBtn = document.getElementById('clear-design');
  if (clearBtn) clearBtn.addEventListener('click', () => { window._userDesignSegments = []; redraw(); });
  const undoBtn = document.getElementById('undo-design');
  if (undoBtn) undoBtn.addEventListener('click', () => { window._userDesignSegments.pop(); redraw(); });
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

function renderDesignResult(result) {
  const el = document.getElementById('design-result-panel');
  if (!el) return;

  let aesHtml = '';
  if (result.aestheticResult) {
    const a = result.aestheticResult;
    aesHtml = `
      <div class="result-card">
        <h5>美学分析</h5>
        <div>分形维数: ${(a.fractalDimension || 0).toFixed(3)}</div>
        <div>信息熵: ${(a.infoEntropy || 0).toFixed(3)}</div>
        <div>视觉复杂度: ${(a.visualComplexity || 0).toFixed(3)}</div>
        <div>图案对称性: ${(a.patternSymmetry || 0).toFixed(3)}</div>
        <div>裂缝密度: ${(a.crackDensity || 0).toFixed(4)}</div>
      </div>
    `;
  }

  let drainHtml = '';
  if (result.drainageResult) {
    const d = result.drainageResult;
    drainHtml = `
      <div class="result-card">
        <h5>排水仿真</h5>
        <div>退水时间: ${(d.recessionTimeSec || 0).toFixed(0)}s</div>
        <div>峰值水深: ${(d.peakWaterDepth || 0).toFixed(4)}m</div>
        <div>渗透速率: ${(d.infiltrationRate || 0).toFixed(6)}</div>
        <div>径流速率: ${(d.surfaceRunoffRate || 0).toFixed(6)}</div>
      </div>
    `;
  }

  el.innerHTML = aesHtml + drainHtml;
}
