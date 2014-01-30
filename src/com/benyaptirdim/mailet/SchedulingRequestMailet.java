package com.benyaptirdim.mailet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.apache.james.util.io.IOUtil;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public class SchedulingRequestMailet extends GenericMailet {
    public void init() throws MessagingException {
    }
//
//    protected String decodeName(String name) throws Exception {
//        log("decode name: " + name);
//        if (name == null || name.length() == 0) {
//            return "unknown";
//        }
//        String ret = java.net.URLDecoder.decode(name, "UTF-8");
//
//        // also check for a few other things in the string:
//        ret = ret.replaceAll("=\\?utf-8\\?q\\?", "");
//        ret = ret.replaceAll("\\?=", "");
//        ret = ret.replaceAll("=20", " ");
//
//        return ret;
//    }

//    protected int saveFile(File saveFile, Part part) throws Exception {
//
//        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(saveFile));
//
//        byte[] buff = new byte[2048];
//        InputStream is = part.getInputStream();
//        int ret = 0, count = 0;
//        while ((ret = is.read(buff)) > 0) {
//            bos.write(buff, 0, ret);
//            count += ret;
//        }
//        bos.close();
//        is.close();
//        return count;
//    }

    public void service(Mail mail) throws MessagingException {
        try {
            // ContentHandler handler = new CustomContentHandler();
            // MimeStreamParser parser = new MimeStreamParser();
            // parser.setContentHandler(handler);
            // parser.parse(mail.getMessage().getInputStream());
            // /*
//            MyHttpParams params = new MyHttpParams();
//            MimeTokenStream stream = new MimeTokenStream();
//            stream.parse(mail.getMessage().getInputStream());
//
//            for (EntityState state = stream.getState(); state != EntityState.T_END_OF_STREAM; state = stream.next()) {
//                switch (state) {
//                    case T_BODY:
//                        params.text += IOUtil.toString(stream.getInputStream());
//                        System.out.println("Body detected, contents = "
//                                + stream.getInputStream() + ", header data = "
//                                + stream.getBodyDescriptor());
//                        break;
//                    case T_FIELD:
//                        System.out.println("Header field detected: "
//                                + stream.getField());
//                        break;
//                    case T_START_MULTIPART:
//                        System.out.println("Multipart message detexted,"
//                                + " header data = "
//                                + stream.getInputStream());
//                        params.addFile(IOUtil.toByteArray(stream.getInputStream()),stream.getBodyDescriptor().getMimeType());
//                }
//            }

//            params.send();
            Collection recipients = mail.getRecipients();
            if (recipients.size() > 1) {
                // TODO:
                return;
            }
            MyHttpParams params = new MyHttpParams();
            MimeMessage message = mail.getMessage();
            Object content = message.getContent();
            if (content instanceof java.lang.String) {
                System.out.println("content instanceof java.lang.String");
                params.text += (String) content;
            } else if (content instanceof Multipart) {
                System.out.println("else if (content instanceof Multipart");
                Multipart mp = (Multipart) content;
                doMultipart(mp,params);
            }
            params.send();

//            log("body:\n " + email.body);
//            log("atc size : " + vema.size());
            MailAddress recipient = (MailAddress) recipients.iterator().next();

            boolean sellerAvailable = recipient.getUser().startsWith("request-available-");

            String requestID = recipient.getUser();
            int pos = requestID.lastIndexOf("-");
            requestID = requestID.substring(pos + 1);

            String sellerAddress = mail.getSender().toString();
            System.out.println("sellerAddress: " + sellerAddress);
            System.out.println("requestID: " + requestID);

            mail.setState(Mail.DEFAULT);
            // mail.setState(Mail.GHOST);

        } catch (Exception sqle) {
            log(sqle.getLocalizedMessage());
            throw new MessagingException(sqle.toString());
        }
    }

    private void doMultipart(Multipart mp, MyHttpParams params) {
        for (int j = 0; j < mp.getCount(); j++) {
            System.out.println("int j: "+j);
            Part part = mp.getBodyPart(j);
            String disposition = part.getDisposition();
            if (disposition == null) {
                System.out.println("disposition null");
                // Check if plain
                MimeBodyPart mbp = (MimeBodyPart) part;
                System.out.println(("MimeBodyPart.content " + mbp.getContent()));
                if (mbp.isMimeType("text/plain")) {
                    System.out.println("Mime type is plain");
                    params.text += (String) mbp.getContent();
                } else if(mbp.isMimeType("text/html")){
                    System.out.println("mbp.isMimeType(\"text/html\")");
                    params.html += mbp.getContent().toString();
                } else {
                    System.out.println("Mime type is not plain :" + mbp.getContentType());
                    params.addFile(IOUtil.toByteArray(part.getInputStream()), mbp.getContentType());
                    // Special non-attachment cases here of
                    // image/gif, text/html, ...
//                    EMailAttach ema = new EMailAttach();
//                    ema.name = decodeName(part.getFileName());
//                    File savedir = new File(receiving_attachments);
//                    savedir.mkdirs();
//                    File savefile = File.createTempFile("emailattach", ".atch",
//                            savedir);
//                    ema.path = savefile.getAbsolutePath();
//                    ema.size = part.getSize();
//                    vema.add(ema);
//                    ema.size = saveFile(savefile, part);
                }
            } else if ((disposition != null) &&
                    (disposition.equals(Part.ATTACHMENT) ||
                    disposition.equals(Part.INLINE))) {
                System.out.println("disposition " + disposition);
                // Check if plain
                MimeBodyPart mbp = (MimeBodyPart) part;
                log("MimeBodyPart.content " + mbp.getContent());
                if (mbp.isMimeType("text/plain")) {
                    System.out.println("Mime type is plain");
                    params.text += (String) mbp.getContent();
                } else {
                    System.out.println("Mime type is not plain :" + mbp.getContentType());
                    params.addFile(IOUtil.toByteArray(part.getInputStream()), mbp.getContentType());
//                    log(String.format("Save file (%s)", part.getFileName()));
//                    EMailAttach ema = new EMailAttach();
//                    ema.name = decodeName(part.getFileName());
//                    File savedir = new File(receiving_attachments);
//                    savedir.mkdirs();
//                    File savefile = File.createTempFile("emailattach", ".atch",
//                            savedir);
//                    ema.path = savefile.getAbsolutePath();
//                    ema.size = part.getSize();
//                    vema.add(ema);
//                    ema.size = saveFile(savefile, part);
                }
            }
        }
    
}

    class MyHttpParams {
        Vector<byte[]> files = new Vector<byte[]>();;
        Vector<String> contentTypes = new Vector<String>();;
        String html = "";
        String text = "";
        Long from = -1L;
        Long to = -1L;
        Long project = -1L;

        @Override
        public String toString() {
            return "        Vector<byte[]> files = \n" + files.size() + 
            "        Vector<String> contentTypes = \n" + contentTypes.size()+ 
            "        String html = \n" + html+ 
            "        String text = \n" + text + 
            "        Long from = \n" + from + 
            "        Long to = \n" + to+ 
            "        Long project = \n" + project+ 
            "";
        }
        void addFile(byte[] arr, String contentType) {
            files.add(arr);
            contentTypes.add(contentType);
        }

        public void send() throws ClientProtocolException, IOException {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpPost post = new HttpPost("http://benyaptirdim.herokuapp.com/rest/post");

            MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addTextBody("key", "heroku");
            for (int i = 0; i < files.size(); i++) {
                builder.addBinaryBody("file[" + i + "]", files.get(i), ContentType.parse(contentTypes.get(0)), "file[" + i + "]");
            }
            builder.addTextBody("html", html);
            builder.addTextBody("from", "" + from);
            builder.addTextBody("to", "" + to);
            builder.addTextBody("text", text);
            builder.addTextBody("project", "" + project);
            post.setEntity(builder.build());

            CloseableHttpResponse response = null;
            try {
                System.out.println("params: " + this.toString());
                System.out.println("post : "+post.getEntity());
                response = httpclient.execute(post);
                HttpEntity entity = response.getEntity();
                System.out.println("response : "+entity);
                if (entity != null) {
                    InputStream instream = entity.getContent();
                    try {
                        // do something useful
                    } finally {
                        instream.close();
                    }
                }
            } finally {
                if(response != null){
                    response.close();
                }
            }
        }
    }
}