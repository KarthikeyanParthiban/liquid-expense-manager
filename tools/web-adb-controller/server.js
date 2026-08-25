const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const { spawn, exec } = require('child_process');
const path = require('path');
const cors = require('cors');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

let activeScrcpyProcess = null;
let activeLogcatProcess = null;
let isStreamingScreen = false;
let screenStreamClients = new Set();
let logcatClients = new Set();

// Utility function to execute shell commands
function runAdb(command) {
    return new Promise((resolve, reject) => {
        exec(`adb ${command}`, { maxBuffer: 10 * 1024 * 1024 }, (error, stdout, stderr) => {
            if (error) {
                resolve({ success: false, error: stderr || error.message, stdout });
            } else {
                resolve({ success: true, stdout: stdout.trim(), stderr });
            }
        });
    });
}

// ----------------------------------------------------
// REST API Endpoints
// ----------------------------------------------------

// 1. Get connected devices
app.get('/api/devices', async (req, res) => {
    const result = await runAdb('devices -l');
    if (!result.success) {
        return res.status(500).json({ error: result.error });
    }

    const lines = result.stdout.split('\n').filter(l => l.trim() && !l.startsWith('List of devices'));
    const devices = lines.map(line => {
        const parts = line.split(/\s+/);
        const id = parts[0];
        const state = parts[1];
        const modelMatch = line.match(/model:(\S+)/);
        const deviceMatch = line.match(/device:(\S+)/);
        return {
            id,
            state,
            model: modelMatch ? modelMatch[1] : id,
            device: deviceMatch ? deviceMatch[1] : 'Android Device'
        };
    });

    res.json({ devices });
});

