import { simulatePedestrianImpact, fetchPedestrianSimHistory } from './api.js';

export function initPedestrianSimulator(pavements, scene) {
  const panel = document.getElementById('propagation-panel');
  if (!panel) return;

  const opts = pavements.map(p =>
    `<option value="${p.id}">${p.name}</option>`
  ).join('');

  panel.innerHTML = `
    <h3>踩踏影响模拟</h3>
    <label class="field-label">选择铺地</label>
    <select id="prop-pavement">${opts}</select>
    <label class="field-label">初始裂缝宽度 (mm)</label>
    <input id="prop-crack-width" type="number" value="2.0" step="0.5" min="0">
    <label class="field-label">踩踏频率 (步/分)</label>
    <input id="prop-step-freq" type="number" value="30" step="5" min="0">
    <label class="field-label">总踩踏次数</label>
    <input id="prop-total-steps" type="number" value="100000" step="10000" min="0">
    <label class="field-label">模拟时长 (小时)</label>
    <input id="prop-sim-hours" type="number" value="1000" step="100" min="0">
    <button id="btn-run-propagation" class="btn-primary">运行模拟</button>
    <div id="propagation-result"></div>
  `;

  document.getElementById('btn-run-propagation').addEventListener('click', async () => {
    const params = {
      pavementId: document.getElementById('prop-pavement').value,
      initialCrackWidthMm: parseFloat(document.getElementById('prop-crack-width').value),
      stepFrequency: parseFloat(document.getElementById('prop-step-freq').value),
      totalSteps: parseInt(document.getElementById('prop-total-steps').value),
      simulationHours: parseFloat(document.getElementById('prop-sim-hours').value)
    };
    try {
      const result = await simulatePedestrianImpact(params);
      renderPropagationResult(result);
      if (scene && result.segmentPropagation) {
        try {
          const data = JSON.parse(result.segmentPropagation);
          scene.applyPropagation(data);
        } catch (_) {}
      }
    } catch (e) {
      document.getElementById('propagation-result').innerHTML =
        `<div class="error-msg">模拟失败: ${e.message}</div>`;
    }
  });
}

function renderPropagationResult(result) {
  const el = document.getElementById('propagation-result');
  if (!el) return;

  el.innerHTML = `
    <div class="result-card">
      <div>初始宽度: ${(result.initialCrackWidthMm || 0).toFixed(2)} mm</div>
      <div>最终宽度: ${(result.finalCrackWidthMm || 0).toFixed(2)} mm</div>
      <div>损伤指数: ${(result.damageIndex || 0).toFixed(4)}</div>
      <div class="damage-bar">
        <div class="damage-fill" style="width:${Math.min(100, (result.damageIndex || 0) * 100)}%;
          background:${(result.damageIndex || 0) < 0.3 ? '#5ba88c' : (result.damageIndex || 0) < 0.7 ? '#e8a838' : '#d45050'}">
        </div>
      </div>
    </div>
  `;
}
