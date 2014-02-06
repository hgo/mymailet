package com.benyaptirdim.mailet;

import org.apache.mailet.GenericRecipientMatcher;
import org.apache.mailet.MailAddress;

public class SchedulingRequestMatcher extends GenericRecipientMatcher
{
  public boolean matchRecipient(MailAddress recipient)
  {
    if (recipient.getUser().startsWith("maker-")) {
      return true;
    }
    if (recipient.getUser().startsWith("customer-")) {
      return true;
    }
    return false;
  }
}