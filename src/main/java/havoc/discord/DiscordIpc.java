package havoc.discord;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DiscordIpc {
   private static final int OP_HANDSHAKE = 0;
   private static final int OP_FRAME = 1;
   private final String clientId;
   private RandomAccessFile pipe;

   public DiscordIpc(String clientId) {
      this.clientId = clientId;
   }

   public synchronized boolean connected() {
      return this.pipe != null;
   }

   public synchronized boolean connect() {
      if (this.pipe != null) {
         return true;
      } else {
         for (int i = 0; i < 10; i++) {
            try {
               RandomAccessFile candidate = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
               String handshake = "{\"v\":1,\"client_id\":\"" + this.clientId + "\"}";
               writeFrame(candidate, 0, handshake);
               int opcode = readInt(candidate);
               int length = readInt(candidate);
               byte[] payload = new byte[Math.max(0, length)];
               candidate.readFully(payload);
               if (opcode != 1) {
                  candidate.close();
                  continue;
               }

               this.pipe = candidate;
               return true;
            } catch (Throwable ignored) {
            }
         }

         return false;
      }
   }

   public synchronized void close() {
      if (this.pipe != null) {
         try {
            this.pipe.close();
         } catch (Throwable ignored) {
         }

         this.pipe = null;
      }
   }

   public synchronized boolean setActivity(String activityJson) {
      if (this.pipe == null && !connect()) {
         return false;
      } else {
         long pid = ProcessHandle.current().pid();
         String frame = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + pid + ",\"activity\":" + activityJson + "},\"nonce\":\"" + UUID.randomUUID() + "\"}";

         try {
            writeFrame(this.pipe, 1, frame);
            readInt(this.pipe);
            int length = readInt(this.pipe);
            byte[] payload = new byte[Math.max(0, length)];
            this.pipe.readFully(payload);
            return true;
         } catch (Throwable ignored) {
            close();
            return false;
         }
      }
   }

   private static void writeFrame(RandomAccessFile target, int opcode, String json) throws java.io.IOException {
      byte[] payload = json.getBytes(StandardCharsets.UTF_8);
      target.write(intLE(opcode));
      target.write(intLE(payload.length));
      target.write(payload);
   }

   private static int readInt(RandomAccessFile source) throws java.io.IOException {
      byte[] bytes = new byte[4];
      source.readFully(bytes);
      return bytes[0] & 0xFF | (bytes[1] & 0xFF) << 8 | (bytes[2] & 0xFF) << 16 | (bytes[3] & 0xFF) << 24;
   }

   private static byte[] intLE(int value) {
      return new byte[]{(byte)value, (byte)(value >>> 8), (byte)(value >>> 16), (byte)(value >>> 24)};
   }
}
