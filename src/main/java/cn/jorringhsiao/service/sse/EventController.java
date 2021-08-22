package cn.jorringhsiao.service.sse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author Jorring Hsiao
 * @date 2021/8/21
 */
@Controller
@RequestMapping("sse")
public class EventController {

    private static final List<String> EVENT_NAMES = Arrays.asList("", "default", "success", "error");

    @Autowired
    private TaskExecutor taskExecutor;

    /**
     * [异步]标准SSE
     * @return SseEmitter
     */
    @GetMapping("standard")
    public SseEmitter sseEmitter() {
        SseEmitter emitter = new SseEmitter();
        taskExecutor.execute(() -> {
            try {
                // 模拟处理过程很长的任务，每完成一个step，输出一个信息给前端
                for (int i = 0; i < 20; i++) {
                    // If you need to specify an event name
    //                SseEmitter.SseEventBuilder builder = SseEmitter.event()
    //                        .name("")
    //                        // more field
    //                        .data(Collections.singletonMap(String.valueOf(i), new Date()));
    //                emitter.send(builder);
                    try {
                        // 这随便输出个时间就算啦
                        Map<String, Object> data = Collections.singletonMap(String.valueOf(i), new Date());
                        emitter.send(data);
                    } catch (IOException e) {
                        // ignore
                        e.printStackTrace();
                    }
                    sleep(ThreadLocalRandom.current().nextInt(10, 500));
                }
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    /**
     * [异步]自定义SSE（使用 ResponseBodyEmitter 输出响应内容）
     * @return ResponseBodyEmitter
     */
    @GetMapping("custom/responseBodyEmitter")
    public ResponseBodyEmitter responseBodyEmitter() {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter() {
            @Override
            protected void extendResponse(ServerHttpResponse outputMessage) {
                super.extendResponse(outputMessage);
                // 必须要设置请求头，文本类型的就可以，随便写
                outputMessage.getHeaders().setContentType(EventUtils.TEXT_CUSTOM_EVENT_STREAM);
            }
        };
        taskExecutor.execute(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    // 随机事件名
                    String eventName = EVENT_NAMES.get(ThreadLocalRandom.current().nextInt(EVENT_NAMES.size()));
                    Map<String, Object> data = Collections.singletonMap(String.valueOf(i), new Date());
                    byte[] bytes = EventUtils.buildEventFrameBytes(eventName, data);
                    try {
                        emitter.send(bytes);
                    } catch (IOException e) {
                        // ignore
                        e.printStackTrace();
                    }
                    sleep(ThreadLocalRandom.current().nextInt(10, 500));
                }
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    /**
     * [异步]自定义SSE（使用 StreamingResponseBody 输出响应内容）
     * @return StreamingResponseBody
     */
    @GetMapping("custom/streamingResponseBody")
    public StreamingResponseBody streamingResponseBody(HttpServletResponse response) {
        // 一定要先设置响应头噢
        response.setContentType(EventUtils.TEXT_CUSTOM_EVENT_STREAM.toString());
        return outputStream -> {
            runTask(outputStream);
        };
    }

    /**
     * [同步]自定义SSE （像写下载文件的接口一样，直接使用response的OutputStream）
     * @param response HttpServletResponse
     */
    @GetMapping("custom/sync")
    public void sync(HttpServletResponse response) throws IOException {
        // 一定要先设置响应头噢
        response.setContentType(EventUtils.TEXT_CUSTOM_EVENT_STREAM.toString());
        try (OutputStream outputStream = response.getOutputStream()) {
            runTask(outputStream);
        }
    }

    private static void runTask(OutputStream outputStream) throws IOException {
        for (int i = 0; i < 20; i++) {
            String eventName = EVENT_NAMES.get(ThreadLocalRandom.current().nextInt(EVENT_NAMES.size()));
            Map<String, Object> data = Collections.singletonMap(String.valueOf(i), new Date());
            byte[] bytes = EventUtils.buildEventFrameBytes(eventName, data);
            outputStream.write(bytes);
            // ==================== 看这里！看这里！看这里！ ====================
            // 重要的事情说三遍：必须刷新。必须刷新。必须刷新。
            // 重要的事情说三遍：必须刷新。必须刷新。必须刷新。
            // 重要的事情说三遍：必须刷新。必须刷新。必须刷新。
            outputStream.flush();
            // 重要的事情说三遍：必须刷新。必须刷新。必须刷新。
            // 重要的事情说三遍：必须刷新。必须刷新。必须刷新。
            // 重要的事情说三遍：必须刷新。必须刷新。必须刷新。
            // ==================== 看这里！看这里！看这里！ ====================
            sleep(ThreadLocalRandom.current().nextInt(10, 500));
        }
    }

    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
