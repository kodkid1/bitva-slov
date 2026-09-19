const path = require('path');
const fs = require('fs');
const http = require('http');
const express = require('express');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  connectionStateRecovery: {
    maxDisconnectionDuration: 2 * 60 * 1000,
    skipMiddlewares: true,
  },
});

app.use(express.static(path.join(__dirname, 'public')));

const PORT = process.env.PORT || 3000;

const rooms = new Map();
const players = new Map();
const stats = new Map();

const pushTokens = new Map();
const activeSockets = new Set();

let messaging = null;
try {
  const { initializeApp, cert } = require('firebase-admin/app');
  const { getMessaging } = require('firebase-admin/messaging');
  let serviceAccount = null;
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  } else {
    const localKey = path.join(__dirname, 'firebase-admin.json');
    if (fs.existsSync(localKey)) {
      serviceAccount = JSON.parse(fs.readFileSync(localKey, 'utf8'));
    }
  }
  if (serviceAccount) {
    initializeApp({ credential: cert(serviceAccount) });
    messaging = getMessaging();
    console.log('Firebase подключён, пуши включены');
  } else {
    console.log('Firebase не настроен, пуши отключены');
  }
} catch (err) {
  console.warn('Firebase недоступен, пуши отключены:', err.message);
}

function nameKey(name) {
  return normalize2(name).toLowerCase();
}

function sendPush(name, title, body, data) {
  if (!messaging) return;
  const token = pushTokens.get(nameKey(name));
  if (!token) return;
  messaging
    .send({ token, notification: { title, body }, data: data || {} })
    .catch((err) => {
      console.warn('Не удалось отправить пуш:', err.message);
      if (err.code === 'messaging/registration-token-not-registered') {
        pushTokens.delete(nameKey(name));
      }
    });
}

function sendPushToRoom(room, title, body, excludeId) {
  room.players.forEach((p) => {
    if (p.id === excludeId) return;
    if (activeSockets.has(p.id)) return;
    sendPush(p.name, title, body);
  });
}

function emptyStats() {
  return { games: 0, wins: 0, losses: 0, bestStreak: 0, currentStreak: 0 };
}

function getStats(name) {
  const key = normalize2(name).toLowerCase();
  return stats.get(key) || emptyStats();
}

function recordGame(playersList, winnerId) {
  playersList.forEach((p) => {
    const key = normalize2(p.name).toLowerCase();
    const s = stats.get(key) || emptyStats();
    s.games += 1;
    if (winnerId === null) {
      s.currentStreak = 0;
    } else if (p.id === winnerId) {
      s.wins += 1;
      s.currentStreak += 1;
      if (s.currentStreak > s.bestStreak) s.bestStreak = s.currentStreak;
    } else {
      s.losses += 1;
      s.currentStreak = 0;
    }
    stats.set(key, s);
  });
}

const START_LETTERS = 'абвгдежзиклмнопрстуфхцчшщэюя'.split('');

const BAD_WORDS = [
  'хуй', 'хуе', 'хуя', 'хую', 'хуи', 'охуе', 'ахуе', 'нахуй', 'похуй', 'нахуя',
  'пизд', 'блят', 'бляд', 'еба', 'ебал', 'ебан', 'ебат', 'ебет', 'ебут', 'ебен',
  'еби', 'ебуч', 'ебы', 'долбо', 'мудак', 'мудил', 'мудач', 'залуп', 'мандав',
  'гандон', 'гондон', 'пидор', 'пидар', 'пидр', 'педик', 'сука', 'сучк', 'сучон',
  'сучар', 'вагин', 'сперм', 'жоп', 'срак', 'говн', 'херн', 'херня',
  'fuck', 'shit', 'bitch', 'dick', 'pussy', 'nigger', 'nigga', 'cunt', 'asshole',
];

function normalize(word) {
  return String(word == null ? '' : word).toLowerCase().trim().replace(/ё/g, 'е');
}

function nextLetterFrom(word) {
  const w = normalize(word);
  let i = w.length - 1;
  while (i >= 0 && (w[i] === 'ь' || w[i] === 'ъ' || w[i] === 'ы')) i--;
  if (i < 0) return w[w.length - 1] || '';
  return w[i];
}

function isBadWord(word) {
  const w = normalize(word);
  return BAD_WORDS.some((bad) => w.includes(bad));
}

const WORDS_PATH = path.join(__dirname, 'data', 'words.txt');
let wordBuf = null;
let wordOffsets = null;

