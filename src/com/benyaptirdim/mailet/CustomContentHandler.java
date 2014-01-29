package com.benyaptirdim.mailet;

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.message.SimpleContentHandler;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.util.io.IOUtil;

public class CustomContentHandler extends SimpleContentHandler {


    public void body(BodyDescriptor bd, InputStream is)
            throws MimeException, IOException {
        System.out.println("Body detected, contents = "
                + IOUtil.toString(is) + ", header data = " + bd);
    }
    public void field(String fieldData) throws MimeException {
        System.out.println("Header field detected: "
            + fieldData);
    }
    public void startMultipart(BodyDescriptor bd) throws MimeException {
        System.out.println("Multipart message detexted, header data = "
            + bd +"\n bd.getBoundary() : "+bd.getBoundary());
        
    }
    @Override
    public void headers(Header arg0) {
        // TODO Auto-generated method stub
        System.out.println("header " + arg0);
    }
}
