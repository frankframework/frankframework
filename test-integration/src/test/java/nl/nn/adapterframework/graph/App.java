package nl.nn.adapterframework.graph;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.microsoft.graph.models.extensions.DateTimeTimeZone;
import com.microsoft.graph.models.extensions.Event;
import com.microsoft.graph.models.extensions.MailFolder;
import com.microsoft.graph.models.extensions.User;

public class App {
	
	private static String ACCESS_TOKEN = "eyJ0eXAiOiJKV1QiLCJub25jZSI6ImpJcmpTVTFvMkJvSGdnYlNNOG5DUUNtR0dsUmV4cndwSmZNTlFKQVBKOEkiLCJhbGciOiJSUzI1NiIsIng1dCI6ImppYk5ia0ZTU2JteFBZck45Q0ZxUms0SzRndyIsImtpZCI6ImppYk5ia0ZTU2JteFBZck45Q0ZxUms0SzRndyJ9.eyJhdWQiOiIwMDAwMDAwMy0wMDAwLTAwMDAtYzAwMC0wMDAwMDAwMDAwMDAiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC9jMDdmYjM1Ny0yYWM0LTRmZDMtOTFhYS1hYTIyYjI3Yzk5YjMvIiwiaWF0IjoxNjAxNDU5NDExLCJuYmYiOjE2MDE0NTk0MTEsImV4cCI6MTYwMTQ2MzMxMSwiYWNjdCI6MCwiYWNyIjoiMSIsImFpbyI6IkUyUmdZS2pNK1dzODc5VE5YRkU3K1dQclZIdXVWMzNpdXArYjFIK2NKM2hiYjZSV3hoMEEiLCJhbXIiOlsicHdkIl0sImFwcF9kaXNwbGF5bmFtZSI6IkphdmEgR3JhcGggVHV0b3JpYWwiLCJhcHBpZCI6IjNlYjliOTA2LTM2MmYtNDdlOS1iYzU3LTMxMGZlZGI4ZmRiZiIsImFwcGlkYWNyIjoiMCIsImZhbWlseV9uYW1lIjoidmFuIEJyYWtlbCIsImdpdmVuX25hbWUiOiJHZXJyaXQiLCJpZHR5cCI6InVzZXIiLCJpcGFkZHIiOiI4My4xNjMuMTU5LjExNyIsIm5hbWUiOiJHZXJyaXQgdmFuIEJyYWtlbCIsIm9pZCI6IjA5YWQxNzNlLWViZmMtNDY4NC04ZmUxLTMzZmRkZTBjMTJmYyIsIm9ucHJlbV9zaWQiOiJTLTEtNS0yMS0yMDQ3MjgyMDItMzM4MjEwNDY1My0xNDgxMTQwNjMxLTExNjQiLCJwbGF0ZiI6IjE0IiwicHVpZCI6IjEwMDM3RkZFQUY1RTdFN0EiLCJyaCI6IjAuQUFBQVY3Tl93TVFxMDAtUnFxb2lzbnlac3dhNXVUNHZOdWxIdkZjeEQtMjRfYjlJQU9nLiIsInNjcCI6IkNhbGVuZGFycy5SZWFkIE1haWwuUmVhZCBNYWlsLlNlbmQgb3BlbmlkIHByb2ZpbGUgVXNlci5SZWFkIGVtYWlsIiwic3ViIjoibkY3d0JxcmM1c1ZTR1NsRGl2VFJTdjMxem9CeUtuby1NUFQ5SzlXOXRDYyIsInRlbmFudF9yZWdpb25fc2NvcGUiOiJFVSIsInRpZCI6ImMwN2ZiMzU3LTJhYzQtNGZkMy05MWFhLWFhMjJiMjdjOTliMyIsInVuaXF1ZV9uYW1lIjoiZ2Vycml0QGludGVncmF0aW9ucGFydG5lcnMubmwiLCJ1cG4iOiJnZXJyaXRAaW50ZWdyYXRpb25wYXJ0bmVycy5ubCIsInV0aSI6IjF2NmRkeUxSREVDckU2RVFqb1MyQUEiLCJ2ZXIiOiIxLjAiLCJ3aWRzIjpbImI3OWZiZjRkLTNlZjktNDY4OS04MTQzLTc2YjE5NGU4NTUwOSJdLCJ4bXNfc3QiOnsic3ViIjoiVklMY084Zy03WkREbDhOYUh4UzRLeUlzd0NGU0dTdkJNY3ZtWHgyODNGdyJ9LCJ4bXNfdGNkdCI6MTQ2NzgwODE0OH0.gMWD67JdZrpNH8eqs6R8AkyQV9ATgIDcYFMuudCtBD5AeKORCqelwxONAff0hs8fo7epsGeRMTmydkDpgBL-o0s4bzeJAa0chRV1Hf5GeMvfS35F61qpz-S6cnWKNjUDu9x4iMCGT618-WWBzuOLrs-cfpo0yIw2WtywkrpFQuXGCsv1ggepzweLBuWGHa1MaAx_bDiXGQpPiTUL8rXVImLmCJ11BaBe8i6GulSsfRACFXB4N0T7U6LGcC7V0jweJ5dKduUEqTnEy8CONq-TSGr_H1wDYHWZP-kM-T0vVI1s6fL1U6x2TkOQJ1VE6RiKMmYRc8JmUukY5gMd_VTSAw";
	
