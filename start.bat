@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Запускаю "Битва слов" на http://localhost:3000
echo Открой этот адрес в браузере. Закрыть игру - Ctrl+C.
node server.js
pause
