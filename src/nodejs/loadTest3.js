const WebSocket = require('ws');

const args = process.argv.slice(2);
const CLIENT_COUNT = parseInt(args[0], 10) || 1000;
const SERVER_URL = "ws://172.30.1.9:8080/ws";

let connected = 0;
let disconnected = 0;
let errorCount = 0;
let sessions = 0;
let totalLatency = 0;
let latencySamples = 0;

function createClient(id) {
    const socket = new WebSocket(SERVER_URL);

    let currentOrder = 0;

    socket.on('open', () => {
        connected++;
        //console.log(`[${id}] ✅ 연결됨 (총 연결: ${connected})`);

        // 5초마다 PING 전송
        setInterval(() => {
            const now = Date.now();
            socket.send("PING:" + now);
        }, 5000);
    });

    socket.on('message', (data) => {
        const msg = data.toString();

        if (msg.startsWith("ORDER:")) {
            currentOrder = parseInt(msg.split(":")[1]);
            //console.log(`[${id}] 🟢 순번 할당됨 → ${currentOrder}`);
        } else  if (msg.startsWith("WAITING:")) {
            const leftOrder = parseInt(msg.split(":")[1]);
            if (currentOrder > leftOrder) {
                currentOrder--;
                //console.log(`[${id}] ⚠️ 앞 순번 ${leftOrder} 이탈 → 내 순번 보정: ${currentOrder} / 총 대기자: ${sessions}`);
                socket.send("UPDATE:" + currentOrder);
            }
        } else if (msg.startsWith("SESSIONS:")) {
            sessions = parseInt(msg.split(":")[1]);
            //console.log(`[${id}] 📊 현재 대기자 수: ${sessions}, 내 순번: ${currentOrder}`);
        } else if (msg.toString().startsWith("PONG:")) {
            const sentTime = parseInt(msg.toString().split(":")[1]);
            const latency = Date.now() - sentTime;
            totalLatency += latency;
            latencySamples++;
        }
    });

    socket.on('close', () => {
        disconnected++;
        //console.log(`[${id}] ⛔ 연결 종료됨 (남은 연결: ${connected - disconnected})`);
    });

    socket.on('error', (err) => {
        errorCount++;
        console.error(`[${id}] ❌ 오류 발생: ${err.message}`);
    });
}

// 여러 개의 클라이언트 순차적으로 생성
for (let i = 0; i < CLIENT_COUNT; i++) {
    setTimeout(() => {
        createClient(i);
    }, i * 2); // 5ms 간격으로 순차 연결
}

// 상태 출력
setInterval(() => {
    console.log(`[STATUS] Connected: ${connected}/${CLIENT_COUNT}, TotalClientCount: ${sessions}, Disconnected: ${disconnected}, Errors: ${errorCount}`);
}, 5000);