function loadWords() {
  if (!fs.existsSync(WORDS_PATH)) {
    console.warn('Словарь не найден: ' + WORDS_PATH + ' (проверка слов отключена)');
    return;
  }
  wordBuf = fs.readFileSync(WORDS_PATH);
  const offsets = [];
  offsets.push(0);
  for (let i = 0; i < wordBuf.length; i++) {
    if (wordBuf[i] === 10) offsets.push(i + 1);
  }
  wordOffsets = Uint32Array.from(offsets);
  console.log('Словарь загружен: ' + wordOffsets.length + ' слов');
}

function wordAt(i) {
  const start = wordOffsets[i];
  const end = i + 1 < wordOffsets.length ? wordOffsets[i + 1] - 1 : wordBuf.length;
  return wordBuf.subarray(start, end);
}

function hasWord(word) {
  if (!wordBuf || !word) return true;
  const target = Buffer.from(word, 'utf8');
  let lo = 0;
  let hi = wordOffsets.length - 1;
  while (lo <= hi) {
    const mid = (lo + hi) >> 1;
    const cmp = Buffer.compare(wordAt(mid), target);
    if (cmp === 0) return true;
    if (cmp < 0) lo = mid + 1;
    else hi = mid - 1;
  }
  return false;
}

function isKnownWord(word) {
  if (!wordBuf) return true;
  const w = normalize(word);
  if (hasWord(w)) return true;
  if (w.length > 4 && hasWord(w.slice(0, -1))) return true;
  if (w.length > 5 && hasWord(w.slice(0, -2))) return true;
  if (w.length > 4 && hasWord(w.slice(0, -1) + 'ь')) return true;
  return false;
}

function clamp(value, min, max, fallback) {
  const n = parseInt(value, 10);
  if (Number.isNaN(n)) return fallback;
  return Math.max(min, Math.min(max, n));
}

function generateRoomId() {
  let id;
  do {
    id = Math.random().toString(36).slice(2, 8);
  } while (rooms.has(id));
  return id;
}

function generateCode() {
  let code;
  do {
    code = String(Math.floor(1000 + Math.random() * 9000));
  } while ([...rooms.values()].some((r) => r.code === code));
  return code;
}

function uniqueName(room, name) {
  let base = normalize2(name) || 'Игрок';
  base = base.slice(0, 20);
  let candidate = base;
  let i = 2;
  while (room.players.some((p) => p.name === candidate)) {
    candidate = base + ' ' + i;
    i++;
  }
  return candidate;
}

function normalize2(name) {
  return String(name == null ? '' : name).trim().replace(/\s+/g, ' ');
}

function publicRoomInfo(room) {
  return {
    id: room.id,
    name: room.name,
    players: room.players.length,
    maxPlayers: room.maxPlayers,
    timer: room.timer,
    state: room.state,
  };
}

function roomPayload(room) {
  return {
    id: room.id,
    name: room.name,
    isPrivate: room.isPrivate,
    code: room.isPrivate ? room.code : null,
    hostId: room.hostId,
    timer: room.timer,
    maxPlayers: room.maxPlayers,
    state: room.state,
    players: room.players.map((p) => ({ id: p.id, name: p.name, alive: p.alive })),
  };
}

function gamePayload(room) {
  return {
    requiredLetter: room.requiredLetter,
    lastWord: room.lastWord,
    turnPlayerId: room.turnPlayerId,
    deadline: room.deadline,
    endIn: Math.max(0, room.deadline - Date.now()),
    timer: room.timer,
    usedWords: [...room.usedWords],
    state: room.state,
    winner: room.winner,
  };
}

function sendRoom(room) {
  io.to(room.id).emit('roomUpdate', roomPayload(room));
}

function sendGame(room) {
  io.to(room.id).emit('gameUpdate', gamePayload(room));
}

function broadcastRoomList() {
  const list = [];
  for (const room of rooms.values()) {
    if (!room.isPrivate && room.state === 'lobby') list.push(publicRoomInfo(room));
  }
  io.emit('roomList', list);
}

function nextAliveId(room, fromId) {
  const n = room.players.length;
  if (n === 0) return null;
  let start = room.players.findIndex((p) => p.id === fromId);
  for (let step = 1; step <= n + 1; step++) {
    const idx = (start + step) % n;
    const cand = room.players[idx];
    if (cand && cand.alive) return cand.id;
  }
  return null;
}

