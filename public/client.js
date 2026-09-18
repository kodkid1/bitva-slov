const socket = io();

let myId = null;
let currentRoom = null;
let game = null;
let timerHandle = null;

const $ = (id) => document.getElementById(id);

function show(screen) {
  ['screen-lobby', 'screen-room', 'screen-game'].forEach((s) => {
    $(s).classList.toggle('hidden', s !== screen);
  });
}

let toastHandle = null;
function toast(message) {
  const el = $('toast');
  el.textContent = message;
  el.classList.remove('hidden');
  clearTimeout(toastHandle);
  toastHandle = setTimeout(() => el.classList.add('hidden'), 2600);
}

function getPlayerName() {
  return $('playerName').value.trim();
}

function requireName() {
  const name = getPlayerName();
  if (!name) {
    toast('Сначала введи имя');
    return null;
  }
  localStorage.setItem('bs_name', name);
  return name;
}

function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  }[c]));
}

function renderRoomList(list) {
  const ul = $('roomList');
  if (!list || list.length === 0) {
    ul.innerHTML = '<li class="muted">Пока нет открытых комнат</li>';
    return;
  }
  ul.innerHTML = list
    .map(
      (r) => `
      <li>
        <div>
          <div>${escapeHtml(r.name)}</div>
          <div class="meta">${r.players}/${r.maxPlayers} игроков · ${r.timer} сек</div>
        </div>
        <button class="primary small" data-join="${r.id}">Войти</button>
      </li>`
    )
    .join('');
  ul.querySelectorAll('[data-join]').forEach((btn) => {
    btn.addEventListener('click', () => joinRoom({ roomId: btn.dataset.join }));
  });
}

function renderRoom(room) {
  currentRoom = room;
  $('roomTitle').textContent = room.name;
  $('roomMeta').textContent =
    `${room.players.length}/${room.maxPlayers} игроков · ход ${room.timer} сек · ` +
    (room.isPrivate ? 'приватная' : 'публичная');

  const codeBox = $('roomCode');
  if (room.code) {
    codeBox.textContent = 'Код: ' + room.code;
    codeBox.classList.remove('hidden');
  } else {
    codeBox.classList.add('hidden');
  }

  $('roomPlayers').innerHTML = room.players
    .map(
      (p) => `
      <li class="${p.id === myId ? 'me' : ''}">
        <span>${escapeHtml(p.name)}</span>
        ${p.id === room.hostId ? '<span class="crown">создатель</span>' : ''}
      </li>`
    )
    .join('');

  const isHost = room.hostId === myId;
  const startBtn = $('startBtn');
  startBtn.classList.toggle('hidden', !isHost);
  startBtn.disabled = room.players.length < 2;

  if (isHost) {
    $('roomHint').textContent =
      room.players.length < 2 ? 'Ждём ещё хотя бы одного игрока' : 'Все готовы? Запускай!';
  } else {
    $('roomHint').textContent = 'Ждём, пока создатель начнёт игру';
  }
}

function renderGame(state) {
  game = state;
  $('gameRoomName').textContent = currentRoom ? currentRoom.name : '';
  $('requiredLetter').textContent = (state.requiredLetter || '?').toUpperCase();

  if (state.lastWord) {
    $('lastWordLine').innerHTML = 'Предыдущее слово: <b>' + escapeHtml(state.lastWord) + '</b>';
  } else {
    $('lastWordLine').textContent = 'Ты начинаешь цепочку';
  }

  const myTurn = state.turnPlayerId === myId && state.state === 'playing';
  const input = $('wordInput');
  const sendBtn = $('sendWordBtn');
  input.disabled = !myTurn;
  sendBtn.disabled = !myTurn;

  const hint = $('turnHint');
  if (myTurn) {
    hint.textContent = 'Твой ход!';
    hint.className = 'turn-hint mine';
    if (document.activeElement !== input) input.focus();
  } else {
    const turnPlayer = currentRoom && currentRoom.players.find((p) => p.id === state.turnPlayerId);
    hint.textContent = turnPlayer ? 'Ход: ' + turnPlayer.name : 'Ожидание...';
    hint.className = 'turn-hint other';
    input.value = '';
  }

  const words = state.usedWords || [];
  $('usedCount').textContent = '(' + words.length + ')';
  $('usedWords').innerHTML = words
    .slice()
    .reverse()
    .map((w) => '<span class="chip">' + escapeHtml(w) + '</span>')
    .join('');

  renderGamePlayers();
  const left = typeof state.endIn === 'number' ? state.endIn : state.timer * 1000;
  startTimer(Date.now() + left, state.timer);
}

function renderGamePlayers() {
  if (!currentRoom) return;
  $('gamePlayers').innerHTML = currentRoom.players
    .map(
      (p) => `
      <li class="${p.alive ? '' : 'dead'}${p.id === myId ? ' me' : ''}">
        <span>${escapeHtml(p.name)}</span>
        <span>${p.alive ? 'в игре' : 'выбыл'}</span>
      </li>`
    )
    .join('');
}

