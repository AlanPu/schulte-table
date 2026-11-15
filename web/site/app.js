/* ==========================================================================
   舒尔特表 PWA — 游戏逻辑
   与 Kotlin/Compose 版本 (v1.1.0) 行为保持一致：
   · 4×4 / 5×5 / 6×6 三档
   · 数字 / 字母 / 汉字 三种内容
   · 彩色 / 单色 两种格子样式
   · 每组配置单独保存前 10 名成绩
   ========================================================================== */

(function () {
  'use strict';

  /* ------------------------------------------------------------- 常量 */

  const PALETTE = [
    '#E53935', '#FB8C00', '#FDD835', '#43A047',
    '#1E88E5', '#8E24AA', '#D81B60', '#5E35B1'
  ];

  const CHINESE_NUMBERS = [
    '零', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十',
    '十一', '十二', '十三', '十四', '十五', '十六', '十七', '十八', '十九', '二十',
    '二十一', '二十二', '二十三', '二十四', '二十五', '二十六', '二十七', '二十八', '二十九', '三十',
    '三十一', '三十二', '三十三', '三十四', '三十五', '三十六'
  ];

  const MODE_NAME = { Numbers: '数字', Letters: '字母', Chinese: '汉字' };

  const LS_SETTINGS = 'schulte:settings:v1';
  const LS_SCORES = 'schulte:scores:v1';
  const MAX_SCORES = 10;

  /* ------------------------------------------------------------- DOM */

  const $ = (id) => document.getElementById(id);

  const el = {
    board: $('board'),
    boardWrap: $('board-wrap'),
    overlay: $('overlay'),
    overlayTitle: $('overlay-title'),
    overlaySub: $('overlay-sub'),
    overlayBtn: $('overlay-btn'),
    timerCard: $('timer-card'),
    timerLabel: $('timer-label'),
    timerValue: $('timer-value'),
    timerBest: $('timer-best'),
    hintCurrent: $('hint-current'),
    hintTarget: $('hint-target'),
    controls: $('controls'),
    btnPause: $('btn-pause'),
    btnRestart: $('btn-restart'),
    footnote: $('footnote'),
    btnScores: $('btn-scores'),
    btnAbout: $('btn-about'),
    btnSettings: $('btn-settings')
  };

  /* ------------------------------------------------------------- 状态 */

  const state = {
    gridSize: 5,
    mode: 'Numbers',
    colors: true,
    numbers: [],
    current: 1,
    running: false,
    paused: false,
    over: false,
    elapsed: 0,
    acc: 0,
    startedAt: 0,
    raf: 0
  };

  const pad = () => state.gridSize * state.gridSize;

  /* ------------------------------------------------------------- 工具 */

  function shuffle(arr) {
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  }

  /** 与 Kotlin 版 colorForNumber 相同：按编号取色，取模 8 */
  function colorFor(number) {
    return PALETTE[number % PALETTE.length];
  }

  /** 亮度公式 (r*299 + g*587 + b*114)/1000 > 128 用深色字，否则白字 */
  function textColorFor(hex) {
    const r = parseInt(hex.slice(1, 3), 16);
    const g = parseInt(hex.slice(3, 5), 16);
    const b = parseInt(hex.slice(5, 7), 16);
    const brightness = (r * 299 + g * 587 + b * 114) / 1000;
    return brightness > 128 ? '#141414' : '#ffffff';
  }

  /**
   * 数字 / 字母 / 汉字 三种呈现。
   * 注意：字母模式在 6×6（36 格）下会超过 26 个字母，
   * 原 Kotlin 版用 fromCharCode 会溢出成 [ \ ] ^ _ ` 等符号，
   * 这里改为 A–Z 之后接 a–j。
   */
  function contentFor(number) {
    switch (state.mode) {
      case 'Letters':
        return number <= 26
          ? String.fromCharCode(65 + number - 1)
          : String.fromCharCode(97 + number - 27);
      case 'Chinese':
        // 数组第 0 项是「零」，所以取 [number] 才是「一…三十六」。
        // 原 Kotlin 版写的是 [number - 1]，导致 1 显示成「零」、36 显示成「三十五」，整体错位一格。
        return CHINESE_NUMBERS[number] || String(number);
      default:
        return String(number);
    }
  }

  function fmtTime(seconds) {
    return seconds.toFixed(2);
  }

  function fmtDate(ts) {
    const d = new Date(ts);
    const p2 = (v) => String(v).padStart(2, '0');
    return `${d.getFullYear()}/${p2(d.getMonth() + 1)}/${p2(d.getDate())} ${p2(d.getHours())}:${p2(d.getMinutes())}`;
  }

  /* ------------------------------------------------------------- 设置持久化 */

  function loadSettings() {
    try {
      const raw = JSON.parse(localStorage.getItem(LS_SETTINGS) || '{}');
      if ([4, 5, 6].includes(raw.gridSize)) state.gridSize = raw.gridSize;
      if (MODE_NAME[raw.mode]) state.mode = raw.mode;
      if (typeof raw.colors === 'boolean') state.colors = raw.colors;
    } catch (_) { /* 用默认值 */ }
  }

  function saveSettings() {
    localStorage.setItem(LS_SETTINGS, JSON.stringify({
      gridSize: state.gridSize, mode: state.mode, colors: state.colors
    }));
  }

  /* ------------------------------------------------------------- 成绩 */

  function scoreKey(gridSize, mode, colors) {
    return `${gridSize}_${mode}_${colors}`;
  }

  function readAllScores() {
    try {
      return JSON.parse(localStorage.getItem(LS_SCORES) || '{}') || {};
    } catch (_) {
      return {};
    }
  }

  function readScores() {
    const all = readAllScores();
    return all[scoreKey(state.gridSize, state.mode, state.colors)] || [];
  }

  function saveScore(record) {
    const all = readAllScores();
    const key = scoreKey(record.gridSize, record.gameMode, record.useColors);
    const list = (all[key] || []).concat(record)
      .sort((a, b) => a.elapsedTime - b.elapsedTime)
      .slice(0, MAX_SCORES);
    all[key] = list;
    localStorage.setItem(LS_SCORES, JSON.stringify(all));
  }

  function clearScoresOfCurrent() {
    const all = readAllScores();
    delete all[scoreKey(state.gridSize, state.mode, state.colors)];
    localStorage.setItem(LS_SCORES, JSON.stringify(all));
  }

  /* ------------------------------------------------------------- 布局 */

  /**
   * 棋盘尺寸与字号全部交给 CSS 计算：
   *   --side    = min(容器宽, 容器高)      ← 容器查询单位 100cqw / 100cqh
   *   --cellpx  = (--side - 间隔) / 格数
   *   .cell 字号 = --cellpx × 系数（按模式与字数分档，见 styles.css）
   *
   * 这里只同步「几乘几」和间隔。不要再在 JS 里测量像素 ——
   * 之前用 getBoundingClientRect 算宽度，会因为棋盘宽度反过来影响容器宽度，
   * 每次旋转/改变窗口都会滞后一轮，尺寸永远慢一拍。
   */
  function syncBoardVars() {
    const n = state.gridSize;
    el.board.style.setProperty('--n', n);
    el.board.style.setProperty('--gap', (n >= 6 ? 6 : 8) + 'px');
    el.board.style.gridTemplateColumns = `repeat(${n}, 1fr)`;
    el.board.style.gridTemplateRows = `repeat(${n}, 1fr)`;
    el.board.dataset.mode = state.mode;
  }

  /* ------------------------------------------------------------- 渲染 */

  function paintTimer() {
    el.timerValue.textContent = fmtTime(state.elapsed);
    el.timerCard.classList.toggle('idle', !state.running && !state.over);
    el.timerCard.classList.toggle('done', state.over);
    el.timerLabel.textContent = state.over ? '完成' : state.paused ? '已暂停' : state.running ? '计时中' : '准备';
  }

  function paintHints() {
    const last = pad();
    el.hintCurrent.textContent = state.over
      ? `全部完成 · ${state.gridSize}×${state.gridSize}`
      : `当前：${contentFor(Math.min(state.current, last))}`;
    el.hintTarget.textContent = state.over
      ? `${MODE_NAME[state.mode]} · ${state.colors ? '彩色' : '单色'}`
      : `目标：${contentFor(last)}`;
  }

  function paintBest() {
    const list = readScores();
    el.timerBest.textContent = list.length ? `最佳 ${fmtTime(list[0].elapsedTime)} 秒` : '';
  }

  function paintFootnote() {
    el.footnote.textContent =
      `点「开始游戏」，按 ${contentFor(1)} → ${contentFor(pad())} 的顺序尽快点完`;
  }

  function buildBoard() {
    const n = state.gridSize;
    syncBoardVars();
    el.board.textContent = '';

    const frag = document.createDocumentFragment();
    state.numbers.forEach((number) => {
      const cell = document.createElement('button');
      cell.type = 'button';
      cell.className = 'cell' + (state.colors ? '' : ' mono');
      cell.dataset.number = String(number);

      if (state.colors) {
        const bg = colorFor(number);
        cell.style.background = bg;
        cell.style.color = textColorFor(bg);
      }

      const text = contentFor(number);
      cell.textContent = text;
      // 字号由 CSS 按字数分档，这里只声明字数
      cell.dataset.len = String([...text].length);

      cell.addEventListener('click', () => onTap(number, cell));
      frag.appendChild(cell);
    });

    el.board.appendChild(frag);
  }

  function paintControls() {
    const active = state.running && !state.over;
    el.btnPause.hidden = !active;
    el.btnRestart.hidden = !active;
    el.btnPause.textContent = state.paused ? '继续' : '暂停';
    el.board.classList.toggle('paused', state.paused);
    el.board.classList.toggle('locked', state.paused || state.over);
  }

  /**
   * 遮罩只在「未开始」和「已完成」两个状态出现。
   * 暂停状态不盖遮罩 —— 改为让棋盘模糊 + 下方按钮区给出「继续 / 重新开始」，
   * 避免屏幕上同时出现两个「继续」按钮。
   */
  function paintOverlay() {
    if (!state.over && state.running) {
      el.overlay.hidden = true;
      return;
    }

    el.overlay.hidden = false;

    if (state.over) {
      el.overlayTitle.textContent = '完成！';
      const list = readScores();
      const best = list.length ? list[0].elapsedTime : null;
      const isNewBest = best !== null && state.elapsed <= best + 0.0001;
      el.overlaySub.textContent =
        `用时 ${fmtTime(state.elapsed)} 秒` + (isNewBest ? ' · 新纪录 🎉' : '');
      el.overlayBtn.textContent = '再来一局';
    } else {
      el.overlayTitle.textContent = '开始游戏';
      el.overlaySub.textContent =
        `${state.gridSize}×${state.gridSize} · ${MODE_NAME[state.mode]} · ${state.colors ? '彩色' : '单色'}`;
      el.overlayBtn.textContent = '开始游戏';
    }
  }

  function paintAll() {
    paintTimer();
    paintHints();
    paintBest();
    paintControls();
    paintOverlay();
  }

  /* ------------------------------------------------------------- 计时 */

  function tick() {
    if (!state.running || state.paused) return;
    state.elapsed = state.acc + (performance.now() - state.startedAt) / 1000;
    paintTimer();
    state.raf = requestAnimationFrame(tick);
  }

  function stopRaf() {
    if (state.raf) cancelAnimationFrame(state.raf);
    state.raf = 0;
  }

  /* ------------------------------------------------------------- 游戏流程 */

  function prepareBoard() {
    state.numbers = shuffle(Array.from({ length: pad() }, (_, i) => i + 1));
    state.current = 1;
    state.elapsed = 0;
    state.acc = 0;
    state.running = false;
    state.paused = false;
    state.over = false;
    buildBoard();
    paintFootnote();
    paintAll();
  }

  function startGame() {
    stopRaf();
    state.numbers = shuffle(Array.from({ length: pad() }, (_, i) => i + 1));
    state.current = 1;
    state.elapsed = 0;
    state.acc = 0;
    state.over = false;
    state.paused = false;
    state.running = true;
    state.startedAt = performance.now();
    buildBoard();
    paintAll();
    tick();
  }

  function pauseGame() {
    if (!state.running || state.over || state.paused) return;
    state.acc += (performance.now() - state.startedAt) / 1000;
    state.paused = true;
    stopRaf();
    paintAll();
  }

  function resumeGame() {
    if (!state.paused) return;
    state.paused = false;
    state.startedAt = performance.now();
    paintAll();
    tick();
  }

  function finishGame() {
    state.acc += (performance.now() - state.startedAt) / 1000;
    state.elapsed = state.acc;
    state.running = false;
    state.over = true;
    stopRaf();

    saveScore({
      elapsedTime: state.elapsed,
      timestamp: Date.now(),
      gridSize: state.gridSize,
      gameMode: state.mode,
      useColors: state.colors
    });

    paintAll();
  }

  function onTap(number, cell) {
    if (!state.running || state.paused || state.over) return;
    if (number !== state.current) return;

    cell.classList.add('hit');
    setTimeout(() => cell.classList.remove('hit'), 140);

    state.current += 1;
    if (state.current > pad()) {
      finishGame();
    } else {
      paintHints();
    }
  }

  /* ------------------------------------------------------------- 弹层控制 */

  function openScrim(id) {
    const scrim = $(id);
    if (!scrim) return;
    scrim.hidden = false;
    const focusable = scrim.querySelector('.btn.primary, .btn.text');
    if (focusable) focusable.focus({ preventScroll: true });
  }

  function closeScrim(id) {
    const scrim = $(id);
    if (scrim) scrim.hidden = true;
  }

  /* ------------------------------------------------------------- 设置弹层 */

  const draft = { gridSize: 5, mode: 'Numbers', colors: true };

  function syncSettingsUI() {
    document.querySelectorAll('#chips-size .chip').forEach((c) => {
      c.classList.toggle('selected', Number(c.dataset.size) === draft.gridSize);
    });
    document.querySelectorAll('#chips-mode .chip').forEach((c) => {
      c.classList.toggle('selected', c.dataset.mode === draft.mode);
    });
    document.querySelectorAll('#chips-style .chip').forEach((c) => {
      c.classList.toggle('selected', (c.dataset.colors === '1') === draft.colors);
    });
  }

  function openSettings() {
    draft.gridSize = state.gridSize;
    draft.mode = state.mode;
    draft.colors = state.colors;
    syncSettingsUI();
    openScrim('scrim-settings');
  }

  function applySettings() {
    const changed =
      draft.gridSize !== state.gridSize ||
      draft.mode !== state.mode ||
      draft.colors !== state.colors;

    state.gridSize = draft.gridSize;
    state.mode = draft.mode;
    state.colors = draft.colors;
    saveSettings();
    closeScrim('scrim-settings');

    if (changed) {
      stopRaf();
      prepareBoard();
    }
  }

  /* ------------------------------------------------------------- 成绩弹层 */

  let clearArmed = false;
  let clearTimer = 0;

  function renderScores() {
    const list = readScores();
    const rows = $('score-rows');
    const empty = $('score-empty');
    const clearBtn = $('scores-clear');

    $('scores-caption').textContent =
      `${state.gridSize}×${state.gridSize} · ${MODE_NAME[state.mode]} · ${state.colors ? '彩色' : '单色'}　（保留前 ${MAX_SCORES} 名）`;

    rows.textContent = '';
    empty.hidden = list.length > 0;
    clearBtn.hidden = list.length === 0;

    list.forEach((rec, i) => {
      const row = document.createElement('div');
      row.className = 'score-row' + (i === 0 ? ' top1' : '');
      const r = document.createElement('span');
      r.className = 'rank';
      r.textContent = String(i + 1);
      const t = document.createElement('span');
      t.className = 'time';
      t.textContent = fmtTime(rec.elapsedTime) + ' 秒';
      const d = document.createElement('span');
      d.className = 'date';
      d.textContent = fmtDate(rec.timestamp);
      row.append(r, t, d);
      rows.appendChild(row);
    });

    clearArmed = false;
    clearBtn.textContent = '清空全部';
  }

  function openScores() {
    renderScores();
    openScrim('scrim-scores');
  }

  function armOrClear() {
    const btn = $('scores-clear');
    if (!clearArmed) {
      clearArmed = true;
      btn.textContent = '再点一次确认';
      clearTimeout(clearTimer);
      clearTimer = setTimeout(() => {
        clearArmed = false;
        btn.textContent = '清空全部';
      }, 3000);
      return;
    }
    clearTimeout(clearTimer);
    clearScoresOfCurrent();
    renderScores();
    paintBest();
  }

  /* ------------------------------------------------------------- 事件绑定 */

  function bind() {
    el.overlayBtn.addEventListener('click', () => {
      if (state.paused) resumeGame();
      else startGame(); // 「开始游戏」与「再来一局」都走这里
    });

    // 点遮罩空白处也能开始 / 再来一局（按钮之外都算）
    el.overlay.addEventListener('click', (e) => {
      if (e.target.closest('#overlay-btn')) return;
      startGame();
    });

    el.btnPause.addEventListener('click', () => {
      if (state.paused) resumeGame();
      else pauseGame();
    });

    el.btnRestart.addEventListener('click', () => startGame());

    el.btnSettings.addEventListener('click', openSettings);
    el.btnScores.addEventListener('click', openScores);
    el.btnAbout.addEventListener('click', () => openScrim('scrim-about'));

    document.querySelectorAll('#chips-size .chip').forEach((c) => {
      c.addEventListener('click', () => {
        draft.gridSize = Number(c.dataset.size);
        syncSettingsUI();
      });
    });
    document.querySelectorAll('#chips-mode .chip').forEach((c) => {
      c.addEventListener('click', () => {
        draft.mode = c.dataset.mode;
        syncSettingsUI();
      });
    });
    document.querySelectorAll('#chips-style .chip').forEach((c) => {
      c.addEventListener('click', () => {
        draft.colors = c.dataset.colors === '1';
        syncSettingsUI();
      });
    });

    $('settings-cancel').addEventListener('click', () => closeScrim('scrim-settings'));
    $('settings-confirm').addEventListener('click', applySettings);
    $('scores-close').addEventListener('click', () => closeScrim('scrim-scores'));
    $('scores-clear').addEventListener('click', armOrClear);
    $('about-close').addEventListener('click', () => closeScrim('scrim-about'));

    // 点遮罩空白处关闭
    [['scrim-settings', 'settings'], ['scrim-scores', 'scores'], ['scrim-about', 'about']].forEach(([id]) => {
      const scrim = $(id);
      scrim.addEventListener('click', (e) => {
        if (e.target === scrim) scrim.hidden = true;
      });
    });

    // 键盘：Esc 关弹层，空格 暂停/继续
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        ['scrim-settings', 'scrim-scores', 'scrim-about'].forEach(closeScrim);
        return;
      }
      if (e.code === 'Space' && state.running && !state.over) {
        e.preventDefault();
        if (state.paused) resumeGame();
        else pauseGame();
      }
    });

    // 切到后台自动暂停，避免计时白跑
    document.addEventListener('visibilitychange', () => {
      if (document.hidden && state.running && !state.paused && !state.over) pauseGame();
    });

    // 长按不弹菜单
    el.board.addEventListener('contextmenu', (e) => e.preventDefault());

    // 尺寸全由 CSS 容器查询负责，旋转 / 分屏无需 JS 重新测量。
    // 唯一需要重算的是 iOS 上地址栏收起导致的 100dvh 变化，syncBoardVars 幂等，随手叫一次即可。
    window.addEventListener('orientationchange', () => setTimeout(syncBoardVars, 120));
  }

  /* ------------------------------------------------------------- 启动 */

  loadSettings();
  bind();
  prepareBoard();

  // 注册 Service Worker（离线可用）
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js').catch(() => { /* 忽略：无 SW 也能玩 */ });
    });
  }
})();
