import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Created by WES on 2017-06-11.
 */
public class Util {
    @NotNull
    public static String bufferByteToString(ByteBuffer buf) {
        return bufferByteToString(buf, buf.position());
    }

    @NotNull
    public static String bufferByteToString(ByteBuffer buf, int n) {
        byte[] bytes = buf.array();
        String str = new String(bytes, 0, n, StandardCharsets.UTF_8);
        return str;
    }
}
