package org.vpns.proxy.core;
import java.nio.ByteBuffer;
import java.util.Locale;


public class HttpHostHeaderParser {
	public static final int HTTP=0;
	public static final int HTTPS=1;
	public static final int UNKOWN=3;
	public static final int SSL=4;
	public static int parse(byte data) {
        switch (data) {
                case 'G'://GET
                case 'H'://HEAD
                case 'P'://POST,PUT
                case 'D'://DELETE
                case 'O'://OPTIONS
                case 'T'://TRACE
                return HTTP;
				case 'C'://CONNECT
				return HTTPS;
                case 0x16://SSL 
				return SSL;
				
            }
        return UNKOWN;
    }
    public static String parseHost(byte[] buffer, int offset, int count) {
        try {
            switch (buffer[offset]) {
                case 'G'://GET
                case 'H'://HEAD
                case 'P'://POST,PUT
                case 'D'://DELETE
                case 'O'://OPTIONS
                case 'T'://TRACE
                case 'C'://CONNECT
                    return getHttpHost(buffer, offset, count);
                case 0x16://SSL
                    return getSNI(buffer, offset, count);
            }
        } catch (Exception e) {
            }
        return null;
    }

    static String getHttpHost(byte[] buffer, int offset, int count) {
        String headerString = new String(buffer, offset, count);
        String[] headerLines = headerString.split("\\r\\n");
        String requestLine = headerLines[0];
        if (requestLine.startsWith("GET") || requestLine.startsWith("POST") || requestLine.startsWith("HEAD") || requestLine.startsWith("OPTIONS")) {
            for (int i = 1; i < headerLines.length; i++) {
                String[] nameValueStrings = headerLines[i].split(":");
                if (nameValueStrings.length == 2) {
                    String name = nameValueStrings[0].toLowerCase(Locale.ENGLISH).trim();
                    String value = nameValueStrings[1].trim();
                    if ("host".equals(name)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    static String getSNI(byte[] buffer, int offset, int count) {
        int limit = offset + count;
        if (count > 43 && buffer[offset] == 0x16) {//TLS Client Hello
            offset += 43;//skip 43 bytes header

            //read sessionID:
            if (offset + 1 > limit) return null;
            int sessionIDLength = buffer[offset++] & 0xFF;
            offset += sessionIDLength;

            //read cipher suites:
            if (offset + 2 > limit) return null;
            int cipherSuitesLength = ByteBuffer.wrap(buffer, offset,offset+2).asShortBuffer().get()& 0xFFFF;
            offset += 2;
            offset += cipherSuitesLength;

            //read Compression method:
            if (offset + 1 > limit) return null;
            int compressionMethodLength = buffer[offset++] & 0xFF;
            offset += compressionMethodLength;

            if (offset == limit) {
                System.err.println("TLS Client Hello packet doesn't contains SNI info.(offset == limit)");
                return null;
            }

            //read Extensions:
            if (offset + 2 > limit) return null;
            int extensionsLength = ByteBuffer.wrap(buffer, offset,offset+2).asShortBuffer().get() & 0xFFFF;
            offset += 2;

            if (offset + extensionsLength > limit) {
                System.err.println("TLS Client Hello packet is incomplete.");
                return null;
            }

            while (offset + 4 <= limit) {
                int type0 = buffer[offset++] & 0xFF;
                int type1 = buffer[offset++] & 0xFF;
                int length = ByteBuffer.wrap(buffer, offset,offset+2).asShortBuffer().get() & 0xFFFF;
                offset += 2;

                if (type0 == 0x00 && type1 == 0x00 && length > 5) { //have SNI
                    offset += 5;//skip SNI header.
                    length -= 5;//SNI size;
                    if (offset + length > limit) return null;
                    String serverName = new String(buffer, offset, length);
                    return serverName;
                } else {
                    offset += length;
                }
            }

            System.err.println("TLS Client Hello packet doesn't contains Host field info.");
            return null;
        } else {
            System.err.println("Bad TLS Client Hello packet.");
            return null;
        }
    }
}
