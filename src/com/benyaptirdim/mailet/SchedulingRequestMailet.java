package com.benyaptirdim.mailet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public class SchedulingRequestMailet extends GenericMailet {
    public void init() throws MessagingException {
    }

    class Email {
        String body = "";
    }

    class EMailAttach {
        String name;
        public String path;
        public int size;
    }

    protected String decodeName(String name) throws Exception {
        log("decode name: " + name);
        if (name == null || name.length() == 0) {
            return "unknown";
        }
        String ret = java.net.URLDecoder.decode(name, "UTF-8");

        // also check for a few other things in the string:
        ret = ret.replaceAll("=\\?utf-8\\?q\\?", "");
        ret = ret.replaceAll("\\?=", "");
        ret = ret.replaceAll("=20", " ");

        return ret;
    }

    protected int saveFile(File saveFile, Part part) throws Exception {

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(saveFile));

        byte[] buff = new byte[2048];
        InputStream is = part.getInputStream();
        int ret = 0, count = 0;
        while ((ret = is.read(buff)) > 0) {
            bos.write(buff, 0, ret);
            count += ret;
        }
        bos.close();
        is.close();
        return count;
    }

    public void service(Mail mail) throws MessagingException {
        try {
//            ContentHandler handler = new CustomContentHandler();
//            MimeStreamParser parser = new MimeStreamParser();
//            parser.setContentHandler(handler);
//            parser.parse(mail.getMessage().getInputStream());
//            /*
            MimeTokenStream stream = new MimeTokenStream();
            stream.parse(mail.getMessage().getInputStream());
            for (EntityState state = stream.getState();
                 state != EntityState.T_END_OF_STREAM;
                 state = stream.next()) {
              switch (state) {
                case T_BODY:
                  System.out.println("Body detected, contents = "
                    + stream.getInputStream() + ", header data = "
                    + stream.getBodyDescriptor());
                  break;
                case T_FIELD:
                  System.out.println("Header field detected: "
                    + stream.getField());
                  break;
                case T_START_MULTIPART:
                  System.out.println("Multipart message detexted,"
                    + " header data = "
                    + stream.getInputStream());
              }
            }
//*/
            Collection recipients = mail.getRecipients();
            if (recipients.size() > 1) {
                // TODO:
                return;
            }
            Email email = new Email();
            MimeMessage message = mail.getMessage();
            Vector<EMailAttach> vema = new Vector<EMailAttach>();
            Object content = message.getContent();
            String receiving_attachments = "./email/attachments/";
            if (content instanceof java.lang.String) {
                email.body = (String) content;
            } else if (content instanceof Multipart) {
                Multipart mp = (Multipart) content;

                for (int j = 0; j < mp.getCount(); j++) {
                    Part part = mp.getBodyPart(j);

                    String disposition = part.getDisposition();

                    if (disposition == null) {
                        log("disposition null");
                        // Check if plain
                        MimeBodyPart mbp = (MimeBodyPart) part;
                        log("MimeBodyPart.content " + mbp.getContent());
                        if (mbp.isMimeType("text/plain")) {
                            log("Mime type is plain");
                            email.body += (String) mbp.getContent();
                        } else {
                            log("Mime type is not plain");
                            // Special non-attachment cases here of
                            // image/gif, text/html, ...
                            EMailAttach ema = new EMailAttach();
                            ema.name = decodeName(part.getFileName());
                            File savedir = new File(receiving_attachments);
                            savedir.mkdirs();
                            File savefile = File.createTempFile("emailattach", ".atch", savedir);
                            ema.path = savefile.getAbsolutePath();
                            ema.size = part.getSize();
                            vema.add(ema);
                            ema.size = saveFile(savefile, part);
                        }
                    } else if ((disposition != null) && (disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE))) {
                        log("disposition " + disposition);
                        // Check if plain
                        MimeBodyPart mbp = (MimeBodyPart) part;
                        log("MimeBodyPart.content " + mbp.getContent());
                        if (mbp.isMimeType("text/plain")) {
                            log("Mime type is plain");
                            email.body += (String) mbp.getContent();
                        } else {
                            log(String.format("Save file (%s)", part.getFileName()));
                            EMailAttach ema = new EMailAttach();
                            ema.name = decodeName(part.getFileName());
                            File savedir = new File(receiving_attachments);
                            savedir.mkdirs();
                            File savefile = File.createTempFile("emailattach", ".atch", savedir);
                            ema.path = savefile.getAbsolutePath();
                            ema.size = part.getSize();
                            vema.add(ema);
                            ema.size = saveFile(savefile, part);
                        }
                    }
                }
            }

            log("body:\n " + email.body);
            log("atc size : " + vema.size());
            MailAddress recipient = (MailAddress) recipients.iterator().next();

            boolean sellerAvailable = recipient.getUser().startsWith("request-available-");

            String requestID = recipient.getUser();
            int pos = requestID.lastIndexOf("-");
            requestID = requestID.substring(pos + 1);

            String sellerAddress = mail.getSender().toString();
            System.out.println("sellerAddress: " + sellerAddress);
            System.out.println("requestID: " + requestID);

            mail.setState("ghost");

        } catch (Exception sqle) {
            throw new MessagingException(sqle.toString());
        }
    }
}