// nodejs 설치 후
// node 파일명 클라이언트수 형태의 명령어로 실행
// node wsLoadTest1 1000

const WebSocket = require('ws');
const { Client } = require('@stomp/stompjs');

// ✅ CLI 인자: node loadTest.js 10000
const args = process.argv.slice(2);
const CLIENT_COUNT = parseInt(args[0], 10) || 10;

const SERVER_URL = "ws://10.205.0.108:8080/ws";
let connected = 0;
const clients = [];

for (let i = 0; i < CLIENT_COUNT; i++) {
    let currentOrder = 0;
    let leftOrder = 0;
    let outReserve = false;

    const client = new Client({
        webSocketFactory: () => new WebSocket(SERVER_URL),
        connectHeaders: {},
        heartbeatIncoming: 5000,
        heartbeatOutgoing: 5000,
        reconnectDelay: 5000,
        debug: () => {},

        onConnect: function () {
            connected++;
            console.log(`[Client ${i}] ✅ 연결됨 (${connected}/${CLIENT_COUNT})`);

            client.subscribe('/user/queue/order', function (message) {
                currentOrder = parseInt(message.body, 10);
                console.log(`[Client ${i}] ▶ 순번: ${currentOrder}번째`);

                if (currentOrder === 1 && !outReserve) {
                    currentOrder--;
                    console.log(`[Client ${i}] 🎉 3초 후 입장 예약`);
                    setTimeout(() => {
                        console.log(`[Client ${i}] ⛔ 입장 완료 (연결 종료)`);
                        outReserve = true;
                        client.forceDisconnect(); // 자동 재연결 유도
                    }, 3000);
                }
            });

            client.subscribe('/topic/waiting', function (message) {
                leftOrder = parseInt(message.body, 10);
                if (currentOrder > 1 && currentOrder > leftOrder) {
                    currentOrder--;
                    console.log(`[Client ${i}] 🔄 순번 감소 → ${currentOrder}번째`);
                    client.publish({
                        destination: "/app/update-order",
                        body: JSON.stringify({ newOrder: currentOrder })
                    });
                }

                if (currentOrder === 1 && !outReserve) {
                    currentOrder--;
                    console.log(`[Client ${i}] 🎉 당신의 순번입니다! 1초 후 입장합니다.`);
                    setTimeout(() => {
                        console.log(`[Client ${i}] ⛔ 입장 완료 (연결 종료)`);
                        outReserve = true;
                        client.forceDisconnect();
                    }, 1000);
                }
            });

            client.publish({
                    "wsLoadTest2.js" 101L, 3478B

                    client.publish({
                        destination: "/app/join",
                        body: ""
                    });
                },

                onStompError: function (frame) {
                console.error(`[Client ${i}] ❗ STOMP 에러:`, frame.headers['message']);
            },

            onWebSocketClose: function () {
                currentOrder = 0;
                connected--;
                outReserve = false;
                const index = clients.indexOf(client);
                if (index > -1) {
                    clients.splice(index, 1);
                    console.log(`[Client ${i}] 🗑️  클라이언트 제거됨 (남은 수: ${clients.length})`);
                }
                console.log(`[Client ${i}] 🔌 연결 끊김 → 순번 초기화됨`);
            }
        });

    // ✅ 순차 연결 (과부하 방지)
    setTimeout(() => {
        client.activate();
    }, i * 2); // 2ms 간격

    clients.push(client);
}

// ✅ 전체 상태 출력
setInterval(() => {
    console.log(`[Status] 연결 수: ${connected}/${CLIENT_COUNT}`);
}, 5000);