	private static boolean use_configured_accessToken = false;
	
	   public static void main(String[] args) {
	        System.out.println("Java Graph Tutorial");
	        System.out.println();

	     // Load OAuth settings
	        final Properties oAuthProperties = new Properties();
	        try {
	            oAuthProperties.load(App.class.getResourceAsStream("/oAuth.properties"));
	        } catch (IOException e) {
	            System.out.println("Unable to read OAuth configuration. Make sure you have a properly formatted oAuth.properties file. See README for details.");
	            return;
	        }

	        final String appId = oAuthProperties.getProperty("app.id");
	        final String[] appScopes = oAuthProperties.getProperty("app.scopes").split(",");
	        
	        
	        // Get an access token
	        Authentication.initialize(appId);
	        final String accessToken = use_configured_accessToken && StringUtils.isNotEmpty(ACCESS_TOKEN) ? ACCESS_TOKEN : Authentication.getUserAccessToken(appScopes);
	        
	     // Greet the user
	        User user = Graph.getUser(accessToken);
	        System.out.println("Welcome " + user.displayName);
	        System.out.println();
	        
	        Scanner input = new Scanner(System.in);

	        int choice = -1;

	        while (choice != 0) {
	            System.out.println("Please choose one of the following options:");
	            System.out.println("0. Exit");
	            System.out.println("1. Display access token");
	            System.out.println("2. List calendar events");
	            System.out.println("3. List mail folders");

	            try {
	                choice = input.nextInt();
	            } catch (InputMismatchException ex) {
	                // Skip over non-integer input
	                input.nextLine();
	            }

	            // Process user choice
	            switch(choice) {
	                case 0:
	                    // Exit the program
	                    System.out.println("Goodbye...");
	                    break;
	                case 1:
	                    // Display access token
	                	System.out.println("Access token: " + accessToken);
	                    break;
	                case 2:
	                    // List the calendar
	                	listCalendarEvents(accessToken);
	                    break;
	                case 3:
	                    // List the calendar
	                	listMailfolders(accessToken);
	                    break;
	                default:
	                    System.out.println("Invalid choice");
	            }
	        }

	        input.close();
	    }
	   
	   private static void listCalendarEvents(String accessToken) {
		    // Get the user's events
		    List<Event> events = Graph.getEvents(accessToken);

		    System.out.println("Events:");

		    for (Event event : events) {
		        System.out.println("Subject: " + event.subject);
		        System.out.println("  Organizer: " + event.organizer.emailAddress.name);
		        System.out.println("  Start: " + formatDateTimeTimeZone(event.start));
		        System.out.println("  End: " + formatDateTimeTimeZone(event.end));
		    }

		    System.out.println();
		}
	   
	   private static void listMailfolders(String accessToken) {
		    // Get the user's events
		    List<MailFolder> folders = Graph.getMailMessages(accessToken);

		    System.out.println("Folders:");

		    for (MailFolder folder : folders) {
		        System.out.println("Folder: " + ToStringBuilder.reflectionToString(folder));
	    }

		    System.out.println();
		}

	   private static String formatDateTimeTimeZone(DateTimeTimeZone date) {
		    LocalDateTime dateTime = LocalDateTime.parse(date.dateTime);

		    return dateTime.format(
		        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) +
		        " (" + date.timeZone + ")";
		}
}
