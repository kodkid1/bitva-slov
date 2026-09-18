const { io } = require('socket.io-client');

const URL = 'http://localhost:3000';
const results = [];

function wait(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function waitFor(socket, event, timeout = 4000) {
  return new Promise((resolve, reject) => {
    const t = setTimeout(() => reject(new Error('timeout waiting for ' + event)), timeout);
    socket.once(event, (data) => {
      clearTimeout(t);
      resolve(data);
    });
  });
}

function check(name, ok, extra = '') {
  results.push({ name, ok });
  console.log((ok ? 'PASS' : 'FAIL') + ' - ' + name + (extra ? ' :: ' + extra : ''));
}

async function main() {
  const a = io(URL, { transports: ['websocket'] });
  const b = io(URL, { transports: ['websocket'] });
  await waitFor(a, 'connected');
  await waitFor(b, 'connected');

  const roomUpdateA = waitFor(a, 'roomUpdate');
  const created = await new Promise((resolve) =>
    a.emit(
      'createRoom',
      { playerName: 'Аня', name: 'Тест', isPrivate: true, timer: 5, maxPlayers: 2 },
      resolve
    )
  );
  check('комната создана', created && created.ok, JSON.stringify(created));

  const first = await roomUpdateA;
  check('приватная комната имеет код', !!first.code, 'code=' + first.code);
  check('создатель - хост', first.hostId === a.id);

  const roomUpdateB = waitFor(b, 'roomUpdate');
  const joined = await new Promise((resolve) =>
    b.emit('joinRoom', { code: first.code, playerName: 'Боря' }, resolve)
  );
  check('вход по коду работает', joined && joined.ok, JSON.stringify(joined));
  await roomUpdateB;

  const startedA = waitFor(a, 'gameUpdate');
  a.emit('startGame');
  const g1 = await startedA;
  check('игра началась, есть буква', !!g1.requiredLetter, 'letter=' + g1.requiredLetter);
  check('ход у одного из игроков', g1.turnPlayerId === a.id || g1.turnPlayerId === b.id);

  const turnSocket = g1.turnPlayerId === a.id ? a : b;
  const otherSocket = turnSocket === a ? b : a;

  const errOther = waitFor(otherSocket, 'wordError');
  otherSocket.emit('submitWord', { word: g1.requiredLetter + 'ааа' });
  const eo = await errOther;
  check('нельзя ходить не в свой ход', !!eo.message, eo.message);

  const errLetter = waitFor(turnSocket, 'wordError');
  turnSocket.emit('submitWord', { word: 'ъъъ' });
  const el = await errLetter;
  check('неверная буква отклоняется', !!el.message, el.message);

  const errFake = waitFor(turnSocket, 'wordError');
  turnSocket.emit('submitWord', { word: g1.requiredLetter + 'бвгджз' });
  const ef = await errFake;
  check('набор букв (не слово) отклоняется', !!ef.message, ef.message);

  const WORDS = {
    а: 'арбуз', б: 'банан', в: 'вода', г: 'гора', д: 'дом', е: 'енот',
    ж: 'жаба', з: 'заяц', и: 'игра', к: 'кот', л: 'луна', м: 'мама',
    н: 'нос', о: 'окно', п: 'парта', р: 'рыба', с: 'собака', т: 'тигр',
    у: 'урок', ф: 'футбол', х: 'хлеб', ц: 'цветок', ч: 'часы', ш: 'школа',
    щ: 'щука', э: 'этаж', ю: 'юла', я: 'яблоко',
  };
  const validWord = WORDS[g1.requiredLetter];
  check('есть слово на выпавшую букву', !!validWord, g1.requiredLetter + ' -> ' + validWord);

  const nextA = waitFor(a, 'gameUpdate');
  const nextB = waitFor(b, 'gameUpdate');
  turnSocket.emit('submitWord', { word: validWord });
  const g2 = await nextA;
  await nextB;
  check('слово принято, ход перешёл', g2.turnPlayerId === otherSocket.id, 'turn=' + g2.turnPlayerId);
  check('слово попало в список', g2.usedWords.includes(validWord), JSON.stringify(g2.usedWords));

  const turnName = turnSocket === a ? 'Аня' : 'Боря';
  const eliminated = waitFor(a, 'playerEliminated', 8000);
  const over = waitFor(a, 'gameOver', 8000);
  const elim = await eliminated;
  const gameover = await over;
  check('игрок выбыл по таймеру', !!elim.name, elim.name);
  check('определён победитель', gameover.winner && gameover.winner.name === turnName, JSON.stringify(gameover.winner) + ' ожидался ' + turnName);

  const loserName = turnName === 'Аня' ? 'Боря' : 'Аня';
  const winnerStats = await new Promise((resolve) =>
    a.emit('statsRequest', { name: turnName }, resolve)
  );
  const loserStats = await new Promise((resolve) =>
    a.emit('statsRequest', { name: loserName }, resolve)
  );
  check('победитель: 1 игра, 1 победа, серия 1',
    winnerStats.games === 1 && winnerStats.wins === 1 && winnerStats.currentStreak === 1 &&
    winnerStats.bestStreak === 1,
    JSON.stringify(winnerStats));
  check('проигравший: 1 игра, 1 поражение, серия 0',
    loserStats.games === 1 && loserStats.losses === 1 && loserStats.currentStreak === 0,
    JSON.stringify(loserStats));

  a.close();
  b.close();

  const failed = results.filter((r) => !r.ok).length;
  console.log('\n' + (results.length - failed) + '/' + results.length + ' тестов пройдено');
  process.exit(failed ? 1 : 0);
}

main().catch((err) => {
  console.error('ОШИБКА ТЕСТА:', err.message);
  process.exit(1);
});
