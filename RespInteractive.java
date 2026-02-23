import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class RespInteractive {
    public static void main(String[] args) throws Exception {
        try (Socket s = new Socket("127.0.0.1", 6379);
             InputStream in = s.getInputStream();
             OutputStream out = s.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Connected. Type commands like: PING | PING hello | SET k v | GET k | exit");
            while (true) {
                System.out.print("> ");
                if (!sc.hasNextLine()) break;
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) break;
                String[] parts = line.split(" ");
                StringBuilder resp = new StringBuilder();
                resp.append("*").append(parts.length).append("\r\n");
                for (String p : parts) {
                    byte[] b = p.getBytes(StandardCharsets.UTF_8);
                    resp.append("$").append(b.length).append("\r\n");
                    resp.append(p).append("\r\n");
                }
                out.write(resp.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();

                // Read reply header
                String header = reader.readLine();
                if (header == null) break;
                System.out.println(header);
                if (header.startsWith("$")) {
                    int len = Integer.parseInt(header.substring(1));
                    if (len == -1) {
                        System.out.println("(nil)");
                    } else {
                        char[] buf = new char[len];
                        int read = 0;
                        while (read < len) {
                            int r = reader.read(buf, read, len - read);
                            if (r == -1) break;
                            read += r;
                        }
                        reader.readLine(); // consume CRLF
                        System.out.println(new String(buf));
                    }
                } else if (header.startsWith("+") || header.startsWith("-") || header.startsWith(":")) {
                    // already printed header
                } else {
                    // arrays or other types: print raw
                    System.out.println("(raw) " + header);
                }
            }
        }
    }
}
