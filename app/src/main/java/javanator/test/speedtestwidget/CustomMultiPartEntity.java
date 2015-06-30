package javanator.test.speedtestwidget;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class CustomMultiPartEntity extends MultipartEntity {

    private final ProgressListener listener;

    public CustomMultiPartEntity(final ProgressListener listener) {
        super();
        this.listener = listener;
    }

    public CustomMultiPartEntity(final HttpMultipartMode mode, final ProgressListener listener) {
        super(mode);
        this.listener = listener;
    }

    public CustomMultiPartEntity(HttpMultipartMode mode, final String boundary, final Charset charset, final ProgressListener listener) {
        super(mode, boundary, charset);
        this.listener = listener;
    }

    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        super.writeTo(new CountingOutputStream(outstream, this.listener));
    }

    public interface ProgressListener {
        void transferred(long num, long timeElapsed);
    }

    public static class CountingOutputStream extends FilterOutputStream {

        private final ProgressListener listener;

        private long transferred = 0;

        private long uploadStartTime = 0;

        public CountingOutputStream(final OutputStream out, final ProgressListener listener) {
            super(out);
            this.listener = listener;
            this.transferred = 0;
        }

        public void write(byte[] b, int off, int len) throws IOException {

            if(uploadStartTime == 0) uploadStartTime = System.nanoTime();
            out.write(b, off, len);
            this.transferred += len;
            this.listener.transferred(this.transferred, System.nanoTime() - uploadStartTime);
        }

        public void write(int b) throws IOException {
            if(uploadStartTime == 0) uploadStartTime = System.nanoTime();
            out.write(b);
            this.transferred++;
            this.listener.transferred(this.transferred, System.nanoTime() - uploadStartTime);
        }
    }
}