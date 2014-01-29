package com.benyaptirdim.mailet;

import org.apache.mailet.GenericRecipientMatcher;
import org.apache.mailet.MailAddress;

public class SchedulingRequestMatcher extends GenericRecipientMatcher
{
  public boolean matchRecipient(MailAddress recipient)
  {
    if (recipient.getUser().startsWith("request-available-")) {
      return true;
    }
    if (recipient.getUser().startsWith("request-unavailable-")) {
      return true;
    }
    return false;
  }
}