package org.mjuecs.mjuecs.component;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.mjuecs.mjuecs.DockerClientFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private final DockerClient dockerClient = DockerClientFactory.createClient();

    // 명령어 버퍼 저장용
    private final Map<String, StringBuilder> bufferMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("✅ WebSocket 연결 완료");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String input = message.getPayload();

        // 세션별 버퍼 준비
        bufferMap.putIfAbsent(sessionId, new StringBuilder());
        StringBuilder buffer = bufferMap.get(sessionId);

        if ("\r".equals(input)) {
            // 엔터키 입력 → 명령 실행
            String command = buffer.toString().trim();
            buffer.setLength(0); // 버퍼 초기화

            System.out.println("📥 명령 실행 요청: " + command);
            executeCommand(session, getContainerId(session), command);
        } else {
            buffer.append(input);
            System.out.println("🧾 입력 중: " + buffer);
        }
    }

    private void executeCommand(WebSocketSession session, String containerId, String command) {
        try {
            // exec 생성
            ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(false)
                    .withCmd("bash", "-c", command)
                    .exec();

            StringBuilder output = new StringBuilder();

            // exec 시작
            dockerClient.execStartCmd(exec.getId())
                    .withDetach(false)
                    .withTty(false)
                    .exec(new ExecStartResultCallback() {
                        @Override
                        public void onNext(Frame frame) {
                            String data = new String(frame.getPayload(), StandardCharsets.UTF_8);
                            output.append(data);
                        }

                        @Override
                        public void onComplete() {
                            try {
                                session.sendMessage(new TextMessage(output.toString()));
                                session.sendMessage(new TextMessage("\r\n$ ")); // 프롬프트처럼
                                System.out.println("📤 실행 결과 전송 완료");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            System.err.println("❌ 명령 실행 에러: " + throwable.getMessage());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        bufferMap.remove(session.getId());
        System.out.println("❌ WebSocket 연결 종료");
    }

    private String getContainerId(WebSocketSession session) {
        String[] parts = session.getUri().getPath().split("/ws/terminal/");
        return parts.length > 1 ? parts[1] : "";
    }
}
