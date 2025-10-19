package dev.wgrgwg.somniverse.global.idempotency.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ReusableRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;
    private final Charset charset;

    public ReusableRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.body = request.getInputStream().readAllBytes();

        Charset cs = StandardCharsets.UTF_8;
        String enc = request.getCharacterEncoding();
        if (enc != null) {
            try {
                cs = Charset.forName(enc);
            } catch (Exception ignore) {
            }
        }
        this.charset = cs;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return bais.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    @Override
    public int getContentLength() {
        long len = getContentLengthLong();
        if (len > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) len;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    public byte[] getCachedBody() {
        return body;
    }
}
