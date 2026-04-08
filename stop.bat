@echo off
chcp 65001 >nul
:: ============================================================
::  LexAI 停止脚本（Windows 版）
::  关闭占用 8081 / 5173 端口的进程
:: ============================================================

echo.
echo  正在停止 LexAI 服务...
echo.

:: 停止后端 (8081)
for /f "tokens=5" %%p in ('netstat -aon ^| findstr ":8081 " ^| findstr "LISTENING"') do (
    echo  [i] 正在关闭后端进程 PID: %%p
    taskkill /PID %%p /F >nul 2>&1
)

:: 停止前端 (5173)
for /f "tokens=5" %%p in ('netstat -aon ^| findstr ":5173 " ^| findstr "LISTENING"') do (
    echo  [i] 正在关闭前端进程 PID: %%p
    taskkill /PID %%p /F >nul 2>&1
)

echo.
echo  [✓] LexAI 服务已停止
echo.
pause