function startTurn(room) {
  clearTimeout(room.turnTimer);
  room.deadline = Date.now() + room.timer * 1000;
  sendGame(room);
  const turnPlayer = room.players.find((p) => p.id === room.turnPlayerId);
  if (turnPlayer && !activeSockets.has(turnPlayer.id)) {
    sendPush(
      turnPlayer.name,
      'Твой ход!',
      'Назови слово на «' + room.requiredLetter.toUpperCase() + '»',
    );
  }
  room.turnTimer = setTimeout(() => handleTimeout(room), room.timer * 1000 + 400);
}

function handleTimeout(room) {
  if (room.state !== 'playing') return;
  const player = room.players.find((p) => p.id === room.turnPlayerId);
  if (!player) {
    room.turnPlayerId = nextAliveId(room, room.turnPlayerId);
    if (room.turnPlayerId) startTurn(room);
    return;
  }
  player.alive = false;
  io.to(room.id).emit('playerEliminated', {
    id: player.id,
    name: player.name,
    reason: 'timeout',
  });
  sendRoom(room);
  if (checkGameOver(room)) return;
  room.turnPlayerId = nextAliveId(room, room.turnPlayerId);
  startTurn(room);
}

function checkGameOver(room) {
  const alive = room.players.filter((p) => p.alive);
  if (alive.length <= 1) {
    room.state = 'finished';
    clearTimeout(room.turnTimer);
    room.turnTimer = null;
    room.turnPlayerId = null;
    room.winner = alive[0] ? { id: alive[0].id, name: alive[0].name } : null;
    recordGame(room.players, room.winner ? room.winner.id : null);
    sendRoom(room);
    sendGame(room);
    io.to(room.id).emit('gameOver', { winner: room.winner });
    if (room.winner) {
      sendPushToRoom(room, 'Игра окончена', 'Победил ' + room.winner.name + '!');
    } else {
      sendPushToRoom(room, 'Игра окончена', 'Ничья!');
    }
    return true;
  }
  return false;
}

function beginGame(room) {
  clearTimeout(room.turnTimer);
  room.state = 'playing';
  room.usedWords = new Set();
  room.lastWord = '';
  room.winner = null;
  room.players.forEach((p) => {
    p.alive = true;
  });
  room.requiredLetter = START_LETTERS[Math.floor(Math.random() * START_LETTERS.length)];
  room.turnPlayerId = room.players[Math.floor(Math.random() * room.players.length)].id;
  sendRoom(room);
  io.to(room.id).emit('gameStarted', {});
  startTurn(room);
  sendPushToRoom(room, 'Игра началась!', 'Скорее открывай «Битву слов»');
}

function leaveRoom(socket) {
  const info = players.get(socket.id);
  if (!info) return;
  const room = rooms.get(info.roomId);
  players.delete(socket.id);
  if (!room) return;

  socket.leave(room.id);
  room.players = room.players.filter((p) => p.id !== socket.id);

  if (room.players.length === 0) {
    clearTimeout(room.turnTimer);
    rooms.delete(room.id);
    broadcastRoomList();
    return;
  }

  if (room.hostId === socket.id) room.hostId = room.players[0].id;

  if (room.state === 'playing') {
    if (room.players.length < 2) {
      checkGameOver(room);
    } else {
      const turnStillValid = room.players.some((p) => p.id === room.turnPlayerId && p.alive);
      if (!turnStillValid) {
        room.turnPlayerId = nextAliveId(room, room.turnPlayerId);
        if (room.turnPlayerId) startTurn(room);
      }
    }
  }

  sendRoom(room);
  broadcastRoomList();
}