function startTimer(deadline, seconds) {
  clearInterval(timerHandle);
  const bar = $('timerBar');
  const text = $('timerText');
  const total = seconds * 1000;

  function tick() {
    const left = Math.max(0, deadline - Date.now());
    const pct = Math.max(0, Math.min(100, (left / total) * 100));
    bar.style.width = pct + '%';
    bar.classList.toggle('low', left < 4000);
    text.textContent = (left / 1000).toFixed(1);
  }
  tick();
  timerHandle = setInterval(tick, 100);
}

function showWinner(winner) {
  clearInterval(timerHandle);
  const overlay = $('winnerOverlay');
  if (!winner) {
    $('winnerName').textContent = 'Ничья';
    $('winnerText').textContent = 'Все выбыли';
  } else if (winner.id === myId) {
    $('winnerName').textContent = 'Ты победил!';
    $('winnerText').textContent = winner.name;
  } else {
    $('winnerName').textContent = winner.name;
    $('winnerText').textContent = 'Последний в игре';
  }
  overlay.classList.remove('hidden');
}

function joinRoom(payload) {
  const name = requireName();
  if (!name) return;
  socket.emit('joinRoom', { ...payload, playerName: name }, (res) => {
    if (!res || !res.ok) toast((res && res.error) || 'Не удалось войти');
  });
}

$('toggleCreate').addEventListener('click', () => {
  const form = $('createForm');
  form.classList.toggle('hidden');
  $('toggleCreate').textContent = form.classList.contains('hidden') ? 'Развернуть' : 'Свернуть';
});

$('createBtn').addEventListener('click', () => {
  const name = requireName();
  if (!name) return;
  socket.emit(
    'createRoom',
    {
      playerName: name,
      name: $('roomName').value.trim() || 'Комната друзей',
      isPrivate: $('roomPrivate').checked,
      timer: parseInt($('roomTimer').value, 10),
      maxPlayers: parseInt($('roomMax').value, 10),
    },
    (res) => {
      if (!res || !res.ok) toast('Не удалось создать комнату');
    }
  );
});

$('refreshBtn').addEventListener('click', () => {
  socket.emit('roomListRequest');
  toast('Список обновлён');
});

$('joinCodeBtn').addEventListener('click', () => {
  const code = $('codeInput').value.trim();
  if (!code) return toast('Введи код');
  joinRoom({ code });
});

$('leaveRoomBtn').addEventListener('click', () => {
  socket.emit('leaveRoom');
});

$('startBtn').addEventListener('click', () => {
  socket.emit('startGame');
});

function submitWord() {
  if (!game || game.turnPlayerId !== myId) return;
  const word = $('wordInput').value.trim();
  if (!word) return;
  $('wordError').textContent = '';
  socket.emit('submitWord', { word });
}

$('sendWordBtn').addEventListener('click', submitWord);
$('wordInput').addEventListener('keydown', (e) => {
  if (e.key === 'Enter') submitWord();
});

$('playAgainBtn').addEventListener('click', () => {
  $('winnerOverlay').classList.add('hidden');
  socket.emit('startGame');
});

$('backToRoomBtn').addEventListener('click', () => {
  $('winnerOverlay').classList.add('hidden');
  show('screen-room');
});

socket.on('connected', (data) => {
  myId = data.id;
});

socket.on('roomList', (list) => {
  renderRoomList(list);
});

socket.on('roomUpdate', (room) => {
  renderRoom(room);
  show(room.state === 'playing' ? 'screen-game' : 'screen-room');
});

socket.on('gameStarted', () => {
  $('wordError').textContent = '';
  $('wordInput').value = '';
  $('winnerOverlay').classList.add('hidden');
  show('screen-game');
  toast('Игра началась!');
});

socket.on('gameUpdate', (state) => {
  if (!$('winnerOverlay').classList.contains('hidden')) return;
  renderGame(state);
});

socket.on('wordAccepted', () => {
  $('wordError').textContent = '';
  $('wordInput').value = '';
});

socket.on('wordError', (data) => {
  $('wordError').textContent = data.message;
});

socket.on('playerEliminated', (data) => {
  toast(data.name + ' выбыл по таймеру');
});

socket.on('gameOver', (data) => {
  showWinner(data.winner);
});

socket.on('errorMessage', (data) => {
  toast(data.message);
});

setInterval(() => {
  if (!$('screen-lobby').classList.contains('hidden')) socket.emit('roomListRequest');
}, 4000);

window.addEventListener('load', () => {
  const saved = localStorage.getItem('bs_name');
  if (saved) $('playerName').value = saved;
  show('screen-lobby');
});
