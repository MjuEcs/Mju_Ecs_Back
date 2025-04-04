// TerminalProcessBridge.java
package org.mjuecs.mjuecs.component;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TerminalProcessBridge extends TextWebSocketHandler {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, ProcessIO> sessionMap = new ConcurrentHashMap<>();

    private record ProcessIO(Process process, BufferedWriter writer) {}

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket 연결됨: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        executor.submit(() -> {
            try {
                String payload = message.getPayload();
                System.out.println("📥 받은 메시지: " + payload);

                if (payload.trim().startsWith("{")) {
                    String containerId = payload.replaceAll("[^a-zA-Z0-9-_]", "");

                    boolean scriptExists = checkScriptCommand(containerId);
                    ProcessBuilder pb = scriptExists
                            ? new ProcessBuilder("docker", "exec", "-it", containerId, "script", "-q", "-c", "bash", "/dev/null")
                            : new ProcessBuilder("docker", "exec", "-it", containerId, "bash");

                    Process process = pb.start();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    sessionMap.put(session.getId(), new ProcessIO(process, writer));

                    executor.submit(() -> {
                        String line;
                        try {
                            while ((line = reader.readLine()) != null) {
                                if (session.isOpen()) {
                                    session.sendMessage(new TextMessage(line + "\n"));
                                }
                            }
                        } catch (IOException ignored) {}
                    });

                    session.sendMessage(new TextMessage("✅ bash 세션 연결 완료\n"));
                    return;
                }

                ProcessIO processIO = sessionMap.get(session.getId());
                if (processIO != null) {
                    processIO.writer.write(payload);
                    processIO.writer.newLine();
                    processIO.writer.flush();
                } else {
                    session.sendMessage(new TextMessage("❌ containerId 먼저 보내세요\n"));
                }

            } catch (Exception e) {
                try {
                    session.sendMessage(new TextMessage("에러: " + e.getMessage()));
                } catch (IOException ignored) {}
                e.printStackTrace();
            }
        });
    }

    private boolean checkScriptCommand(String containerId) {
        try {
            Process check = new ProcessBuilder("docker", "exec", containerId, "which", "script").start();
            int exitCode = check.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("❌ 'which script' 검사 실패: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        System.out.println("WebSocket 종료됨: " + session.getId());
        ProcessIO io = sessionMap.remove(session.getId());
        if (io != null) {
            io.process().destroy();
        }
    }
}
