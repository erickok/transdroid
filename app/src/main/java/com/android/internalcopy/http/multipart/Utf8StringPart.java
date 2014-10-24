package com.android.internalcopy.http.multipart;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.util.EncodingUtils;

public class Utf8StringPart extends PartBase {

    /** Contents of this StringPart. */
    private byte[] content;
    
    /** The String value of this part. */
    private String value;
	
	public Utf8StringPart(String name, String value) {
		super(name, null, null, null);
		this.value = value;
	}

    /**
     * Gets the content in bytes.  Bytes are lazily created to allow the charset to be changed
     * after the part is created.
     * 
     * @return the content in bytes
     */
    private byte[] getContent() {
        if (content == null) {
            content = EncodingUtils.getBytes(value, "utf-8");
        }
        return content;
    }
	
	@Override
	protected void sendData(OutputStream out) throws IOException {
        out.write(getContent());
	}

	@Override
	protected long lengthOfData() throws IOException {
        return getContent().length;
	}
}
