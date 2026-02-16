package com.langleydata.homepoker.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.langleydata.homepoker.api.Feedback;
import com.langleydata.homepoker.api.GameSettings;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.parameter.SentBy;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;

/** A service used for sending emails to players such as invites
 * 
 */
@Service
public class EmailSender {
	private final Logger logger = LoggerFactory.getLogger(EmailSender.class);
	
	private static Map<String, String> tokens = new HashMap<>();
	private static final String FEEDBACK_TO = "support@pokerroomsnow.com";
	static final String SCHEDULE_FROM = "noreply@pokerroomsnow.com";
	
	private static final String TABLE_LINK = "https://app.pokerroomsnow.com/texas/{gameid}";
	private static final String PLAYER_TEMPLATE = "static/email/player-invite.html";
	private static final String CALENDAR_TEMPLATE = "static/email/ical-description.html";
	private static final String HOST_TEMPLATE = "static/email/host-invite.html";
	private static final String ORGANISER_TEMPLATE = "static/email/organiser-info.html";
	private static final String INVITE_SUBJECT = "You are invited to a private poker game at PokerRoomsNow.com";
	private static final String HOST_SUBJECT = "You are hosting a new poker game at PokerRoomsNow.com";
	private static final String ORGANISER_SUBJECT = "You have scheduled a new poker game at PokerRoomsNow.com";
    private static final String MAILTO = "MAILTO:";
    
    
    static final long DEFAULT_DURATION = 1000 * 60 * 60 * 4;
    static final String CAL_EVENT_NAME = "Poker Game";
	
	@Value("${spring.profiles.active:Unknown}")
	private String activeProfile;
	
    @Autowired
    private JavaMailSender emailSender;

    
    static {
    	tokens.put("hostname", Pattern.quote("{hostname}"));
    	tokens.put("hostemail", Pattern.quote("{hostemail}"));
    	tokens.put("organisername", Pattern.quote("{organisername}"));
    	tokens.put("organiseremail", Pattern.quote("{organiseremail}"));
    	tokens.put("gameid", Pattern.quote("{gameid}"));
    	tokens.put("tablelink", Pattern.quote("{tablelink}"));
    	tokens.put("gametype", Pattern.quote("{gametype}"));
    	tokens.put("gameformat", Pattern.quote("{gameformat}"));
    	tokens.put("buyin", Pattern.quote("{buyin}"));
    	tokens.put("moneytype", Pattern.quote("{moneytype}"));
    	tokens.put("starttime", Pattern.quote("{starttime}"));
    	tokens.put("maxplayers", Pattern.quote("{maxplayers}"));
    	tokens.put("numplayers", Pattern.quote("{numplayers}"));
    	tokens.put("hostwallet", Pattern.quote("{hostwallet}"));
    	tokens.put("additionalinfo", Pattern.quote("{additionalinfo}"));
    }
    
