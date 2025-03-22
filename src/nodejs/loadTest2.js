const WebSocket = require('ws');

const args = process.argv.slice(2);
const CLIENT_COUNT = parseInt(args[0], 10) || 10000;
const SERVER_URL = "ws://localhost:8080/ws";

let connected = 0;
let disconnected = 0;
let errorCount = 0;
let totalLatency = 0;
let latencySamples = 0;

const clients = [];

for (let i = 0; i < CLIENT_COUNT; i++) {
    setTimeout(() => {
        const socket = new WebSocket(SERVER_URL);

        socket.on('open', () => {
            connected++;

            // Ping-like 타임스탬프 전송
            const now = Date.now();
            socket.send("PING:" + now);
            // 5초마다 PING 전송
            setInterval(() => {
                const now = Date.now();
                socket.send("PING:" + now);
            }, 5000);
        });

        socket.on('message', (msg) => {
            if (msg.toString().startsWith("PONG:")) {
                const sentTime = parseInt(msg.toString().split(":")[1]);
                const latency = Date.now() - sentTime;
                totalLatency += latency;
                latencySamples++;
            }
        });

        socket.on('close', () => {
            disconnected++;
        });

        socket.on('error', (err) => {
            errorCount++;
        });

        clients.push(socket);
    }, i * 2); // 2ms 간격으로 분산 연결
}

// 3초마다 상태 출력
setInterval(() => {
    const avgLatency = latencySamples > 0 ? (totalLatency / latencySamples).toFixed(2) : "-";
    console.log(`[STATUS] Connected: ${connected}/${CLIENT_COUNT}, Disconnected: ${disconnected}, Errors: ${errorCount}, Avg Latency: ${avgLatency}ms`);

    // 리셋
    totalLatency = 0;
    latencySamples = 0;
}, 5000);

