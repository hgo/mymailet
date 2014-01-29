package com.benyaptirdim.mailet;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import javax.mail.MessagingException;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public class SchedulingRequestMailet extends GenericMailet {
    public void init() throws MessagingException {
    }

    public void service(Mail mail) throws MessagingException {
        try {
            Collection recipients = mail.getRecipients();
            if (recipients.size() > 1) {
                return;
            }
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