    public static byte[] screenshotToByteArray(final String screenshot, ScreenConversion encodeDecode) {
        if (StringUtils.isNotBlank(screenshot)) {
        	int split = screenshot.indexOf(",");
        	String toAttach = screenshot;
        	if (split > 0) {
        		toAttach = screenshot.substring(split + 1);
        	}
        	
        	if (encodeDecode == ScreenConversion.ENCODE) {
        		return Base64.encodeBase64(toAttach.getBytes());
        	} else {
        		return Base64.decodeBase64(toAttach.getBytes());
        	}
        }
        return null;
    }
    public enum ScreenConversion {
    	ENCODE, DECODE
    }
    /**
     * 
     * @param feedback
     * @return
     * @throws MessagingException
     */
    public boolean sendEmail(final Feedback feedback) throws MessagingException {
    	final MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(feedback.getPlayerEmail());

        helper.setTo(FEEDBACK_TO);
        helper.setSubject(feedback.getSubject()); 
        helper.setText(String.format("Message ID: %s\n\n %s", feedback.getMessageId(), feedback.getBody()), true);
        
        final byte[] bScreen = screenshotToByteArray(feedback.getScreen(), ScreenConversion.ENCODE);
        if (bScreen !=null) {
        	helper.addAttachment("screenshot.png", new ByteArrayResource( bScreen ));
        }
        //TODO This doesn't seem to be sending...?
        new Thread(() -> {
        	try {
				if (activeProfile.contains("prod")) {
					emailSender.send(message);
				} else {
					logger.info("Send feedback email to: {} from {}", FEEDBACK_TO, feedback.getPlayerEmail());
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
        }).start();
        
		return true;
    }
    /**
     * 
     * @param to
     * @param subject
     * @param htmlBody
     * @throws MessagingException
     */
    public boolean sendEmail(final String to, final String subject, final String htmlBody, final Calendar iCal) throws MessagingException {
        
    	final MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(SCHEDULE_FROM);
        helper.setTo(to);
        helper.setSubject(subject); 
        helper.setText(htmlBody, true);
        if (iCal!=null) {
        	helper.addAttachment("invite.ics", new ByteArrayResource(iCal.toString().getBytes()));
        }
        
		if (StringUtils.isBlank(to)) {
			return false;
		}
        new Thread(() -> {
        	try {
				if (activeProfile.contains("prod")) {
					emailSender.send(message);
				} else {
					logger.info("Send feedback email to: {} from {}", to, SCHEDULE_FROM);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
        }).start();
        
		return true;
    }
    
    /** Send invite emails to everyone in the settings invite list
     * 
     * @param settings
     * @return
     * @throws MessagingException 
     */
    public boolean sendInviteEmails(final GameSettings settings) throws MessagingException {
    	
    	final String inviteTemplate = buildTemplate(PLAYER_TEMPLATE, settings);
    	
    	if (StringUtils.isBlank(inviteTemplate)) {
    		return false;
    	}
    	
    	final Calendar iCal = createiCalEvent(settings);
    	
    	// Send to players
    	if (settings.getInvitedEmails().size() > 0) {
	    	logger.info("Sending invite emails to {} players for game {}", settings.getInvitedEmails().size(), settings.getGameId());
	    	settings.getInvitedEmails().forEach(email -> {
				try {
					sendEmail(email, INVITE_SUBJECT, inviteTemplate, iCal);
				} catch (MessagingException e) {
					logger.error(e.getMessage());
				}
			});
    	}
    	
    	// Send host email (if not organiser)
    	if (settings.getHostEmail().equalsIgnoreCase(settings.getOrganiserEmail())==false) {
    		sendEmail(settings.getHostEmail(), HOST_SUBJECT, buildTemplate(HOST_TEMPLATE, settings), iCal);
    	}

    	// Organiser email
    	logger.info("New game '{}' organised by user {} - Emails sent", settings.getGameId(), settings.getOrganiserId());
    	sendEmail(settings.getOrganiserEmail(), ORGANISER_SUBJECT, buildTemplate(ORGANISER_TEMPLATE, settings), iCal);
    	
    	return true;
    }
    
    /** Create an iCalendar event for the game
     * 
     * @param schedTime The scheduled start time UTC
     * @param attendeesEmail The list of invites
     * @return A new iCalendar event
     */
    private Calendar createiCalEvent(final GameSettings settings) {
    	
    	final Invitee organiser = new Invitee(settings.getOrganiserName(), settings.getOrganiserEmail());
    	final Invitee host = new Invitee(settings.getHostName(), settings.getHostEmail());
    	final String description = buildTemplate(CALENDAR_TEMPLATE, settings);
    	
    	String hostTitle = CAL_EVENT_NAME;
    	if (StringUtils.isNotBlank(host.name)) {
	    	if (host.name.toLowerCase().endsWith("s")) {
	    		hostTitle = host.name + "' " + CAL_EVENT_NAME;
	    	} else {
	    		hostTitle = host.name + "'s " + CAL_EVENT_NAME;
	    	}
    	}
    	
    	// Create the event
    	DateTime start = new DateTime(settings.getScheduledTime());
    	DateTime end = new DateTime(settings.getScheduledTime() + DEFAULT_DURATION);
    	VEvent meeting = new VEvent(start, end, hostTitle);
    	meeting.getProperties().add(Method.REQUEST);
    	meeting.getProperties().add(Transp.OPAQUE);
    	meeting.getProperties().add(Status.VEVENT_CONFIRMED);
        
    	// generate unique identifier..
    	UidGenerator ug = new RandomUidGenerator();
    	Uid uid = ug.generateUid();
    	meeting.getProperties().add(uid);
    	
    	// General info
    	meeting.getProperties().add(new Description(description));
    	meeting.getProperties().add(new Location(TABLE_LINK.replace("{gameid}",settings.getGameId())));
    	settings.getInvitedEmails().forEach(i -> addAttendee(meeting, new Invitee(i), Role.OPT_PARTICIPANT));
    	
    	// Who organised it
    	addAttendee(meeting, organiser, Role.CHAIR);
    	addAttendee(meeting, host, Role.REQ_PARTICIPANT);
    	
    	// Create a calendar
    	Calendar icsCalendar = new Calendar();
    	icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
    	icsCalendar.getProperties().add(Version.VERSION_2_0);
    	icsCalendar.getProperties().add(CalScale.GREGORIAN);
    	icsCalendar.getProperties().add(new Summary(hostTitle));
    	icsCalendar.getComponents().add(meeting);
    	
    	return icsCalendar;
    }

	/**
	 * Add an attendee to the iCal meeting
	 * 
	 * @param meeting The meeting event
	 * @param invite  The Invitee
	 * @param role    The role of the attendee. If Role.CHAIR then it is assumed to
	 *                be the organiser
	 */
	private void addAttendee(VEvent meeting, Invitee invite, Role role) {
		Attendee att = new Attendee(URI.create(MAILTO + invite.email));
		
		if (StringUtils.isNotBlank(invite.name)) {
			att.getParameters().add(new Cn("\"" + invite.name + "\""));
		}
		
		if (role == Role.CHAIR) {
			final ParameterList pl = new ParameterList();
			pl.add(new Cn("\"" + invite.name + "\""));
			try {
				final Organizer o = new Organizer(pl, MAILTO + invite.email);
				o.getParameters().add(new SentBy(SCHEDULE_FROM));
				meeting.getProperties().add(o);
			} catch (URISyntaxException e) {
				logger.error("Add organiser error: " + e.getMessage());
			}
			return;
		}
		
		att.getParameters().add(role);
		att.getParameters().add(Rsvp.TRUE);
		meeting.getProperties().add(att);
	}

    /** Load an email template from resources and populate tokens
     * 
     * @param file
     * @return
     */
    private String buildTemplate(final String file, GameSettings settings) {
    	String template = "";
    	try (InputStream inputStream = new ClassPathResource(file).getInputStream()) {
    		template = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.error("Unable to read template file:", e);
			return null;
		}
    	
    	if (StringUtils.isBlank(template)) {
    		return null;
    	}

    	// There's probably a better way of doing this, but not expecting much overhead!
    	return template.replaceAll(tokens.get("hostname"), settings.getHostName())
    						.replaceAll(tokens.get("hostemail"), settings.getHostEmail())
    						.replaceAll(tokens.get("organisername"), settings.getOrganiserName())
    						.replaceAll(tokens.get("organiseremail"), settings.getOrganiserEmail())
				    		.replaceAll(tokens.get("gameid"), settings.getGameId())
				    		.replaceAll(tokens.get("tablelink"), TABLE_LINK.replace("{gameid}",settings.getGameId()))
				    		.replaceAll(tokens.get("gametype"), settings.getGameType().getFriendly())
				    		.replaceAll(tokens.get("additionalinfo"), settings.getAdditionalInfo())
				    		.replaceAll(tokens.get("gameformat"), settings.getFormat().name())
				    		.replaceAll(tokens.get("moneytype"), settings.getMoneyType().name())
				    		.replaceAll(tokens.get("hostwallet"), settings.isHostControlledWallet() ? "will" : "will not")
				    		.replaceAll(tokens.get("buyin"), "&pound;" + settings.getBuyInAmount())
				    		.replaceAll(tokens.get("maxplayers"), ""+settings.getGameType().getMaxPlayers() )
				    		.replaceAll(tokens.get("numplayers"), ""+settings.getInvitedEmails().size() );
    	
    }

    static class Invitee {
    	final String name;
    	final String email;
    	
    	Invitee(final String email) {
    		this.email = email;
    		this.name = null;
    	}
    	Invitee(final String name, final String email) {
    		this.name = name;
    		this.email = email;
    	}
    }
}