// 2. Get detailed device telemetry & status
app.get('/api/device/info', async (req, res) => {
    try {
        const [modelRes, osRes, sizeRes, batteryRes, ipRes] = await Promise.all([
            runAdb('shell getprop ro.product.model'),
            runAdb('shell getprop ro.build.version.release'),
            runAdb('shell wm size'),
            runAdb('shell dumpsys battery'),
            runAdb('shell ip route | grep wlan0 | head -n 1')
        ]);

        const model = modelRes.stdout || 'Unknown';
        const androidVersion = osRes.stdout || 'Unknown';
        
        let width = 1080;
        let height = 2400;
        const sizeMatch = sizeRes.stdout.match(/(\d+)x(\d+)/);
        if (sizeMatch) {
            width = parseInt(sizeMatch[1]);
            height = parseInt(sizeMatch[2]);
        }

        let batteryLevel = 100;
        const batteryMatch = batteryRes.stdout.match(/level:\s*(\d+)/);
        if (batteryMatch) {
            batteryLevel = parseInt(batteryMatch[1]);
        }

        const isCharging = batteryRes.stdout.includes('status: 2') || batteryRes.stdout.includes('status: 5') || batteryRes.stdout.includes('AC powered: true') || batteryRes.stdout.includes('USB powered: true');

        res.json({
            model,
            androidVersion,
            resolution: { width, height },
            battery: {
                level: batteryLevel,
                isCharging
            },
            ip: ipRes.stdout || '127.0.0.1'
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 3. Touch Input: Tap
app.post('/api/adb/tap', async (req, res) => {
    const { x, y } = req.body;
    if (x === undefined || y === undefined) {
        return res.status(400).json({ error: 'x and y coordinates are required' });
    }
    const result = await runAdb(`shell input tap ${Math.round(x)} ${Math.round(y)}`);
    res.json(result);
});

// 4. Touch Input: Swipe / Drag
app.post('/api/adb/swipe', async (req, res) => {
    const { x1, y1, x2, y2, duration = 300 } = req.body;
    const result = await runAdb(`shell input swipe ${Math.round(x1)} ${Math.round(y1)} ${Math.round(x2)} ${Math.round(y2)} ${duration}`);
    res.json(result);
});

// 5. Hardware Keys
app.post('/api/adb/key', async (req, res) => {
    const { keycode } = req.body;
    // Common: 3=HOME, 4=BACK, 187=APP_SWITCH, 26=POWER, 24=VOL_UP, 25=VOL_DOWN
    const result = await runAdb(`shell input keyevent ${keycode}`);
    res.json(result);
});

// 6. Text Typing
app.post('/api/adb/text', async (req, res) => {
    const { text } = req.body;
    if (!text) return res.json({ success: true });
    // Escape shell special characters
    const escaped = text.replace(/([ "$\\])/g, '\\$1');
    const result = await runAdb(`shell input text "${escaped}"`);
    res.json(result);
});

// 7. Liquid Expense Manager Actions
app.post('/api/app/launch', async (req, res) => {
    const result = await runAdb('shell am start -n com.expensemanager.app/.MainActivity');
    res.json(result);
});

app.post('/api/app/stop', async (req, res) => {
    const result = await runAdb('shell am force-stop com.expensemanager.app');
    res.json(result);
});

app.post('/api/app/restart', async (req, res) => {
    await runAdb('shell am force-stop com.expensemanager.app');
    const result = await runAdb('shell am start -n com.expensemanager.app/.MainActivity');
    res.json(result);
});

app.post('/api/app/clear', async (req, res) => {
    const result = await runAdb('shell pm clear com.expensemanager.app');
    res.json(result);
});

app.post('/api/app/grant-permissions', async (req, res) => {
    const permissions = [
        'android.permission.READ_SMS',
        'android.permission.RECEIVE_SMS',
        'android.permission.POST_NOTIFICATIONS'
    ];
    const results = [];
    for (const p of permissions) {
        results.push(await runAdb(`shell pm grant com.expensemanager.app ${p}`));
    }
    res.json({ success: true, results });
});

// 8. SMS Transaction Simulation Injection
app.post('/api/sms/simulate', async (req, res) => {
    const { sender = 'HDFCBK', body = 'Rs 450.00 spent at Swiggy on 25-Aug-26' } = req.body;
    
    // Broadcast custom simulated intent to app receiver
    const escapedSender = sender.replace(/"/g, '\\"');
    const escapedBody = body.replace(/"/g, '\\"');
    
    const result = await runAdb(`shell am broadcast -a com.expensemanager.app.ACTION_SIMULATE_SMS --es sender "${escapedSender}" --es body "${escapedBody}" -p com.expensemanager.app`);
    res.json(result);
});

// 9. Build and Install Debug APK
app.post('/api/app/build-install', (req, res) => {
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');

    const projectRoot = path.resolve(__dirname, '../../');
    const env = Object.assign({}, process.env, {
        JAVA_HOME: '/home/karthikeyan/android-dev/jdk-17',
        ANDROID_HOME: '/home/karthikeyan/android-dev/sdk',
        PATH: `/home/karthikeyan/android-dev/jdk-17/bin:/home/karthikeyan/android-dev/sdk/platform-tools:${process.env.PATH}`
    });

    const gradleProcess = spawn('./gradlew', ['installDebug'], {
        cwd: projectRoot,
        env
    });

    gradleProcess.stdout.on('data', (data) => {
        res.write(`data: ${JSON.stringify({ type: 'stdout', text: data.toString() })}\n\n`);
    });

    gradleProcess.stderr.on('data', (data) => {
        res.write(`data: ${JSON.stringify({ type: 'stderr', text: data.toString() })}\n\n`);
    });

    gradleProcess.on('close', (code) => {
        res.write(`data: ${JSON.stringify({ type: 'done', code })}\n\n`);
        res.end();
    });
});

// 10. Native Scrcpy Session Control
app.post('/api/scrcpy/start', (req, res) => {
    if (activeScrcpyProcess) {
        return res.json({ success: true, message: 'Scrcpy is already running' });
    }

    try {
        activeScrcpyProcess = spawn('scrcpy', [
            '--always-on-top',
            '--stay-awake',
            '--max-fps=120',
            '--window-title=Liquid Expense Manager - 120Hz Cast'
        ], {
            detached: true,
            stdio: 'ignore'
        });

        activeScrcpyProcess.unref();

        activeScrcpyProcess.on('exit', () => {
            activeScrcpyProcess = null;
        });

        res.json({ success: true, message: 'Native Scrcpy 120Hz window launched!' });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

app.post('/api/scrcpy/stop', (req, res) => {
    if (activeScrcpyProcess) {
        activeScrcpyProcess.kill();
        activeScrcpyProcess = null;
    }
    exec('pkill -9 scrcpy');
    res.json({ success: true, message: 'Scrcpy stopped' });
});

// 11. Screenshot download
app.get('/api/screenshot', (req, res) => {
    res.setHeader('Content-Type', 'image/png');
    res.setHeader('Content-Disposition', 'inline; filename="screenshot.png"');
    const screencap = spawn('adb', ['exec-out', 'screencap', '-p']);
    screencap.stdout.pipe(res);
});

// 12. Arbitrary ADB Terminal command
app.post('/api/terminal', async (req, res) => {
    const { command } = req.body;
    if (!command) return res.status(400).json({ error: 'Command required' });
    
    // Safety check: run through adb
    const cmdToRun = command.startsWith('adb') ? command : `adb ${command}`;
    exec(cmdToRun, { maxBuffer: 5 * 1024 * 1024 }, (error, stdout, stderr) => {
        res.json({
            output: stdout || stderr || (error ? error.message : ''),
            exitCode: error ? error.code : 0
        });
    });
});

// ----------------------------------------------------
// Real-time Screen Streaming Engine via WebSockets
// ----------------------------------------------------
wss.on('connection', (ws, req) => {
    const url = req.url;

    if (url.includes('/ws/screen')) {
        screenStreamClients.add(ws);
        if (!isStreamingScreen) {
            startScreenCaptureLoop();
        }

        ws.on('close', () => {
            screenStreamClients.delete(ws);
            if (screenStreamClients.size === 0) {
                isStreamingScreen = false;
            }
        });
    } else if (url.includes('/ws/logcat')) {
        logcatClients.add(ws);
        startLogcatStreamIfNeeded();

        ws.on('close', () => {
            logcatClients.delete(ws);
            if (logcatClients.size === 0 && activeLogcatProcess) {
                activeLogcatProcess.kill();
                activeLogcatProcess = null;
            }
        });
    }
});

let isCapturingFrame = false;

function startScreenCaptureLoop() {
    isStreamingScreen = true;

    async function captureNext() {
        if (!isStreamingScreen || screenStreamClients.size === 0) {
            isStreamingScreen = false;
            return;
        }

        if (isCapturingFrame) {
            setTimeout(captureNext, 30);
            return;
        }

        isCapturingFrame = true;
        const startTime = Date.now();

        try {
            const screencap = spawn('adb', ['exec-out', 'screencap', '-p']);
            const chunks = [];

            screencap.stdout.on('data', chunk => chunks.push(chunk));
            
            screencap.on('close', (code) => {
                isCapturingFrame = false;
                if (code === 0 && chunks.length > 0) {
                    const buffer = Buffer.concat(chunks);
                    const base64 = buffer.toString('base64');
                    const message = JSON.stringify({
                        type: 'frame',
                        image: `data:image/png;base64,${base64}`,
                        timestamp: Date.now(),
                        latencyMs: Date.now() - startTime
                    });

                    for (const client of screenStreamClients) {
                        if (client.readyState === WebSocket.OPEN) {
                            client.send(message);
                        }
                    }
                }
                setTimeout(captureNext, 40); // Target ~20-25 FPS smooth mirror
            });

            screencap.on('error', () => {
                isCapturingFrame = false;
                setTimeout(captureNext, 200);
            });
        } catch (e) {
            isCapturingFrame = false;
            setTimeout(captureNext, 200);
        }
    }

    captureNext();
}

function startLogcatStreamIfNeeded() {
    if (activeLogcatProcess) return;

    activeLogcatProcess = spawn('adb', ['logcat', '-v', 'time']);

    activeLogcatProcess.stdout.on('data', (data) => {
        const text = data.toString();
        const lines = text.split('\n').filter(l => l.trim().length > 0);

        for (const line of lines) {
            const message = JSON.stringify({
                type: 'log',
                raw: line,
                timestamp: Date.now()
            });

            for (const client of logcatClients) {
                if (client.readyState === WebSocket.OPEN) {
                    client.send(message);
                }
            }
        }
    });

    activeLogcatProcess.on('exit', () => {
        activeLogcatProcess = null;
    });
}

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`🚀 Liquid Web ADB Studio running on http://localhost:${PORT}`);
});
