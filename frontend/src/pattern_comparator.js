import { patternCompare, fetchPatternComparisonHistory } from './api.js';

export function initPatternComparator(pavements, scene) {
  const panel = document.getElementById('comparison-panel');
  if (!panel) return;

  const checkboxes = pavements.map(p => `
    <label class="check-item">
      <input type="checkbox" class="compare-check" value="${p.id}" data-style="${p.pavementStyle || 'ICE_CRACK'}">
      ${p.name} (${p.pavementStyle || 'ICE_CRACK'})
    </label>
  `).join('');

  panel.innerHTML = `
    <h3>样式对比分析</h3>
    <div class="check-list">${checkboxes}</div>
    <div class="btn-row">
      <button id="btn-style-compare" class="btn-primary" style="flex:1">样式对比</button>
    </div>
    <div id="style-comparison-result"></div>
  `;

  document.getElementById('btn-style-compare').addEventListener('click', async () => {
    const ids = [...document.querySelectorAll('.compare-check:checked')].map(c => c.value);
    if (ids.length < 2) {
      alert('请选择至少2个铺地');
      return;
    }
    try {
      const result = await patternCompare(ids);
      renderComparisonResult(result, 'style-comparison-result');
    } catch (e) {
      document.getElementById('style-comparison-result').innerHTML =
        `<div class="error-msg">对比失败: ${e.message}</div>`;
    }
  });
}

function renderComparisonResult(result, targetId) {
  const el = document.getElementById(targetId);
  if (!el) return;

  const aesCards = (result.aestheticResults || []).map(r => `
    <div class="comparison-card">
      <h5>${r.name || r.pavementStyle || ''}</h5>
      <div>分形维数: ${(r.fractalDimension || 0).toFixed(3)}</div>
      <div>信息熵: ${(r.infoEntropy || 0).toFixed(3)}</div>
      <div>视觉复杂度: ${(r.visualComplexity || 0).toFixed(3)}</div>
      <div>图案对称性: ${(r.patternSymmetry || 0).toFixed(3)}</div>
    </div>
  `).join('');

  const drainCards = (result.drainageResults || []).map(r => `
    <div class="comparison-card">
      <h5>${r.name || r.pavementStyle || ''}</h5>
      <div>退水时间: ${(r.recessionTimeSec || 0).toFixed(0)}s</div>
      <div>峰值水深: ${(r.peakWaterDepth || 0).toFixed(4)}m</div>
      <div>排水速率: ${(r.drainageRate || 0).toFixed(6)}</div>
      <div>渗透速率: ${(r.infiltrationRate || 0).toFixed(6)}</div>
    </div>
  `).join('');

  el.innerHTML = `
    <div class="comparison-section">
      <h4>美学指标</h4>
      <div class="comparison-grid">${aesCards}</div>
    </div>
    <div class="comparison-section">
      <h4>排水指标</h4>
      <div class="comparison-grid">${drainCards}</div>
    </div>
    <div class="comparison-summary">${result.summary || ''}</div>
  `;
}
