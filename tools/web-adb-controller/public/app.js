/* =========================================================
   LIQUID STUDIO • ADB & SCRCPY CLIENT CONTROLLER
   ========================================================= */

document.addEventListener('DOMContentLoaded', () => {
    // State
    let deviceResolution = { width: 1080, height: 2400 };
    let screenWs = null;
    let logcatWs = null;
    let isLogcatPaused = false;
    let activeLogcatTag = '';
    let isMouseDown = false;
    let touchStartX = 0;
    let touchStartY = 0;
    let touchStartTime = 0;
    let frameCount = 0;
    let lastFpsUpdate = Date.now();

    // DOM Elements
    const canvas = document.getElementById('screenCanvas');
    const ctx = canvas.getContext('2d');
    const screenPlaceholder = document.getElementById('screenPlaceholder');
    const touchFeedback = document.getElementById('touchFeedback');
    const fpsCounter = document.getElementById('fpsCounter');

    // Telemetry Elements
    const deviceModelEl = document.getElementById('deviceModel');
    const androidVersionEl = document.getElementById('androidVersion');
    const batteryLevelEl = document.getElementById('batteryLevel');
    const deviceResolutionEl = document.getElementById('deviceResolution');
    const connectionStatusEl = document.getElementById('connectionStatus');

    // ----------------------------------------------------
    // 1. Telemetry & Device Info
    // ----------------------------------------------------
    async function fetchDeviceInfo() {
        try {
            const res = await fetch('/api/device/info');
            if (!res.ok) throw new Error('Failed to fetch info');
            const data = await res.json();

            deviceModelEl.textContent = data.model || 'Android Device';
            androidVersionEl.textContent = `v${data.androidVersion || '--'}`;
            batteryLevelEl.textContent = `${data.battery.level}% ${data.battery.isCharging ? '⚡' : ''}`;
            
            if (data.resolution) {
                deviceResolution = data.resolution;
                deviceResolutionEl.textContent = `${data.resolution.width}×${data.resolution.height}`;
                canvas.width = data.resolution.width;
                canvas.height = data.resolution.height;
            }

            connectionStatusEl.textContent = 'Connected';
            connectionStatusEl.style.color = 'var(--accent-emerald)';
        } catch (e) {
            connectionStatusEl.textContent = 'Offline / Retrying';
            connectionStatusEl.style.color = 'var(--accent-coral)';
        }
    }

    fetchDeviceInfo();
    setInterval(fetchDeviceInfo, 6000);

    // ----------------------------------------------------
    // 2. In-Browser Screen Mirroring (WebSocket Stream)
    // ----------------------------------------------------
    function initScreenStream() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        screenWs = new WebSocket(`${protocol}//${window.location.host}/ws/screen`);

        const img = new Image();
        img.onload = () => {
            ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
            screenPlaceholder.style.display = 'none';

            // Calculate FPS
            frameCount++;
            const now = Date.now();
            if (now - lastFpsUpdate >= 1000) {
                const fps = Math.round((frameCount * 1000) / (now - lastFpsUpdate));
                fpsCounter.textContent = `${fps} FPS`;
                frameCount = 0;
                lastFpsUpdate = now;
            }
        };

        screenWs.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (data.type === 'frame') {
                    img.src = data.image;
                }
            } catch (err) {}
        };

        screenWs.onclose = () => {
            setTimeout(initScreenStream, 2000);
        };
    }

    initScreenStream();

    // ----------------------------------------------------
    // 3. Interactive Touch & Gestures Mapping
    // ----------------------------------------------------
    function getCanvasCoordinates(e) {
        const rect = canvas.getBoundingClientRect();
        const clientX = e.clientX || (e.touches && e.touches[0].clientX);
        const clientY = e.clientY || (e.touches && e.touches[0].clientY);

        const scaleX = deviceResolution.width / rect.width;
        const scaleY = deviceResolution.height / rect.height;

        const x = (clientX - rect.left) * scaleX;
        const y = (clientY - rect.top) * scaleY;

        return {
            x: Math.max(0, Math.min(deviceResolution.width, x)),
            y: Math.max(0, Math.min(deviceResolution.height, y)),
            uiX: clientX - rect.left,
            uiY: clientY - rect.top
        };
    }

    function showTouchRipple(uiX, uiY) {
        touchFeedback.style.left = `${uiX}px`;
        touchFeedback.style.top = `${uiY}px`;
        touchFeedback.classList.remove('active');
        void touchFeedback.offsetWidth; // trigger reflow
        touchFeedback.classList.add('active');
    }

    canvas.addEventListener('mousedown', (e) => {
        isMouseDown = true;
        const coords = getCanvasCoordinates(e);
        touchStartX = coords.x;
        touchStartY = coords.y;
        touchStartTime = Date.now();
        showTouchRipple(coords.uiX, coords.uiY);
    });

    canvas.addEventListener('mouseup', async (e) => {
        if (!isMouseDown) return;
        isMouseDown = false;

        const coords = getCanvasCoordinates(e);
        const touchEndX = coords.x;
        const touchEndY = coords.y;
        const duration = Math.min(1000, Math.max(100, Date.now() - touchStartTime));

        const dist = Math.hypot(touchEndX - touchStartX, touchEndY - touchStartY);

        if (dist < 15) {
            // Single Tap
            await fetch('/api/adb/tap', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ x: touchStartX, y: touchStartY })
            });
        } else {
            // Swipe / Scroll Gesture
            await fetch('/api/adb/swipe', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    x1: touchStartX,
                    y1: touchStartY,
                    x2: touchEndX,
                    y2: touchEndY,
                    duration
                })
            });
        }
    });

    // ----------------------------------------------------
    // 4. Hardware Keys & Typing
    // ----------------------------------------------------
    async function sendKey(keycode) {
        await fetch('/api/adb/key', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ keycode })
        });
    }

    document.getElementById('navBack').addEventListener('click', () => sendKey(4));
    document.getElementById('navHome').addEventListener('click', () => sendKey(3));
    document.getElementById('navRecents').addEventListener('click', () => sendKey(187));
    document.getElementById('navPower').addEventListener('click', () => sendKey(26));
    document.getElementById('navVolDown').addEventListener('click', () => sendKey(25));
    document.getElementById('navVolUp').addEventListener('click', () => sendKey(24));

    // Text Input Bar
    const adbTextInput = document.getElementById('adbTextInput');
    const btnSendText = document.getElementById('btnSendText');

    async function sendTextInput() {
        const text = adbTextInput.value.trim();
        if (!text) return;
        await fetch('/api/adb/text', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text })
        });
        adbTextInput.value = '';
    }

    btnSendText.addEventListener('click', sendTextInput);
    adbTextInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') sendTextInput();
    });

    // ----------------------------------------------------
    // 5. Native Scrcpy Launcher & Screenshot
    // ----------------------------------------------------
    async function launchNativeScrcpy() {
        const res = await fetch('/api/scrcpy/start', { method: 'POST' });
        const data = await res.json();
        showStatusBanner(data.message || 'Scrcpy 120Hz Window Opened!');
    }

    document.getElementById('btnLaunchScrcpy').addEventListener('click', launchNativeScrcpy);
    document.getElementById('btnLaunchScrcpySide').addEventListener('click', launchNativeScrcpy);

    document.getElementById('btnSaveScreenshot').addEventListener('click', () => {
        window.open('/api/screenshot', '_blank');
    });

    document.getElementById('btnRefreshDevice').addEventListener('click', fetchDeviceInfo);

    // ----------------------------------------------------
    // 6. Tabs Management
    // ----------------------------------------------------
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            tabButtons.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            const targetId = btn.getAttribute('data-tab');
            document.getElementById(targetId).classList.add('active');
        });
    });

    // ----------------------------------------------------
    // 7. App Lifecycle Controls
    // ----------------------------------------------------
    function showStatusBanner(msg) {
        const banner = document.getElementById('actionStatusBanner');
        const msgEl = document.getElementById('actionStatusMsg');
        msgEl.textContent = msg;
        banner.style.display = 'block';
        setTimeout(() => { banner.style.display = 'none'; }, 4000);
    }

    async function triggerAppAction(endpoint, successMessage) {
        try {
            const res = await fetch(endpoint, { method: 'POST' });
            const data = await res.json();
            showStatusBanner(successMessage);
        } catch (e) {
            showStatusBanner(`Error: ${e.message}`);
        }
    }

    document.getElementById('btnLaunchApp').addEventListener('click', () => {
        triggerAppAction('/api/app/launch', 'Liquid Expense Manager Launched!');
    });

    document.getElementById('btnRestartApp').addEventListener('click', () => {
        triggerAppAction('/api/app/restart', 'App Relaunched!');
    });

    document.getElementById('btnStopApp').addEventListener('click', () => {
        triggerAppAction('/api/app/stop', 'App Force-Stopped!');
    });

    document.getElementById('btnClearData').addEventListener('click', () => {
        if (confirm('Clear all app data and cache?')) {
            triggerAppAction('/api/app/clear', 'App data cleared!');
        }
    });

    document.getElementById('btnGrantPermissions').addEventListener('click', () => {
        triggerAppAction('/api/app/grant-permissions', 'SMS & Notification Permissions Granted!');
    });

    // ----------------------------------------------------
    // 8. SMS Transaction Simulator Studio
    // ----------------------------------------------------
    const smsSenderInput = document.getElementById('smsSender');
    const smsBodyInput = document.getElementById('smsBody');
    const btnInjectSms = document.getElementById('btnInjectSms');

    // Preset Chips
    document.querySelectorAll('.preset-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const sender = chip.getAttribute('data-sender');
            const body = chip.getAttribute('data-body');
            smsSenderInput.value = sender;
            smsBodyInput.value = body;
            injectSms(sender, body);
        });
    });

    async function injectSms(sender, body) {
        try {
            btnInjectSms.disabled = true;
            btnInjectSms.textContent = 'Broadcasting...';

            const res = await fetch('/api/sms/simulate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sender, body })
            });

            const data = await res.json();
            showStatusBanner(`SMS Broadcast sent from "${sender}"! Check app parser.`);
        } catch (e) {
            showStatusBanner(`Broadcast error: ${e.message}`);
        } finally {
            btnInjectSms.disabled = false;
            btnInjectSms.innerHTML = `
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none">
                    <line x1="22" y1="2" x2="11" y2="13"></line>
                    <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                </svg>
                <span>Broadcast SMS to App</span>
            `;
        }
    }

    btnInjectSms.addEventListener('click', () => {
        injectSms(smsSenderInput.value.trim(), smsBodyInput.value.trim());
    });

    // ----------------------------------------------------
    // 9. Build & Deploy Console
    // ----------------------------------------------------
    const btnRunBuild = document.getElementById('btnRunBuild');
    const buildLogBody = document.getElementById('buildLogBody');

    btnRunBuild.addEventListener('click', () => {
        buildLogBody.textContent = '🚀 Starting Gradle installDebug build...\n';
        btnRunBuild.disabled = true;

        const eventSource = new EventSource('/api/app/build-install');

        eventSource.onmessage = (event) => {
            const data = JSON.parse(event.data);
            if (data.type === 'stdout' || data.type === 'stderr') {
                buildLogBody.textContent += data.text;
                buildLogBody.scrollTop = buildLogBody.scrollHeight;
            } else if (data.type === 'done') {
                buildLogBody.textContent += `\n✨ Build process exited with code ${data.code}.\n`;
                btnRunBuild.disabled = false;
                eventSource.close();
            }
        };

        eventSource.onerror = () => {
            buildLogBody.textContent += '\n⚠️ Connection to build process closed.\n';
            btnRunBuild.disabled = false;
            eventSource.close();
        };
    });

    // ----------------------------------------------------
    // 10. Real-Time Logcat Streamer
    // ----------------------------------------------------
    const logcatBody = document.getElementById('logcatBody');
    const logcatSearch = document.getElementById('logcatSearch');
    const btnToggleLogcat = document.getElementById('btnToggleLogcat');
    const btnClearLogcat = document.getElementById('btnClearLogcat');

    function initLogcatStream() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        logcatWs = new WebSocket(`${protocol}//${window.location.host}/ws/logcat`);

        logcatWs.onmessage = (event) => {
            if (isLogcatPaused) return;

            try {
                const data = JSON.parse(event.data);
                if (data.type === 'log') {
                    appendLogLine(data.raw);
                }
            } catch (err) {}
        };
    }

    function appendLogLine(raw) {
        const query = logcatSearch.value.trim().toLowerCase();
        if (query && !raw.toLowerCase().includes(query)) return;
        if (activeLogcatTag && !raw.includes(activeLogcatTag)) return;

        const div = document.createElement('div');
        div.className = 'log-line';

        if (raw.includes(' E ') || raw.includes('AndroidRuntime: FATAL') || raw.includes('Exception')) {
            div.classList.add('error');
        } else if (raw.includes(' W ')) {
            div.classList.add('warn');
        } else if (raw.includes(' I ')) {
            div.classList.add('info');
        } else {
            div.classList.add('debug');
        }

        div.textContent = raw;
        logcatBody.appendChild(div);

        // Keep maximum 500 lines in DOM for ultra-smooth 60fps performance
        if (logcatBody.children.length > 500) {
            logcatBody.removeChild(logcatBody.children[0]);
        }

        logcatBody.scrollTop = logcatBody.scrollHeight;
    }

    initLogcatStream();

    btnToggleLogcat.addEventListener('click', () => {
        isLogcatPaused = !isLogcatPaused;
        btnToggleLogcat.textContent = isLogcatPaused ? 'Resume' : 'Pause';
    });

    btnClearLogcat.addEventListener('click', () => {
        logcatBody.innerHTML = '';
    });

    // Tag Quick Filters
    document.querySelectorAll('.tag-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            document.querySelectorAll('.tag-chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            activeLogcatTag = chip.getAttribute('data-tag');
        });
    });

    // ----------------------------------------------------
    // 11. ADB Shell Terminal
    // ----------------------------------------------------
    const terminalOutput = document.getElementById('terminalOutput');
    const terminalCmdInput = document.getElementById('terminalCmdInput');
    const btnExecTerminal = document.getElementById('btnExecTerminal');

    async function executeTerminalCmd() {
        const cmd = terminalCmdInput.value.trim();
        if (!cmd) return;

        terminalOutput.textContent += `\n$ adb ${cmd}\n`;
        terminalCmdInput.value = '';

        try {
            const res = await fetch('/api/terminal', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ command: cmd })
            });
            const data = await res.json();
            terminalOutput.textContent += data.output || '(empty output)\n';
        } catch (e) {
            terminalOutput.textContent += `Error: ${e.message}\n`;
        }

        terminalOutput.scrollTop = terminalOutput.scrollHeight;
    }

    btnExecTerminal.addEventListener('click', executeTerminalCmd);
    terminalCmdInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') executeTerminalCmd();
    });
});
