package com.benyaptirdim.mailet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Vector;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.james.util.io.IOUtil;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public class SchedulingRequestMailet extends GenericMailet {
    Properties properties = new Properties();

    public void init() throws MessagingException {
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.host", "smtp.yandex.ru");
        properties.put("mail.smtp.port", "25");
    }

    private boolean textIsHtml = false;

    private String getText2(Part p) throws MessagingException, IOException {

        if (p.isMimeType("text/*")) {
            String s = new String((p.getContent().toString()).getBytes("UTF-8"));
            textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text ??
            // TODO:
            Multipart mp = (Multipart) p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getText2(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText2(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText2(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText2(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }

    public void forward(Message message, boolean processFailed) throws AddressException, MessagingException {
        Session session = Session.getDefaultInstance(properties);
        Message forward = new MimeMessage(session);
        // Fill in header
        forward.setRecipients(Message.RecipientType.TO, InternetAddress.parse("guven@benyaptirdim.com"));
        forward.setSubject("Fwd " + (processFailed ? "failed!" : "successed") + " James Message -" + message.getSubject());
        forward.setFrom(new InternetAddress("guven@benyaptirdim.com"));

        // Create the message part
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        // Create a multipart message
        Multipart multipart = new MimeMultipart();
        // set content
        messageBodyPart.setContent(message, "message/rfc822");
        // Add part to multi part
        multipart.addBodyPart(messageBodyPart);
        // Associate multi-part with message
        forward.setContent(multipart);
        forward.saveChanges();

        // Send the message by authenticating the SMTP server
        // Create a Transport instance and call the sendMessage
        Transport t = session.getTransport("smtp");
        try {
            t.connect("guven@benyaptirdim.com", "RAPtor12345");
            t.sendMessage(forward, forward.getAllRecipients());
        } finally {
            if (t != null) {
                t.close();
            }
        }
    }

    public void service(Mail mail) throws MessagingException {
        try {
            MyHttpParams params = new MyHttpParams();
            MimeMessage message = mail.getMessage();
            String text = getText(message);
            log("adding text : " + text);
            if (text != null) {
                params.text += process(text);
            }
            String html = getHtml(message);
            log("adding html : " + html);
            if (html != null) {
                
                params.html += process(html);
            }
            try {
                Multipart multipart = (Multipart) message.getContent();
                // log(multipart.getCount());

                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && ! hasInline(bodyPart)) {
                        continue; // dealing with attachments only
                    }
                    log("adding file:" + bodyPart.getFileName());
                    log("content type:" + bodyPart.getContentType());
                    params.addFile(IOUtil.toByteArray(bodyPart.getInputStream()), bodyPart.getContentType(), bodyPart.getFileName());
                }
            } catch (Exception e) {
                log(e.toString());
            }

            params.send();

            MailAddress recipient = (MailAddress) mail.getRecipients().iterator().next();

            // maker gonderiyor
            if (recipient.getUser().startsWith("maker-")) {
                processMaker(recipient.getUser(), params);
            } else if (recipient.getUser().startsWith("customer-")) {
                processCustomer(recipient.getUser(), params);
            }

            // mail.setState(Mail.DEFAULT);
            mail.setState(Mail.GHOST);
            try {
                forward(message, false);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception sqle) {
            log(sqle.getLocalizedMessage());
            try {
                forward(mail.getMessage(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            throw new MessagingException(sqle.toString());
        }
    }

    private String process(String string) {
        ProcessBuilder pb = new ProcessBuilder("ruby", "/root/email_reply_parser.rb","'"+string.replaceAll("'", "\"")+"'");
        try {
            Process p =pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ( (line = br.readLine()) != null) {
               builder.append(line);
               builder.append(System.getProperty("line.separator"));
            }
            String result = builder.toString();
            return result;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return string;
    }

    private boolean hasInline(BodyPart bodyPart) {
        try {
            if(bodyPart.getHeader("Content-ID") !=null ){
                return true;
            }
            if(bodyPart.getHeader("X-Attachment-Id") !=null ){
                return true;
            }
            if(bodyPart.getDisposition() != null && bodyPart.getDisposition().contains("inline")){
                return true;
            }
            
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return false;
    }

    // address starts with maker-
    private void processMaker(String address, MyHttpParams params) throws Exception {
        String[] splitted = address.split("-");
        if (splitted.length != 4) {
            log("error on split processmaker");
            throw new Exception("error on split processmaker");
        }
        // maker-MakerID-ClientID-ProjectUUID
        // M-C-P pattern from netas!
        // splitted[0] = maker!!
        Long maker = Long.parseLong(splitted[1]);
        Long client = Long.parseLong(splitted[2]);
        String project = splitted[3];
        params.maker = maker;
        params.project = project;
        params.customer = client;
        params.fromTo = "M2C";
    }

    // address starts with client-
    private void processCustomer(String address, MyHttpParams params) throws Exception {
        String[] splitted = address.split("-");
        if (splitted.length != 4) {
            log("error on split processcustomer");
            throw new Exception("error on split processcustomer");
        }
        // customer-MakerID-ClientID-ProjectUUID
        // M-C-P pattern from netas!
        // splitted[0] = customer!!
        Long maker = Long.parseLong(splitted[1]);
        Long customer = Long.parseLong(splitted[2]);
        String project = splitted[3];
        params.maker = maker;
        params.project = project;
        params.customer = customer;
        params.fromTo = "C2M";
    }

    private String getHtml(Part p) throws MessagingException, IOException {
        log("getHtml");
        log(p.getContentType());
        if (p.isMimeType("text/html")) {
            log("get html content - : "+ new String((p.getContent().toString()).getBytes("UTF-8")));
            String s = readUTF8(p.getInputStream());
            log("getHtml html found!\n" + s);
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            log("getHtml multipart/alternative");
            Multipart mp = (Multipart) p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                log("getHtml multipart/alternative " + i);
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/html")) {
                    log("getHtml in " + "multipart/alternative " + i + "   text/html");
                    if (text == null){
                        text = getHtml(bp);
                        if(text != null){
                            return text;
                        }
                    }
                    continue;
                } else {
                    text = getHtml(bp);
                    if(text != null){
                        return text;
                    }
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getHtml(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }
        return null;
    }

    private String readUTF8(InputStream in) {
        try {
            BufferedInputStream buf = new BufferedInputStream(in);
            InputStreamReader reader = new InputStreamReader(buf, "UTF-8");
            StringBuilder builder = new StringBuilder();
            int data = reader.read();
            while (data != -1) {
                char theChar = (char) data;
                builder.append(theChar);
                data = reader.read();
            }
            buf.close();
            in.close();
            reader.close();
            return builder.toString();
        } catch (Exception e) {
            log(e.getLocalizedMessage());
        }
        return null;
    }

    private String getText(Part p) throws MessagingException, IOException {
        log("getText");
        if (p.isMimeType("text/plain")) {
            log("get content - : "+ new String((p.getContent().toString()).getBytes("UTF-8")));
            String s = readUTF8(p.getInputStream());
            log("getText plain found!\n" + s);
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            log("getText multipart/alternative");
            Multipart mp = (Multipart) p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                log("getText multipart/alternative " + i);
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    log("getText in " + "multipart/alternative " + i + "   text/plain");
                    if (text == null){
                        text = getText(bp);
                        if(text != null){
                            return text;
                        }
                    }
                    continue;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }
        return null;
    }

    class MyHttpParams {
        Vector<byte[]> files = new Vector<byte[]>();;
        Vector<String> contentTypes = new Vector<String>();;
        Vector<String> fileNames = new Vector<String>();;
        String html = "";
        String text = "";
        Long maker = -1L;
        Long customer = -1L;
        String project = "";
        String fromTo;

        @Override
        public String toString() {
            return "        Vector<byte[]> files = \n" + files.size() + "        Vector<String> contentTypes = \n" + contentTypes.size()
                    + "        String html = \n" + html + "        String text = \n" + text + "        Long from = \n" + maker + "        Long to = \n"
                    + customer + "        Long project = \n" + project + "";
        }

        void addFile(byte[] arr, String contentType, String filename) {
            files.add(arr);
            contentTypes.add(contentType);
            fileNames.add(filename);
        }

        public void send() throws ClientProtocolException, IOException {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpPost post = new HttpPost("http://benyaptirdim.herokuapp.com/rest/post");

            MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addTextBody("key", "heroku");
            for (int i = 0; i < files.size(); i++) {
                builder.addBinaryBody("files", files.get(i), ContentType.parse(fileNames.get(i)), fileNames.get(i));
            }
            builder.addTextBody("html", html,ContentType.create("text/plain", "UTF-8"));
            builder.addTextBody("from", "" + maker);
            builder.addTextBody("to", "" + customer);
            builder.addTextBody("text", text,ContentType.create("text/plain", "UTF-8"));
            builder.addTextBody("project", "" + project);
            post.setEntity(builder.build());
            builder.setCharset(Charset.forName("UTF-8"));

            CloseableHttpResponse response = null;
            try {
                log("params: " + this.toString());
                log("post : " + post.getEntity());
                response = httpclient.execute(post);
                HttpEntity entity = response.getEntity();
                log("response : " + entity);
                if (entity != null) {
                    InputStream instream = entity.getContent();
                    try {
                        // do something useful
                    } finally {
                        instream.close();
                    }
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }
    }
}