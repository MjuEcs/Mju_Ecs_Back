package org.mjuecs.mjuecs.component;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.mjuecs.mjuecs.DockerClientFactory;
import org.mjuecs.mjuecs.TerminalOutputCallback;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private final DockerClient dockerClient;
    private final Map<String, OutputStream> stdinMap = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> inputBuffer = new ConcurrentHashMap<>();


    public TerminalWebSocketHandler() {
        this.dockerClient = DockerClientFactory.createClient();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String containerId = getContainerId(session);

        // 1. 입력 스트림 생성
        PipedOutputStream stdinWriter = new PipedOutputStream();
        PipedInputStream stdinReader = new PipedInputStream(stdinWriter);
        stdinMap.put(session.getId(), stdinWriter);

        // 2. docker exec 명령 생성
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withTty(true)
                .withCmd("bash","-i")  // 또는 bash
                .exec();

        // 3. 결과 콜백 생성

        TerminalOutputCallback callback = new TerminalOutputCallback(session);

        // 4. exec 실행
        dockerClient.execStartCmd(exec.getId())
                .withDetach(false)
                .withTty(true)
                .withStdIn(stdinReader)
                .exec(new TerminalOutputCallback(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        OutputStream stdin = stdinMap.get(session.getId());
        if (stdin == null) return;

        String sessionId = session.getId();
        String input = message.getPayload();

        inputBuffer.putIfAbsent(sessionId, new StringBuilder());
        StringBuilder buffer = inputBuffer.get(sessionId);

        if ("\r".equals(input)) {
            // 사용자 엔터 입력 → 줄 완성 → 한 줄 보내기
            buffer.append("\n"); // 실제 쉘 명령어 종료
            stdin.write(buffer.toString().getBytes(StandardCharsets.UTF_8));
            stdin.flush();
            buffer.setLength(0); // 버퍼 초기화
        } else {
            buffer.append(input);
        }

        // 로그 확인
        System.out.println("🧾 현재 입력 버퍼: " + buffer.toString());
    }





    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        OutputStream stdin = stdinMap.remove(session.getId());
        if (stdin != null) {
            stdin.close();
        }
    }

    private String getContainerId(WebSocketSession session) {
        return session.getUri().getPath().split("/ws/terminal/")[1];
    }
}
