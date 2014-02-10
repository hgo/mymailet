package com.benyaptirdim.mailet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
  
  public static void main(String[] args) {
      String s = "en son bunu yaziyorum bak adam olun,..\n" + 
      		"\n" + 
      		"\n" + 
      		"On 10 February 2014 17:16, Güven Özyurt <guvenozyurt@gmail.com> wrote:\n" + 
      		"\n" + 
      		"> 312312 312  xa\n" + 
      		"> s dsa\n" + 
      		"> d al;sd'as .;' 12.;3' 12 3\n" + 
      		">\n" + 
      		">\n" + 
      		"> On 10 February 2014 15:11, Güven Özyurt <guvenozyurt@gmail.com> wrote:\n" + 
      		">\n" + 
      		">> çşiçşieqwçp ğeqçşie *qçşei* *qweqw *\n" + 
      		">>\n" + 
      		">>\n" + 
      		">> On 10 February 2014 15:08, Güven Özyurt <guvenozyurt@gmail.com> wrote:\n" + 
      		">>\n" + 
      		">>> md kladm kla d\n" + 
      		">>> *mkld amk dlas*\n" + 
      		">>>\n" + 
      		">>>\n" + 
      		">>> On 10 February 2014 14:58, Güven Özyurt <guvenozyurt@gmail.com> wrote:\n" + 
      		">>>\n" + 
      		">>>> md kladm kla d\n" + 
      		">>>> *mkld amk dlas*\n" + 
      		">>>>\n" + 
      		">>>>\n" + 
      		">>>>\n" + 
      		">>>> On 10 February 2014 14:42, Güven Özyurt <guvenozyurt@gmail.com> wrote:\n" + 
      		">>>>\n" + 
      		">>>>> md kladm kla d\n" + 
      		">>>>> *mkld amk dlas*\n" + 
      		">>>>>\n" + 
      		">>>>>\n" + 
      		">>>>>\n" + 
      		">>>>>\n" + 
      		">>>>> On 10 February 2014 14:30, Güven Özyurt <guvenozyurt@gmail.com> wrote:\n" + 
      		">>>>>\n" + 
      		">>>>>> dyuyutyuer wn hrwenr jrwe\n" + 
      		">>>>>>\n" + 
      		">>>>>> *fdsfkmlsdf*\n" + 
      		">>>>>>\n" + 
      		">>>>>> *njkdjksanjd*\n" + 
      		">>>>>>\n" + 
      		">>>>>>\n" + 
      		">>>>>> On 10 February 2014 14:22, Güven Özyurt <guvenozyurt@gmail.com>wrote:\n" + 
      		">>>>>>\n" + 
      		">>>>>>> *caliskusu *\n" + 
      		">>>>>>> *ben se italik*\n" + 
      		">>>>>>>\n" + 
      		">>>>>>> bu da yar\n" + 
      		">>>>>>>\n" + 
      		">>>>>>>\n" + 
      		">>>>>>> On 10 February 2014 11:31, <notification@benyaptirdim.com> wrote:\n" + 
      		">>>>>>>\n" + 
      		">>>>>>>> ardaaskin@gmail.comcreated a project.butce: 250<v<500. Detay: cok\n" + 
      		">>>>>>>> acayip seyler istiyorum... reply to comment\n" + 
      		">>>>>>>>\n" + 
      		">>>>>>>\n" + 
      		">>>>>>>\n" + 
      		">>>>>>\n" + 
      		">>>>>\n" + 
      		">>>>\n" + 
      		">>>\n" + 
      		">>\n" + 
      		">\n" + 
      		"";
      System.out.println(process(s));
  }
  private static String process(String string) {
      ProcessBuilder pb = new ProcessBuilder("ruby" ,"/Users/guvenozyurt/Desktop/git/mymailet/jar/email_reply_parser.rb ","'"+string+"'");
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
}