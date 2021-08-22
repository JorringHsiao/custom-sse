package cn.jorringhsiao.service.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Jorring Hsiao
 * @date 2021/8/21
 */
public class EventUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final MediaType TEXT_CUSTOM_EVENT_STREAM = new MediaType("text", "custom-event-stream");

    public static final String EVENT_PART_SPLITTER = "\n";

    public static final String EVENT_SPLITTER = "\n\n";

    public static final int EVENT_FIXED_SIZE =
            + EVENT_PART_SPLITTER.length()
                    + EVENT_SPLITTER.length();


    public static byte[] buildEventFrameBytes(String eventName, Map<String, Object> data) {
        return buildEventFrameText(eventName, data).getBytes(StandardCharsets.UTF_8);
    }

    public static String buildEventFrameText(String eventName, Map<String, Object> data) {
        if (eventName == null) {
            eventName = "";
        } else if (eventName.contains(EVENT_PART_SPLITTER)) {
            throw new IllegalArgumentException("event name cannot contain the '\\r\\n'");
        }
        String dataString = CollectionUtils.isEmpty(data) ? "{}" : toString(data);
        // 计算容量 避免浪费内存
        int capacity = EVENT_FIXED_SIZE + eventName.length() + dataString.length();
        return new StringBuilder(capacity)
                // 事件名
                .append(eventName)
                .append(EVENT_PART_SPLITTER)
                // 数据：json字符串，对象类型
                .append(dataString)
                .append(EVENT_SPLITTER)
                .toString();
    }

    private static String toString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

}
