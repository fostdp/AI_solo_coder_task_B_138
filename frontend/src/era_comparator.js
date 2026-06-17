import { eraCompare, fetchEraComparisonHistory } from './api.js';

export function initEraComparator(pavements, scene) {
  const panel = document.getElementById('era-panel');
  if (!panel) return;

  const checkboxes = pavements.map(p => `
    <label class="check-item">
      <input type="checkbox" class="era-check" value="${p.id}" data-era="${p.era || 'ANCIENT'}">
      ${p.name} (${p.era || 'ANCIENT'})
    </label>
  `).join('');

  panel.innerHTML = `
    <h3>跨时代对比</h3>
    <div class="check-list">${checkboxes}</div>
    <div class="btn-row">
      <button id="btn-era-compare" class="btn-primary" style="flex:1">跨时代对比</button>
    </div>
    <div id="era-comparison-result"></div>
  `;

  document.getElementById('btn-era-compare').addEventListener('click', async () => {
    const ids = [...document.querySelectorAll('.era-check:checked')].map(c => c.value);
    if (ids.length < 2) {
      alert('请选择至少2个铺地（需包含古代和现代）');
      return;
    }
    try {
      const result = await eraCompare(ids);
      renderEraResult(result);
    } catch (e) {
      document.getElementById('era-comparison-result').innerHTML =
        `<div class="error-msg">对比失败: ${e.message}</div>`;
    }
  });
}

function renderEraResult(result) {
  const el = document.getElementById('era-comparison-result');
  if (!el) return;

  const renderGroup = (label, data) => {
    if (!data) return '';
    return `
      <div class="comparison-card">
        <h5>${label}</h5>
        <div>平均退水时间: ${((data.avgRecessionTimeSec || 0)).toFixed(0)}s</div>
        <div>平均视觉复杂度: ${((data.avgComplexity || 0)).toFixed(3)}</div>
        <div>平均渗透速率: ${((data.avgInfiltrationRate || 0)).toFixed(6)}</div>
      </div>
    `;
  };

  el.innerHTML = `
    <div class="comparison-section">
      <div class="comparison-grid">
        ${renderGroup('古代铺地', result.ancientAvg)}
        ${renderGroup('现代透水砖', result.modernAvg)}
      </div>
    </div>
    <div class="comparison-summary">${result.summary || ''}</div>
  `;
}
