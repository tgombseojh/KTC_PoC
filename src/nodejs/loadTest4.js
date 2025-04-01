const WebSocket = require('ws');

const args = process.argv.slice(2);
const CLIENT_COUNT = parseInt(args[0], 10) || 1000;
//const SERVER_URL = "ws://10.205.0.152:8080/ws";
const SERVER_URL = "ws://127.0.0.1:8080/ws";
//const SERVER_URL = "ws://host.docker.internal:8080/ws";

let connected = 0;
let disconnected = 0;
let errorCount = 0;
let sessions = 0;
let totalLatency = 0;
let latencySamples = 0;

const MAX_RETRY = 5;
const clients = new Map();        // id → socket
const retryTracker = new Map();   // id → retry count

function createClient(id) {
    const socket = new WebSocket(SERVER_URL);
    let currentOrder = 0;

    clients.set(id, socket); // 클라이언트 관리

    socket.on('open', () => {
        connected++;
        retryTracker.set(id, 0); // 성공시 재시도 횟수 초기화

        // PING 전송
        //setInterval(() => {
        //    const now = Date.now();
        //    socket.send("PING:" + now);
        //}, 10000);
    });

    socket.on('message', (data) => {
        const msg = data.toString();

        if (msg === "BUSY") {
            // 서버가 거절한 경우 재시도
            socket.close();
            return;
        }

        if (msg.startsWith("ORDER:")) {
            currentOrder = parseInt(msg.split(":")[1]);
        } else if (msg.startsWith("WAITING:")) {
            const leftOrder = parseInt(msg.split(":")[1]);
            if (currentOrder > leftOrder) {
                currentOrder--;
                socket.send("UPDATE:" + currentOrder);
            }
        } else if (msg.startsWith("SESSIONS:")) {
            const parsed = parseInt(msg.split(":")[1]);
            if (!isNaN(parsed) && parsed > 0) {
                sessions = parsed;
                if (currentOrder > sessions) currentOrder = sessions;
            }
        } else if (msg.startsWith("PONG:")) {
            const sentTime = parseInt(msg.split(":")[1]);
            const latency = Date.now() - sentTime;
            totalLatency += latency;
            latencySamples++;
        }
    });

    socket.on('close', () => {
        disconnected++;
        clients.delete(id);
    });

    socket.on('error', (err) => {
        errorCount++;

        const retry = retryTracker.get(id) || 0;
        if (retry >= MAX_RETRY) {
            console.warn(`[${id}] ❌ 최대 재시도 초과 (${MAX_RETRY})`);
            clients.delete(id);
            return;
        }

        retryTracker.set(id, retry + 1);

        setTimeout(() => {
            createClient(id); // 재연결 시도
        }, 3000);
    });
}

// 클라이언트 생성 시작
for (let i = 0; i < CLIENT_COUNT; i++) {
    setTimeout(() => {
        createClient(i);
    }, i * 1); // 2ms 간격으로 연결
}

// 상태 출력
setInterval(() => {
    const avgLatency = latencySamples > 0 ? (totalLatency / latencySamples).toFixed(2) : "-";
    console.log(`[STATUS] Connected: ${connected}/${CLIENT_COUNT}, TotalClientCount: ${sessions}, Disconnected: ${disconnected}, Errors: ${errorCount}, Avg Latency: ${avgLatency}ms`);
    totalLatency = 0;
    latencySamples = 0;
}, 5000);