package cn.jorringhsiao.service.sse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author Jorring Hsiao
 * @date 2021/8/22
 */
@Controller
@RequestMapping("demo")
public class DemoController {

    @Autowired
    private TaskExecutor taskExecutor;

    @GetMapping("numProgress")
    public ResponseBodyEmitter numProgress(@RequestParam int count, @RequestParam int delayOrigin, @RequestParam int delayBound) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter() {
            @Override
            protected void extendResponse(ServerHttpResponse outputMessage) {
                super.extendResponse(outputMessage);
                outputMessage.getHeaders().setContentType(EventUtils.TEXT_CUSTOM_EVENT_STREAM);
            }
        };
        taskExecutor.execute(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    Map<String, Object> data = Collections.singletonMap("message", "已完成步骤[" + i + "]");
                    byte[] bytes = EventUtils.buildEventFrameBytes("default", data);
                    emitter.send(bytes);
                    int delay = ThreadLocalRandom.current().nextInt(delayOrigin, delayBound);
                    sleep(delay);
                }
            } catch (IOException e) {
                // ignore
                e.printStackTrace();
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
