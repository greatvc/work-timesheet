<?php
declare(strict_types=1);

/* ------------------------------------------------------------------
   Work - tracker βάρδιας 8ωρου + 30', με διάλειμμα 30'.
   Δεδομένα: ./data/shift.json  (δημιουργείται αυτόματα)
   ------------------------------------------------------------------ */

date_default_timezone_set('Europe/Athens');

const SHIFT_SECONDS = 8 * 3600 + 30 * 60;   // 8 ώρες 30 λεπτά
const BREAK_SECONDS = 30 * 60;              // 30 λεπτά διάλειμμα
const BREAK_CUTOFF  = 31 * 60;              // νεκρή ζώνη 31' στην αρχή και στο τέλος της βάρδιας

$DATA_DIR  = __DIR__ . '/data';
$DATA_FILE = $DATA_DIR . '/shift.json';

function emptyState(): array
{
    return ['date' => null, 'start' => null, 'break' => null];
}

function readState(string $file): array
{
    if (!is_file($file)) {
        return emptyState();
    }
    $json = json_decode((string) @file_get_contents($file), true);
    if (!is_array($json)) {
        return emptyState();
    }
    return [
        'date'  => isset($json['date'])  ? (string) $json['date'] : null,
        'start' => isset($json['start']) ? (int) $json['start'] : null,
        'break' => isset($json['break']) ? (int) $json['break'] : null,
    ];
}

function writeState(string $dir, string $file, array $state): void
{
    if (!is_dir($dir)) {
        @mkdir($dir, 0775, true);
    }
    @file_put_contents($file, json_encode($state, JSON_UNESCAPED_UNICODE), LOCK_EX);
}

function greekDay(int $w): string
{
    $days = ['Κυριακή', 'Δευτέρα', 'Τρίτη', 'Τετάρτη', 'Πέμπτη', 'Παρασκευή', 'Σάββατο'];
    return $days[$w];
}

/* Αυτόματο reset: ό,τι δεν είναι σημερινό, δεν μετράει. */
function currentPayload(string $dir, string $file): array
{
    $today = date('Y-m-d');
    $state = readState($file);

    if ($state['date'] !== null && $state['date'] !== $today) {
        writeState($dir, $file, emptyState());
        $state = emptyState();
    }

    $sameDay = ($state['date'] === $today);
    $start   = $sameDay ? $state['start'] : null;
    $break   = $sameDay ? $state['break'] : null;

    return [
        'now'      => time(),
        'day'      => greekDay((int) date('w')),
        'date'     => $today,
        'start'    => $start,
        'end'      => $start ? $start + SHIFT_SECONDS : null,
        'brk'      => $break,
        'brkEnd'   => $break ? $break + BREAK_SECONDS : null,
    ];
}

/* ---------------------------- mini API ---------------------------- */

$api = isset($_GET['api']) ? (string) $_GET['api'] : '';

if ($api !== '') {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');

    $today = date('Y-m-d');
    $state = readState($DATA_FILE);
    $now   = time();

    if ($api === 'start') {
        if ($state['date'] !== $today || empty($state['start'])) {
            writeState($DATA_DIR, $DATA_FILE, ['date' => $today, 'start' => $now, 'break' => null]);
        }
    } elseif ($api === 'reset') {
        writeState($DATA_DIR, $DATA_FILE, emptyState());
    } elseif ($api === 'break') {
        $ok = $state['date'] === $today
            && !empty($state['start'])
            && empty($state['break'])
            && $now >= $state['start'] + BREAK_CUTOFF
            && $now < $state['start'] + SHIFT_SECONDS - BREAK_CUTOFF;
        if ($ok) {
            $state['break'] = $now;
            writeState($DATA_DIR, $DATA_FILE, $state);
        }
    }

    echo json_encode(currentPayload($DATA_DIR, $DATA_FILE), JSON_UNESCAPED_UNICODE);
    exit;
}

