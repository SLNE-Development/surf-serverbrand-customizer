package dev.slne.surf.serverbrandcustomizer.buf;


import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

/**
 * The code in this class is a modified version of the instance methods in
 * {@link com.github.retrooper.packetevents.wrapper.PacketWrapper} from the PacketEvents library to
 * write strings and varints to a {@link ByteBuf}.
 *
 * @see <a
 * href="https://github.com/retrooper/packetevents/blob/478a3e56c3a6e812dfd0d61bec025f3934265135/api/src/main/java/com/github/retrooper/packetevents/wrapper/PacketWrapper.java#L664-L683">Write
 * Utf-8 string</a>
 * @see <a
 * href="https://github.com/retrooper/packetevents/blob/478a3e56c3a6e812dfd0d61bec025f3934265135/api/src/main/java/com/github/retrooper/packetevents/wrapper/PacketWrapper.java#L404-L427">Write
 * VarInt</a>
 */
public final class Utf8String {

  private Utf8String() {
    throw new UnsupportedOperationException("This class cannot be instantiated");
  }

  public static void writeString(ByteBuf buf, String string) {
    writeString(buf, string, Short.MAX_VALUE);
  }

  public static void writeString(ByteBuf buf, @NotNull String string, int maxLength) {
    byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > maxLength) {
      throw new EncoderException(
          "String too big (was " + bytes.length + " bytes encoded, max " + maxLength + ")");
    } else {
      writeVarInt(buf, bytes.length);
      buf.writeBytes(bytes);
    }
  }

  public static void writeVarInt(ByteBuf buf, int value) {
    /* Got this code/optimization from https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
     * Copyright and permission notice above (above the class).
     * Steinborn's post says that the code is under the MIT, last accessed 29.06.2024.
     */
    if ((value & (0xFFFFFFFF << 7)) == 0) {
      buf.writeByte(value);
    } else if ((value & (0xFFFFFFFF << 14)) == 0) {
      int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
      buf.writeShort(w);
    } else if ((value & (0xFFFFFFFF << 21)) == 0) {
      int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
      buf.writeMedium(w);
    } else if ((value & (0xFFFFFFFF << 28)) == 0) {
      int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
              | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
      buf.writeInt(w);
    } else {
      int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
              | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
      buf.writeInt(w);
      buf.writeByte(value >>> 28);
    }
  }
}
