const http = require('http');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const PORT = 3000;
const APK_PATH = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');

let isBuilding = false;
let lastBuildLog = 'Build ready.';

const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DriveCare - Native Android Vehicle Management</title>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #0f172a;
      --card-bg: #1e293b;
      --card-border: #334155;
      --primary: #38bdf8;
      --primary-hover: #0284c7;
      --text: #f8fafc;
      --text-muted: #94a3b8;
      --accent-green: #34d399;
      --accent-orange: #fb923c;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
      background-color: var(--bg);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    header {
      background: rgba(30, 41, 59, 0.8);
      backdrop-filter: blur(12px);
      border-bottom: 1px solid var(--card-border);
      padding: 1rem 2rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      position: sticky;
      top: 0;
      z-index: 50;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      font-size: 1.25rem;
      font-weight: 800;
      color: var(--text);
      text-decoration: none;
    }
    .logo-icon {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #38bdf8, #818cf8);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.2rem;
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .btn {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.6rem 1.2rem;
      border-radius: 8px;
      font-weight: 600;
      font-size: 0.9rem;
      cursor: pointer;
      text-decoration: none;
      transition: all 0.2s ease;
      border: none;
    }
    .btn-primary {
      background: var(--primary);
      color: #0f172a;
    }
    .btn-primary:hover {
      background: var(--primary-hover);
      transform: translateY(-1px);
    }
    .btn-outline {
      background: transparent;
      color: var(--text);
      border: 1px solid var(--card-border);
    }
    .btn-outline:hover {
      background: rgba(255,255,255,0.05);
      border-color: var(--text-muted);
    }
    main {
      flex: 1;
      max-width: 1280px;
      margin: 0 auto;
      width: 100%;
      padding: 2rem;
      display: grid;
      grid-template-columns: 1fr 380px;
      gap: 2rem;
    }
    @media (max-width: 1024px) {
      main { grid-template-columns: 1fr; }
    }
    .hero {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 2rem;
      margin-bottom: 2rem;
    }
    .hero h1 {
      font-size: 1.8rem;
      font-weight: 800;
      margin-bottom: 0.5rem;
      background: linear-gradient(to right, #38bdf8, #818cf8);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .hero p {
      color: var(--text-muted);
      line-height: 1.6;
      margin-bottom: 1.5rem;
    }
    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.35rem 0.8rem;
      border-radius: 20px;
      font-size: 0.8rem;
      font-weight: 600;
      background: rgba(52, 211, 153, 0.15);
      color: var(--accent-green);
      border: 1px solid rgba(52, 211, 153, 0.3);
    }
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--accent-green);
      animation: pulse 2s infinite;
    }
    @keyframes pulse {
      0% { opacity: 1; }
      50% { opacity: 0.4; }
      100% { opacity: 1; }
    }
    .feature-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1.25rem;
    }
    .feature-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 1.25rem;
      transition: transform 0.2s;
    }
    .feature-card:hover {
      transform: translateY(-2px);
      border-color: rgba(56, 189, 248, 0.4);
    }
    .feature-icon {
      font-size: 1.5rem;
      margin-bottom: 0.75rem;
    }
    .feature-title {
      font-weight: 700;
      margin-bottom: 0.35rem;
      font-size: 1rem;
    }
    .feature-desc {
      font-size: 0.85rem;
      color: var(--text-muted);
      line-height: 1.5;
    }
    /* Phone frame simulator */
    .phone-container {
      position: sticky;
      top: 5.5rem;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .phone-frame {
      width: 100%;
      max-width: 360px;
      height: 680px;
      background: #000;
      border-radius: 40px;
      border: 12px solid #334155;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
      position: relative;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
    .phone-notch {
      width: 120px;
      height: 20px;
      background: #334155;
      position: absolute;
      top: 0;
      left: 50%;
      transform: translateX(-50%);
      border-bottom-left-radius: 12px;
      border-bottom-right-radius: 12px;
      z-index: 10;
    }
    .phone-screen {
      flex: 1;
      background: #0f172a;
      overflow-y: auto;
      padding-top: 24px;
      display: flex;
      flex-direction: column;
    }
    .app-header {
      background: #1e293b;
      padding: 1rem;
      border-bottom: 1px solid #334155;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .app-title {
      font-weight: 700;
      font-size: 1.1rem;
      color: #38bdf8;
    }
    .app-content {
      padding: 1rem;
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .mock-card {
      background: #1e293b;
      border: 1px solid #334155;
      border-radius: 10px;
      padding: 0.85rem;
    }
    .mock-title {
      font-size: 0.85rem;
      font-weight: 700;
      margin-bottom: 0.25rem;
    }
    .mock-sub {
      font-size: 0.75rem;
      color: #94a3b8;
    }
    .nav-bar {
      background: #1e293b;
      border-top: 1px solid #334155;
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      padding: 0.5rem 0;
      text-align: center;
    }
    .nav-item {
      font-size: 0.65rem;
      color: #94a3b8;
      cursor: pointer;
      padding: 0.25rem;
    }
    .nav-item.active {
      color: #38bdf8;
      font-weight: 700;
    }
    footer {
      border-top: 1px solid var(--card-border);
      padding: 1.5rem;
      text-align: center;
      color: var(--text-muted);
      font-size: 0.85rem;
      margin-top: auto;
    }
  </style>
</head>
<body>

  <header>
    <a href="#" class="logo">
      <div class="logo-icon">🚗</div>
      <span>DriveCare Android</span>
    </a>
    <div class="header-actions">
      <a href="/download/apk" class="btn btn-primary" id="downloadBtn">
        <span>⬇️</span> Download APK
      </a>
      <button onclick="rebuildApk()" class="btn btn-outline" id="rebuildBtn">
        <span>⚡</span> Rebuild Project
      </button>
    </div>
  </header>

  <main>
    <div>
      <div class="hero">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem;">
          <span class="status-badge">
            <span class="status-dot"></span> Kotlin Jetpack Compose Built
          </span>
          <span style="font-size: 0.85rem; color: var(--text-muted);" id="apkInfo">Checking APK...</span>
        </div>
        <h1>Complete Vehicle Management Suite</h1>
        <p>DriveCare is a full-featured native Android application providing intelligent maintenance tracking, document storage with expiry reminders, fuel efficiency logging, emergency assistance, and smart telemetry diagnostic tools.</p>
        <div style="display: flex; gap: 1rem; flex-wrap: wrap;">
          <a href="/download/apk" class="btn btn-primary">Download app-debug.apk</a>
          <button onclick="checkStatus()" class="btn btn-outline">Refresh Status</button>
        </div>
      </div>

      <h2 style="font-size: 1.25rem; font-weight: 700; margin-bottom: 1rem;">Core Android Application Modules</h2>
      
      <div class="feature-grid">
        <div class="feature-card">
          <div class="feature-icon">🚘</div>
          <div class="feature-title">Vehicle Management</div>
          <div class="feature-desc">Multi-vehicle support with detailed specifications, odometer records, license plate tracking, and insurance details.</div>
        </div>

        <div class="feature-card">
          <div class="feature-icon">🛠️</div>
          <div class="feature-title">Maintenance & Service</div>
          <div class="feature-desc">Log services, workshop history, cost breakdowns, timeline history, and photo receipt capture.</div>
        </div>

        <div class="feature-card">
          <div class="feature-icon">📄</div>
          <div class="feature-title">Document Safe & Expiry</div>
          <div class="feature-desc">Store driving licenses, registrations, and insurance policies with automated Android push notification alerts.</div>
        </div>

        <div class="feature-card">
          <div class="feature-icon">⛽</div>
          <div class="feature-title">Fuel & Efficiency Logs</div>
          <div class="feature-desc">Track fill-ups, distance, liters, fuel cost per km, and efficiency trends with interactive metrics.</div>
        </div>

        <div class="feature-card">
          <div class="feature-icon">🔔</div>
          <div class="feature-title">Smart Reminder System</div>
          <div class="feature-desc">Integrated AlarmManager and BroadcastReceiver for reliable scheduled notifications for services and renewals.</div>
        </div>

        <div class="feature-card">
          <div class="feature-icon">🆘</div>
          <div class="feature-title">Emergency Contacts & OBD</div>
          <div class="feature-desc">Instant roadside assistance contacts, nearby workshop lookup, and vehicle health telemetry simulator.</div>
        </div>
      </div>
    </div>

    <!-- Phone Simulator -->
    <div class="phone-container">
      <div class="phone-frame">
        <div class="phone-notch"></div>
        <div class="phone-screen">
          <div class="app-header">
            <span class="app-title">DriveCare</span>
            <span style="font-size: 0.8rem; background: rgba(56,189,248,0.2); color: #38bdf8; padding: 2px 6px; border-radius: 4px;">Android</span>
          </div>

          <div class="app-content" id="phoneContent">
            <!-- Dynamic screen content injected via JS -->
          </div>

          <div class="nav-bar">
            <div class="nav-item active" onclick="switchTab('vehicles', this)">🚗<br>Vehicles</div>
            <div class="nav-item" onclick="switchTab('service', this)">🛠️<br>Service</div>
            <div class="nav-item" onclick="switchTab('documents', this)">📄<br>Docs</div>
            <div class="nav-item" onclick="switchTab('fuel', this)">⛽<br>Fuel</div>
            <div class="nav-item" onclick="switchTab('more', this)">⚙️<br>More</div>
          </div>
        </div>
      </div>
      <p style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.75rem;">Interactive Android App Preview</p>
    </div>
  </main>

  <footer>
    <p>DriveCare Native Android App &bull; Powered by Kotlin, Jetpack Compose, Room DB & Android SDK</p>
  </footer>

  <script>
    const mockData = {
      vehicles: [
        { name: "Toyota Camry", plate: "ABC-1234", mileage: "45,200 km", fuel: "7.2 L/100km", status: "Good" },
        { name: "Honda CR-V", plate: "XYZ-9876", mileage: "28,100 km", fuel: "8.1 L/100km", status: "Service Due Soon" }
      ],
      services: [
        { title: "Synthetic Oil Change", date: "2026-06-15", cost: "$85.00", shop: "Apex Auto Care" },
        { title: "Brake Pad Replacement", date: "2026-05-10", cost: "$210.00", shop: "City Garage" }
      ],
      docs: [
        { name: "Vehicle Registration", expiry: "2027-12-31", status: "VALID" },
        { name: "Insurance Policy", expiry: "2026-08-15", status: "EXPIRING SOON" }
      ]
    };

    function switchTab(tab, el) {
      document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
      if (el) {
        el.classList.add('active');
      } else {
        const navItems = document.querySelectorAll('.nav-item');
        const tabIndex = ['vehicles', 'service', 'documents', 'fuel', 'more'].indexOf(tab);
        if (tabIndex >= 0 && navItems[tabIndex]) {
          navItems[tabIndex].classList.add('active');
        }
      }

      const content = document.getElementById('phoneContent');
      if (tab === 'vehicles') {
        content.innerHTML = \`
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 700; font-size: 0.9rem;">My Vehicles (\${mockData.vehicles.length})</span>
            <span style="font-size: 0.75rem; color: #38bdf8;">+ Add</span>
          </div>
          \${mockData.vehicles.map(v => \`
            <div class="mock-card">
              <div style="display: flex; justify-content: space-between;">
                <div class="mock-title">\${v.name}</div>
                <span style="font-size: 0.7rem; color: \${v.status.includes('Due') ? '#fb923c' : '#34d399'}; font-weight: 600;">\${v.status}</span>
              </div>
              <div class="mock-sub">\${v.plate} &bull; \${v.mileage}</div>
              <div class="mock-sub" style="margin-top: 4px; color: #38bdf8;">Avg Fuel: \${v.fuel}</div>
            </div>
          \`).join('')}
        \`;
      } else if (tab === 'service') {
        content.innerHTML = \`
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 700; font-size: 0.9rem;">Maintenance Log</span>
            <span style="font-size: 0.75rem; color: #38bdf8;">+ Log</span>
          </div>
          \${mockData.services.map(s => \`
            <div class="mock-card">
              <div style="display: flex; justify-content: space-between;">
                <div class="mock-title">\${s.title}</div>
                <div style="font-weight: 700; color: #38bdf8;">\${s.cost}</div>
              </div>
              <div class="mock-sub">\${s.shop} &bull; \${s.date}</div>
            </div>
          \`).join('')}
        \`;
      } else if (tab === 'documents') {
        content.innerHTML = \`
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 700; font-size: 0.9rem;">Document Safe</span>
            <span style="font-size: 0.75rem; color: #38bdf8;">+ Upload</span>
          </div>
          \${mockData.docs.map(d => \`
            <div class="mock-card">
              <div style="display: flex; justify-content: space-between;">
                <div class="mock-title">\${d.name}</div>
                <span style="font-size: 0.65rem; background: \${d.status.includes('SOON') ? 'rgba(251,146,60,0.2)' : 'rgba(52,211,153,0.2)'}; color: \${d.status.includes('SOON') ? '#fb923c' : '#34d399'}; padding: 2px 4px; border-radius: 4px;">\${d.status}</span>
              </div>
              <div class="mock-sub">Expires: \${d.expiry}</div>
            </div>
          \`).join('')}
        \`;
      } else if (tab === 'fuel') {
        content.innerHTML = \`
          <div style="font-weight: 700; font-size: 0.9rem;">Fuel Tracker</div>
          <div class="mock-card" style="background: rgba(56,189,248,0.1); border-color: rgba(56,189,248,0.3);">
            <div class="mock-sub">Average Fuel Economy</div>
            <div style="font-size: 1.4rem; font-weight: 800; color: #38bdf8;">7.65 L / 100km</div>
          </div>
          <div class="mock-card">
            <div class="mock-title">Shell Station</div>
            <div class="mock-sub">42.5 Liters &bull; $68.00</div>
          </div>
        \`;
      } else {
        content.innerHTML = \`
          <div style="font-weight: 700; font-size: 0.9rem;">Settings & Utilities</div>
          <div class="mock-card"><div class="mock-title">🆘 Emergency Assistance</div></div>
          <div class="mock-card"><div class="mock-title">📊 OBD Diagnostic Telemetry</div></div>
          <div class="mock-card"><div class="mock-title">🌐 App Language & Units</div></div>
        \`;
      }
    }

    async function checkStatus() {
      try {
        const res = await fetch('/api/status');
        const data = await res.json();
        const apkInfo = document.getElementById('apkInfo');
        if (data.apkExists) {
          apkInfo.textContent = \`APK Ready (\${(data.apkSize / (1024 * 1024)).toFixed(2)} MB)\`;
        } else {
          apkInfo.textContent = 'APK not yet built';
        }
      } catch (e) {
        console.error(e);
      }
    }

    async function rebuildApk() {
      const btn = document.getElementById('rebuildBtn');
      btn.disabled = true;
      btn.textContent = 'Building Gradle APK...';
      try {
        const res = await fetch('/api/rebuild', { method: 'POST' });
        const data = await res.json();
        alert(data.message || 'Build started');
      } catch (e) {
        alert('Build error');
      } finally {
        btn.disabled = false;
        btn.textContent = '⚡ Rebuild Project';
        checkStatus();
      }
    }

    // Initialize screen
    switchTab('vehicles');
    checkStatus();
  </script>
</body>
</html>
`;

const server = http.createServer((req, res) => {
  const url = req.url;

  if (url === '/' || url === '/index.html') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(htmlContent);
  } else if (url === '/download/apk') {
    if (fs.existsSync(APK_PATH)) {
      const stat = fs.statSync(APK_PATH);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="DriveCare-debug.apk"'
      });
      fs.createReadStream(APK_PATH).pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('APK file not built yet. Please run build step.');
    }
  } else if (url === '/api/status') {
    const apkExists = fs.existsSync(APK_PATH);
    const apkSize = apkExists ? fs.statSync(APK_PATH).size : 0;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'running',
      appName: 'DriveCare',
      platform: 'Android (Kotlin / Jetpack Compose)',
      apkExists,
      apkSize,
      isBuilding,
      lastBuildLog
    }));
  } else if (url === '/api/rebuild' && (req.method === 'POST' || req.method === 'GET')) {
    if (isBuilding) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ message: 'Build already in progress...' }));
      return;
    }
    isBuilding = true;
    exec('./gradlew assembleDebug', { cwd: __dirname }, (error, stdout, stderr) => {
      isBuilding = false;
      if (error) {
        lastBuildLog = stderr || error.message;
        console.error('Gradle build failed:', error);
      } else {
        lastBuildLog = 'Build completed successfully.';
      }
    });
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ message: 'Gradle assembleDebug triggered in background.' }));
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('404 Not Found');
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`DriveCare Web Preview Server listening on http://0.0.0.0:${PORT}`);
});
