const { spawn } = require('child_process');
const net = require('net');
const http = require('http');

const PORT = 3000;

function portInUse(port) {
  return new Promise((resolve) => {
    const socket = net.connect({ port, host: '127.0.0.1' });
    socket.setTimeout(1500);
    socket.once('connect', () => {
      socket.destroy();
      resolve(true);
    });
    socket.once('error', () => {
      socket.destroy();
      resolve(false);
    });
    socket.once('timeout', () => {
      socket.destroy();
      resolve(false);
    });
  });
}

function isOurGame(port) {
  return new Promise((resolve) => {
    const req = http.get({ host: '127.0.0.1', port, path: '/', timeout: 1500 }, (res) => {
      let body = '';
      res.on('data', (c) => {
        body += c;
        if (body.length > 4000) req.destroy();
      });
      res.on('end', () => resolve(body.includes('Битва') || body.includes('bitva')));
    });
    req.on('error', () => resolve(false));
    req.on('timeout', () => {
      req.destroy();
      resolve(false);
    });
  });
}

async function main() {
  console.log('============================================================');
  console.log('  БИТВА СЛОВ - ОНЛАЙН');
  console.log('============================================================');

  let server = null;
  const busy = await portInUse(PORT);

  if (busy) {
    const ours = await isOurGame(PORT);
    if (ours) {
      console.log('  На порту ' + PORT + ' уже работает сервер игры - использую его.');
    } else {
      console.log('  ВНИМАНИЕ: порт ' + PORT + ' занят другой программой.');
      console.log('  Закрой её и запусти этот файл заново.');
      process.exit(1);
    }
  } else {
    console.log('  Запускаю сервер...');
    server = spawn('node', ['server.js'], { cwd: __dirname, stdio: ['ignore', 'inherit', 'inherit'] });
    server.on('error', (e) => {
      console.error('Не удалось запустить сервер:', e.message);
      process.exit(1);
    });
    await new Promise((r) => setTimeout(r, 4000));
  }

  console.log('');
  console.log('  Подключаю туннель... ждём ссылку ~10-15 секунд.');
  console.log('');

  const ssh = spawn(
    'ssh',
    [
      '-o', 'StrictHostKeyChecking=no',
      '-o', 'ServerAliveInterval=30',
      '-R', '80:localhost:' + PORT,
      'nokey@localhost.run',
    ],
    { cwd: __dirname }
  );

  let buffer = '';
  let found = false;

  function handle(data) {
    const text = data.toString();
    buffer += text;
    if (!found) {
      const m = buffer.match(/https:\/\/[a-z0-9]+\.lhr\.life/);
      if (m) {
        found = true;
        console.log('');
        console.log('  ============================================');
        console.log('     ССЫЛКА ДЛЯ ТЕЛЕФОНОВ:');
        console.log('     ' + m[0]);
        console.log('  ============================================');
        console.log('');
        console.log('  Скинь эту ссылку друзьям.');
        console.log('  Пока это окно открыто - игра работает.');
        console.log('  Остановить: закрой окно или нажми Ctrl+C.');
        console.log('');
      }
    }
  }

  ssh.stdout.on('data', handle);
  ssh.stderr.on('data', handle);

  ssh.on('error', (e) => {
    console.error('SSH не запустился:', e.message);
    console.error('Проверь, что в Windows включён компонент "Клиент OpenSSH".');
  });

  ssh.on('exit', () => {
    console.log('Туннель закрыт.');
    if (server) server.kill();
    process.exit(0);
  });

  setTimeout(() => {
    if (!found) {
      console.log('');
      console.log('Ссылка не появилась за 40 секунд. Проверь:');
      console.log('- есть ли интернет;');
      console.log('- нет ли строки с ошибкой (Permission denied / Network is unreachable).');
    }
  }, 40000);

  process.on('SIGINT', () => {
    ssh.kill();
    if (server) server.kill();
    process.exit(0);
  });
}

main();
