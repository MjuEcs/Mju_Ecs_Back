package org.mjuecs.mjuecs;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TerminalOutputCallback extends ResultCallback.Adapter<Frame> {

    private final WebSocketSession session;

    public TerminalOutputCallback(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public void onNext(Frame frame) {
        try {
            byte[] payload = frame.getPayload();
            String rawOutput = new String(payload, StandardCharsets.UTF_8);

            // ANSI 이스케이프 시퀀스 제거
            String cleaned = rawOutput.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");

            // 디버깅 로그
            System.out.println("📤 [cleaned] 출력: [" + cleaned + "]");

            session.sendMessage(new TextMessage(cleaned)); // 클린 출력만 전송
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onError(Throwable throwable) {
        System.err.println("Docker exec 오류: " + throwable.getMessage());
    }

    @Override
    public void onComplete() {
        System.out.println("Docker exec 종료");
    }
}
