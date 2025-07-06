package com.example.sportsystem.pushservice.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.util.SocketFactoryUtil;

/**
 * WebSocket 重连客户端
 */
public class WebSocketReconnectClient {

    private static final int MAX_RETRY = 5;
    private static final int RECONNECT_INTERVAL = 5; // 单位：秒

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private int retryCount = 0;
    private final Set<String> subscribedMatchIds = new HashSet<>();

    public WebSocketReconnectClient(URI serverUri) {
        super(serverUri, SocketFactoryUtil.getDefault(), null, new Headers());
    }

    /**
     * 订阅特定赛事
     * @param matchId 赛事ID
     */
    public void subscribeMatch(String matchId) {
        if (this.isOpen()) {
            this.send("{\"type\":\"subscribe\",\"data\":\"" + matchId + "\"}");
            subscribedMatchIds.add(matchId);
        }
    }

    /**
     * 取消订阅
     * @param matchId 赛事ID
     */
    public void unsubscribeMatch(String matchId) {
        if (this.isOpen()) {
            this.send("{\"type\":\"unsubscribe\",\"data\":\"" + matchId + "\"}");
            subscribedMatchIds.remove(matchId);
        }
    }

    /**
     * 恢复所有订阅
     */
    private void restoreSubscriptions() {
        subscribedMatchIds.forEach(this::subscribeMatch);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("[WebSocket] 连接已建立");
        retryCount = 0; // 重置重试次数
        restoreSubscriptions(); // 重连后恢复订阅
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[WebSocket] 收到消息: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WebSocket] 连接关闭: " + reason);
        scheduleReconnect();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[WebSocket] 发生错误: " + ex.getMessage());
        this.close();
    }

    /**
     * 调度重连任务
     */
    private void scheduleReconnect() {
        if (retryCount < MAX_RETRY) {
            int delay = (int) Math.min(RECONNECT_INTERVAL * Math.pow(2, retryCount), 60);
            System.out.println("[WebSocket] 第 " + (retryCount + 1) + " 次重连，将在 " + delay + " 秒后尝试...");
            scheduler.schedule(this::reconnect, delay, TimeUnit.SECONDS);
            retryCount++;
        } else {
            System.err.println("[WebSocket] 达到最大重试次数，停止连接");
        }
    }
}