$boot = currentPayload($DATA_DIR, $DATA_FILE);
?>
<!doctype html>
<!-- Work TimeSheet · v1.0.0 · Last update: 23/09/2026 18:18 (ώρα Ελλάδας) -->
<html lang="el">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#0b0d12">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
<meta name="apple-mobile-web-app-title" content="Work TimeSheet">
<link rel="icon" href="work.ico" sizes="any">
<link rel="shortcut icon" href="work.ico">
<link rel="apple-touch-icon" href="apple-touch-icon.png">
<title>Work TimeSheet</title>
<style>
  :root{
    --bg:#0b0d12;
    --card:#151922;
    --line:#242a36;
    --label:#8e97a6;
    --dim:#5d6575;
    --value:#f0b429;
    --ok:#4ade80;
    --soft:#7fae92;
    --brk:#58b0c8;
    box-sizing:border-box;
    padding-top:env(safe-area-inset-top,0px);
    padding-bottom:env(safe-area-inset-bottom,0px);
    padding-left:env(safe-area-inset-left,0px);
    padding-right:env(safe-area-inset-right,0px);
  }
  *,*::before,*::after{box-sizing:inherit}
  html,body{height:100%}
  body{
    margin:0;
    background:var(--bg);
    color:#e7eaf0;
    font:400 14px/1.35 -apple-system,BlinkMacSystemFont,"SF Pro Text","Segoe UI",Roboto,sans-serif;
    -webkit-font-smoothing:antialiased;
    -webkit-tap-highlight-color:transparent;
    display:flex;
    align-items:flex-end;
    justify-content:flex-start;
    padding:16px;
    touch-action:manipulation;
  }
  @media (max-width:560px){
    body{align-items:center;justify-content:center}
  }

  .card{
    position:relative;
    overflow:hidden;
    width:320px;
    background:var(--card);
    border:1px solid var(--line);
    border-radius:14px;
    padding:0 14px 12px;
    opacity:.78;
    transition:opacity .35s ease,border-color .5s ease,box-shadow .5s ease;
    user-select:none;
    -webkit-user-select:none;
  }
  .card:hover,.card.awake{opacity:1}

  /* --- banner + κεφαλίδα --- */
  .banner{
    margin:0 -14px;
    padding:14px 0 11px;
    border-bottom:1px solid var(--line);
  }
  .banner img{
    display:block;width:84%;max-width:250px;height:auto;margin:0 auto;
  }
  .head{
    display:flex;align-items:center;gap:12px;
    padding:10px 0 8px;margin-bottom:2px;
    border-bottom:1px solid var(--line);
  }
  .head .worker{display:block;height:72px;width:auto;flex:none}
  .day{
    flex:1;text-align:center;
    font-size:19px;font-weight:800;letter-spacing:.2px;color:#e7eaf0;
  }

  .row{
    display:flex;
    align-items:baseline;
    justify-content:space-between;
    gap:10px;
    padding:5px 0;
  }
  .row .k{color:var(--label);font-weight:600;letter-spacing:.1px}
  .row .v{
    color:var(--value);
    font-weight:700;
    font-variant-numeric:tabular-nums;
    font-feature-settings:"tnum" 1;
  }
  .clock .v{font-size:22px;letter-spacing:.5px}
  .rest .v{font-size:19px}
  .endr .v{color:var(--soft);font-weight:600}
  .colon{transition:opacity .12s linear}
  .colon.off{opacity:.18}

  /* --- μπλοκ διαλείμματος --- */
  .brkbox{
    margin:8px -4px 2px;
    padding:6px 8px 4px;
    border-radius:9px;
    background:rgba(88,176,200,.08);
    border:1px solid rgba(88,176,200,.22);
  }
  .brkbox[hidden]{display:none}
  .brkbox .k{color:#8fbecb}
  .brkbox .v{color:var(--brk)}
  .brkbox .count .v{font-size:18px}

  .sep{height:1px;background:var(--line);margin:10px -2px 8px}

  .state{color:var(--label);font-size:12.5px;min-height:17px;font-weight:600}
  .sub{color:var(--dim);font-size:11.5px;margin-top:3px;min-height:15px}
  .legend{
    display:flex;align-items:center;gap:6px;
    color:var(--dim);font-size:11.5px;margin-top:4px;
  }
  .legend[hidden]{display:none}
  .legend .dot{
    width:6px;height:6px;border-radius:50%;
    background:var(--brk);flex:none;
  }

  .btns{display:flex;gap:8px;margin-top:12px}
  button{
    flex:1;
    font:inherit;
    font-weight:600;
    font-size:13px;
    color:#dfe4ec;
    background:#1d2330;
    border:1px solid var(--line);
    border-radius:9px;
    padding:9px 10px;
    cursor:pointer;
    -webkit-appearance:none;
    transition:background .15s ease,opacity .15s ease;
  }
  button .ic{font-size:19px;line-height:1}
  button.go,button.brkbtn,#btnReset{font-size:19px;line-height:1;padding:8px 10px}
  button.go{color:#12161f;background:var(--value);border-color:var(--value)}

  /* διπλό tap για reset */
  #btnReset{position:relative;overflow:hidden}
  #btnReset.armed{background:#7a2030;border-color:#c04358}
  #btnReset .fill{
    position:absolute;left:0;bottom:0;height:3px;width:100%;
    background:#ff7183;transform:scaleX(0);transform-origin:left;
  }
  button.brkbtn{
    width:100%;margin-top:8px;
    display:flex;align-items:center;justify-content:center;
    padding:4px;
    color:#e6f6fb;
    background:#1b5064;
    border-color:#276b83;
  }
  button.brkbtn img{
    display:block;height:38px;width:auto;
    -webkit-user-drag:none;user-select:none;pointer-events:none;
  }
  button:active{transform:translateY(1px)}
  button:disabled{opacity:.32;cursor:default}
  button:focus-visible{outline:2px solid #6ea8ff;outline-offset:2px}

  /* --- τελείωσε η βάρδια --- */
  .ver{
    margin-top:11px;
    display:flex;align-items:center;justify-content:center;gap:9px;
    font-size:10.5px;letter-spacing:.3px;color:#454c5a;
  }
  .ver .tagico{font-size:11px;line-height:1}

  .done{border-color:rgba(74,222,128,.45)}
  .done .row .v,.done .state{color:var(--ok)}
  .done .state{
    font-size:16.5px;font-weight:800;letter-spacing:.4px;
    animation:textglow 2.4s ease-in-out infinite;
  }
  .done .state .party{
    font-size:19px;margin-left:10px;vertical-align:-1px;
    text-shadow:none;
  }
  @keyframes textglow{
    0%,100%{text-shadow:0 0 8px rgba(74,222,128,.35)}
    50%{text-shadow:0 0 16px rgba(74,222,128,.7),0 0 30px rgba(74,222,128,.28)}
  }
  .done.pop{animation:glow 1.6s ease-out 2}
  @keyframes glow{
    0%{box-shadow:0 0 0 0 rgba(74,222,128,0)}
    35%{box-shadow:0 0 22px 2px rgba(74,222,128,.28)}
    100%{box-shadow:0 0 0 0 rgba(74,222,128,0)}
  }

  canvas#confetti{
    position:absolute;inset:0;
    width:100%;height:100%;
    pointer-events:none;z-index:5;
  }

  /* --- ερώτηση διαλείμματος --- */
  .ask{
    position:absolute;inset:0;z-index:10;
    display:flex;align-items:center;justify-content:center;
    padding:14px;
    background:rgba(9,11,16,.86);
    -webkit-backdrop-filter:blur(3px);
    backdrop-filter:blur(3px);
    animation:fade .16s ease-out;
  }
  .ask[hidden]{display:none}
  .ask-in{width:100%;text-align:center;animation:rise .18s ease-out}
  .ask-in p{margin:0 0 12px;font-size:13.5px;font-weight:600;color:#e7eaf0}
  .ask-b{display:flex;gap:8px}
  .ask-b .yes{color:#06222b;background:var(--brk);border-color:var(--brk)}
  @keyframes fade{from{opacity:0}to{opacity:1}}
  @keyframes rise{from{opacity:0;transform:translateY(6px) scale(.97)}to{opacity:1;transform:none}}
</style>
</head>
<body>

<div class="card" id="card">
  <canvas id="confetti"></canvas>

  <div class="banner"><img src="flexo.png" alt="Flexopack"></div>

  <div class="head">
    <img class="worker" src="worker.png" alt="">
    <span class="day" id="day">&nbsp;</span>
  </div>

  <div class="row clock">
    <span class="k">Ώρα</span>
    <span class="v" id="clock">--<span class="colon" id="colon">:</span>--</span>
  </div>

  <div class="row rest">
    <span class="k">Υπόλοιπο</span>
    <span class="v" id="rest">--:--</span>
  </div>

  <div class="row endr">
    <span class="k">Σχόλασμα</span>
    <span class="v" id="endtime">--:--</span>
  </div>

  <div class="brkbox" id="brkbox" hidden>
    <div class="row count">
      <span class="k">Διάλειμμα</span>
      <span class="v" id="brkleft">30:00</span>
    </div>
    <div class="row">
      <span class="k">Λήγει</span>
      <span class="v" id="brkend">--:--</span>
    </div>
  </div>

  <div class="sep"></div>

  <div class="state" id="state">&nbsp;</div>
  <div class="sub" id="sub">&nbsp;</div>
  <div class="legend" id="legend" hidden><span class="dot"></span><span id="legendtx"></span></div>

  <div class="btns">
    <button class="go" id="btnStart" title="Έναρξη βάρδιας" aria-label="Έναρξη βάρδιας">💼</button>
    <button id="btnReset" title="Reset" aria-label="Reset"><span class="ic">♻️</span><i class="fill"></i></button>
  </div>
  <button class="brkbtn" id="btnBreak" title="Διάλειμμα" aria-label="Διάλειμμα"><img src="food.png" alt=""></button>

  <div class="ver"><span class="tagico">🏷️</span><span>v1.0.0</span></div>

  <div class="ask" id="ask" hidden>
    <div class="ask-in">
      <p>Να ξεκινήσω διάλειμμα;</p>
      <div class="ask-b">
        <button id="askNo">Όχι</button>
        <button class="yes" id="askYes">Ναι</button>
      </div>
    </div>
  </div>
</div>

<script>
(function () {
  var BREAK  = <?= BREAK_SECONDS ?>;
  var CUTOFF = <?= BREAK_CUTOFF ?>;
  var boot  = <?= json_encode($boot, JSON_UNESCAPED_UNICODE) ?>;

  var card     = document.getElementById('card');
  var elDay    = document.getElementById('day');
  var elClock  = document.getElementById('clock');
  var elColon  = document.getElementById('colon');
  var elRest   = document.getElementById('rest');
  var elEnd    = document.getElementById('endtime');
  var brkBox   = document.getElementById('brkbox');
  var brkLeft  = document.getElementById('brkleft');
  var brkEndEl = document.getElementById('brkend');
  var elState  = document.getElementById('state');
  var elSub    = document.getElementById('sub');
  var legend   = document.getElementById('legend');
  var legendTx = document.getElementById('legendtx');
  var btnGo    = document.getElementById('btnStart');
  var btnRs    = document.getElementById('btnReset');
  var btnBk    = document.getElementById('btnBreak');
  var ask      = document.getElementById('ask');

  var st = boot;
  var offset = boot.now * 1000 - Date.now();   // ώρα server - ώρα συσκευής
  var celebrated = false;

  function serverNow() { return (Date.now() + offset) / 1000; }
  function two(n) { return (n < 10 ? '0' : '') + n; }
  function hhmm(ts) {
    var d = new Date(ts * 1000);
    return two(d.getHours()) + ':' + two(d.getMinutes());
  }

  function apply(data) {
    st = data;
    offset = data.now * 1000 - Date.now();
    if (!st.start) { celebrated = false; card.classList.remove('pop'); }
    render();
  }

  function call(action) {
    return fetch('?api=' + action, { method: 'POST', cache: 'no-store' })
      .then(function (r) { return r.json(); })
      .then(apply)
      .catch(function () {});
  }

  function sync() {
    fetch('?api=state', { cache: 'no-store' })
      .then(function (r) { return r.json(); })
      .then(apply)
      .catch(function () {});
  }

  var lastDay = new Date().getDate();

  function render() {
    var now = serverNow();

    /* πέρασαν τα μεσάνυχτα; τράβα αμέσως καθαρή κατάσταση από τον server */
    var today = new Date(now * 1000).getDate();
    if (today !== lastDay) { lastDay = today; sync(); }

    elDay.textContent = st.day;

    var d = new Date(now * 1000);
    elClock.childNodes[0].nodeValue = two(d.getHours());
    elClock.childNodes[2].nodeValue = two(d.getMinutes());
    elColon.classList.toggle('off', d.getSeconds() % 2 === 1);

    var running  = !!st.start && now < st.end;
    var finished = !!st.start && now >= st.end;
    var onBreak  = !!st.brk && now < st.brkEnd;
    var hadBreak = !!st.brk && now >= st.brkEnd;

    /* --- διάλειμμα --- */
    if (onBreak) {
      var bl = Math.max(0, Math.round(st.brkEnd - now));
      brkLeft.textContent  = two(Math.floor(bl / 60)) + ':' + two(bl % 60);
      brkEndEl.textContent = hhmm(st.brkEnd);
      brkBox.hidden = false;
    } else {
      brkBox.hidden = true;
    }

    /* το legend μπαίνει μόνο αφού τελειώσει το διάλειμμα */
    if (hadBreak) {
      legend.hidden = false;
      legendTx.textContent = 'Διάλειμμα ' + hhmm(st.brk) + ' - ' + hhmm(st.brkEnd);
    } else {
      legend.hidden = true;
    }

    btnBk.disabled = !running || !!st.brk
      || (now - st.start) < CUTOFF      // όχι μέσα στο πρώτο 31λεπτο
      || (st.end - now) < CUTOFF;       // ούτε στο τελευταίο

    /* --- βάρδια --- */
    if (!st.start) {
      card.classList.remove('done', 'pop');
      elRest.textContent = '8:30';
      elEnd.textContent  = '--:--';
      elState.innerHTML = '&nbsp;';
      elSub.innerHTML   = '&nbsp;';
      btnGo.disabled = false;
      btnRs.disabled = true;
      if (rsUntil) disarm();
      return;
    }

    elEnd.textContent = hhmm(st.end);

    if (running) {
      card.classList.remove('done', 'pop');
      var mins = Math.ceil((st.end - now) / 60);
      elRest.textContent  = Math.floor(mins / 60) + ':' + two(mins % 60);
      elState.textContent = 'Ξεκίνησες ' + hhmm(st.start);
      elSub.innerHTML     = '&nbsp;';
      btnGo.disabled = true;
      btnRs.disabled = false;
    } else if (finished) {
      card.classList.add('done');
      elRest.textContent  = '0:00';
      if (elState.firstElementChild === null) {
        elState.innerHTML = 'Σχόλασες<span class="party">🎉</span>';
      }
      elSub.textContent   = hhmm(st.start) + ' - ' + hhmm(st.end);
      btnGo.disabled = true;
      btnRs.disabled = true;
      btnBk.disabled = true;
      if (rsUntil) disarm();

      if (!celebrated) {
        celebrated = true;
        card.classList.add('awake');
        void card.offsetWidth;
        card.classList.add('pop');
        confetti();
      }
    }
  }

  /* ----------------------------- κουμπιά ----------------------------- */

  btnGo.onclick = function () { if (!btnGo.disabled) { disarm(); call('start'); } };

  /* --- reset με διπλό πάτημα, παράθυρο 10 δευτερολέπτων --- */
  var rsFill = btnRs.querySelector('.fill');
  var rsUntil = 0, rsTick = null;

  function disarm() {
    rsUntil = 0;
    clearTimeout(rsTick);
    rsTick = null;
    btnRs.classList.remove('armed');
    rsFill.style.transition = 'none';
    rsFill.style.transform = 'scaleX(0)';
  }

  function arm() {
    rsUntil = Date.now() + 10000;
    btnRs.classList.add('armed');
    card.classList.add('awake');

    rsFill.style.transition = 'none';
    rsFill.style.transform = 'scaleX(1)';
    void rsFill.offsetWidth;
    rsFill.style.transition = 'transform 10s linear';
    rsFill.style.transform = 'scaleX(0)';

    clearTimeout(rsTick);
    rsTick = setTimeout(disarm, 10000);
  }

  btnRs.onclick = function () {
    if (btnRs.disabled) return;
    if (rsUntil && Date.now() < rsUntil) {
      disarm();
      call('reset');
    } else {
      arm();
    }
  };

  function closeAsk() { ask.hidden = true; }
  btnBk.onclick = function () {
    if (btnBk.disabled) return;
    card.classList.add('awake');
    ask.hidden = false;
  };
  document.getElementById('askNo').onclick  = closeAsk;
  document.getElementById('askYes').onclick = function () { closeAsk(); disarm(); call('break'); };
  ask.addEventListener('click', function (e) { if (e.target === ask) closeAsk(); });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && !ask.hidden) closeAsk();
  });

  /* το card ξυπνάει στο άγγιγμα, αλλιώς μένει χαμηλά */
  ['pointerdown', 'touchstart'].forEach(function (ev) {
    card.addEventListener(ev, function () {
      card.classList.add('awake');
      clearTimeout(card._t);
      card._t = setTimeout(function () { card.classList.remove('awake'); }, 12000);
    }, { passive: true });
  });

  setInterval(render, 500);
  setInterval(sync, 60000);
  document.addEventListener('visibilitychange', function () { if (!document.hidden) sync(); });
  render();

  /* ------------------ confetti, μόνο μέσα στο card ------------------ */
  function confetti() {
    var cv = document.getElementById('confetti');
    var w = card.clientWidth, h = card.clientHeight;
    var dpr = Math.min(window.devicePixelRatio || 1, 2);
    cv.width = w * dpr; cv.height = h * dpr;
    var ctx = cv.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    var colors = ['#4ade80', '#f0b429', '#58b0c8', '#f472b6', '#e7eaf0'];
    var bits = [];
    for (var i = 0; i < 70; i++) {
      bits.push({
        x: Math.random() * w,
        y: -10 - Math.random() * h * 0.9,
        vx: (Math.random() - 0.5) * 0.7,
        vy: 0.9 + Math.random() * 1.7,
        s: 3 + Math.random() * 4,
        r: Math.random() * Math.PI,
        vr: (Math.random() - 0.5) * 0.22,
        sw: 0.4 + Math.random() * 0.6,
        c: colors[(Math.random() * colors.length) | 0]
      });
    }

    var t0 = performance.now();
    (function frame(t) {
      var age = t - t0;
      ctx.clearRect(0, 0, w, h);
      ctx.globalAlpha = age > 2600 ? Math.max(0, 1 - (age - 2600) / 900) : 1;

      for (var i = 0; i < bits.length; i++) {
        var b = bits[i];
        b.x += b.vx + Math.sin((age / 400) + b.r) * 0.25;
        b.y += b.vy;
        b.r += b.vr;
        b.vy += 0.013;
        ctx.save();
        ctx.translate(b.x, b.y);
        ctx.rotate(b.r);
        ctx.scale(1, Math.abs(Math.cos(age / 260 * b.sw)) * 0.8 + 0.2);
        ctx.fillStyle = b.c;
        ctx.fillRect(-b.s / 2, -b.s / 2, b.s, b.s * 0.62);
        ctx.restore();
      }

      if (age < 3600) {
        requestAnimationFrame(frame);
      } else {
        ctx.clearRect(0, 0, w, h);
      }
    })(t0);
  }
})();
</script>
</body>
</html>