io.on('connection', (socket) => {
  socket.emit('connected', { id: socket.id });
  socket.emit('roomList', [...rooms.values()]
    .filter((r) => !r.isPrivate && r.state === 'lobby')
    .map(publicRoomInfo));

  socket.on('createRoom', (payload, callback) => {
    const data = payload || {};
    const name = normalize2(data.name).slice(0, 30) || 'Комната';
    const isPrivate = !!data.isPrivate;
    const timer = clamp(data.timer, 5, 120, 15);
    const maxPlayers = clamp(data.maxPlayers, 2, 6, 6);

    const room = {
      id: generateRoomId(),
      name,
      isPrivate,
      code: isPrivate ? generateCode() : null,
      hostId: socket.id,
      timer,
      maxPlayers,
      players: [],
      state: 'lobby',
      turnPlayerId: null,
      requiredLetter: '',
      lastWord: '',
      usedWords: new Set(),
      turnTimer: null,
      deadline: 0,
      winner: null,
    };
    rooms.set(room.id, room);

    const playerName = uniqueName(room, data.playerName);
    room.players.push({ id: socket.id, name: playerName, alive: true });
    players.set(socket.id, { id: socket.id, name: playerName, roomId: room.id });
    socket.join(room.id);

    if (typeof callback === 'function') callback({ ok: true, roomId: room.id });
    sendRoom(room);
    broadcastRoomList();
  });

  socket.on('joinRoom', (payload, callback) => {
    const data = payload || {};
    let room = null;

    if (data.code) {
      room = [...rooms.values()].find((r) => r.isPrivate && r.code === normalize2(data.code));
    } else if (data.roomId) {
      room = rooms.get(data.roomId);
    }

    const fail = (message) => {
      if (typeof callback === 'function') callback({ ok: false, error: message });
      socket.emit('errorMessage', { message });
    };

    if (!room) return fail('Комната не найдена');
    if (room.state !== 'lobby') return fail('Игра уже идёт');
    if (room.players.length >= room.maxPlayers) return fail('Комната заполнена');
    if (players.has(socket.id)) leaveRoom(socket);

    const playerName = uniqueName(room, data.playerName);
    room.players.push({ id: socket.id, name: playerName, alive: true });
    players.set(socket.id, { id: socket.id, name: playerName, roomId: room.id });
    socket.join(room.id);

    if (typeof callback === 'function') callback({ ok: true, roomId: room.id });
    sendRoom(room);
    broadcastRoomList();
    sendPushToRoom(room, 'Новый игрок', playerName + ' зашёл в комнату', socket.id);
  });

  socket.on('leaveRoom', () => {
    leaveRoom(socket);
  });

  socket.on('startGame', () => {
    const info = players.get(socket.id);
    if (!info) return;
    const room = rooms.get(info.roomId);
    if (!room) return;
    if (room.state === 'playing') return;
    if (room.hostId !== socket.id && room.state !== 'finished') {
      return socket.emit('errorMessage', { message: 'Игру запускает создатель комнаты' });
    }
    if (room.players.length < 2) {
      return socket.emit('errorMessage', { message: 'Нужно минимум 2 игрока' });
    }
    beginGame(room);
  });

  socket.on('roomListRequest', () => {
    broadcastRoomList();
  });

  socket.on('statsRequest', (payload, callback) => {
    const name = normalize2(payload && payload.name);
    if (typeof callback === 'function') callback(getStats(name));
  });

  socket.on('registerPush', (payload) => {
    const name = normalize2(payload && payload.name);
    const token = payload && payload.token;
    if (!name || !token) return;
    pushTokens.set(nameKey(name), token);
  });

  socket.on('unregisterPush', (payload) => {
    const name = normalize2(payload && payload.name);
    if (!name) return;
    pushTokens.delete(nameKey(name));
  });

  socket.on('setActive', (payload) => {
    const active = !!(payload && payload.active);
    if (active) activeSockets.add(socket.id);
    else activeSockets.delete(socket.id);
  });

  socket.on('submitWord', (payload) => {
    const info = players.get(socket.id);
    if (!info) return;
    const room = rooms.get(info.roomId);
    if (!room || room.state !== 'playing') return;
    if (room.turnPlayerId !== socket.id) {
      return socket.emit('wordError', { message: 'Сейчас не твой ход' });
    }

    const word = normalize(payload && payload.word);
    if (!word) return socket.emit('wordError', { message: 'Введи слово' });
    if (!word.startsWith(room.requiredLetter)) {
      return socket.emit('wordError', {
        message: 'Слово должно начинаться на «' + room.requiredLetter.toUpperCase() + '»',
      });
    }
    if (isBadWord(word)) {
      return socket.emit('wordError', { message: 'Запрещённое слово' });
    }
    if (room.usedWords.has(word)) {
      return socket.emit('wordError', { message: 'Это слово уже было' });
    }
    if (!isKnownWord(word)) {
      return socket.emit('wordError', { message: 'Такого слова нет. Напиши другое' });
    }

    room.usedWords.add(word);
    room.lastWord = word;
    room.requiredLetter = nextLetterFrom(word);
    socket.emit('wordAccepted', { word });

    room.turnPlayerId = nextAliveId(room, room.turnPlayerId);
    startTurn(room);
  });

  socket.on('disconnect', () => {
    activeSockets.delete(socket.id);
    leaveRoom(socket);
  });
});

loadWords();

server.listen(PORT, () => {
  console.log('Битва слов запущена на http://localhost:' + PORT);
});
