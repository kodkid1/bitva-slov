const { io } = require('socket.io-client');

const URL = 'http://localhost:3000';

function waitFor(socket, event, timeout = 4000) {
  return new Promise((resolve, reject) => {
    const t = setTimeout(() => reject(new Error('timeout ' + event)), timeout);
    socket.once(event, (d) => {
      clearTimeout(t);
      resolve(d);
    });
  });
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const a = io(URL, { transports: ['websocket'] });
  const b = io(URL, { transports: ['websocket'] });

  const bLists = [];
  b.on('roomList', (list) => bLists.push(list));

  await waitFor(a, 'connected');
  await waitFor(b, 'connected');
  await sleep(300);
  const before = bLists.length;
  console.log('B список на старте:', JSON.stringify(bLists[before - 1]));

  const created = await new Promise((r) =>
    a.emit(
      'createRoom',
      { playerName: 'Аня', name: 'Публичная', isPrivate: false, timer: 15, maxPlayers: 6 },
      r
    )
  );
  console.log('комната создана:', JSON.stringify(created));

  await sleep(500);
  const list = bLists[bLists.length - 1] || [];
  console.log('B получил обновлений списка:', bLists.length - before);
  console.log('B последний список:', JSON.stringify(list));
  const visible = list.some((r) => r.id === created.roomId);
  console.log(visible ? 'OK: комната видна другому игроку' : 'БАГ: комната НЕ видна другому игроку');

  a.close();
  b.close();
  process.exit(visible ? 0 : 1);
}

main().catch((e) => {
  console.error('ОШИБКА:', e.message);
  process.exit(1);
});
