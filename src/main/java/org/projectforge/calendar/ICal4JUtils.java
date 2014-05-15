/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.calendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Comment;
import net.fortuna.ical4j.model.property.Contact;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.Dates;

import org.apache.commons.lang.StringUtils;
import org.projectforge.common.DateFormats;
import org.projectforge.common.DateHelper;
import org.projectforge.common.RecurrenceFrequency;
import org.projectforge.common.StringHelper;
import org.projectforge.plugins.teamcal.event.TeamEventAttendeeDO;
import org.projectforge.plugins.teamcal.event.TeamEventDO;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.PFUserDO;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ICal4JUtils
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ICal4JUtils.class);

  private static TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();

  private static final String ICAL_DATETIME_FORMAT = "yyyyMMdd'T'HHmmss";

  private static final String ICAL_DATE_FORMAT = "yyyyMMdd";

  /**
   * @return The timeZone (ical4j) built of the default java timeZone of the user.
   * @see PFUserContext#getTimeZone()
   */
  public static TimeZone getUserTimeZone()
  {
    return registry.getTimeZone(PFUserContext.getTimeZone().getID());
  }

  /**
   * @return The timeZone (ical4j) built of the default java timeZone of the user.
   * @see PFUserContext#getTimeZone()
   */
  public static TimeZone getTimeZone(final java.util.TimeZone timeZone)
  {
    return registry.getTimeZone(timeZone.getID());
  }

  public static VEvent createVEvent(final Date startDate, final Date endDate, final String uid, final String summary)
  {
    return createVEvent(startDate, endDate, uid, summary, false);
  }

  public static VEvent createVEvent(final Date startDate, final Date endDate, final String uid, final String summary, final boolean allDay)
  {
    final TimeZone timezone = getUserTimeZone();
    return createVEvent(startDate, endDate, uid, summary, allDay, timezone);
  }

  public static VEvent createVEvent(final Date startDate, final Date endDate, final String uid, final String summary, final boolean allDay,
      final TimeZone timezone)
  {
    VEvent vEvent;
    if (allDay == true) {
      final Date startUtc = CalendarUtils.getUTCMidnightDate(startDate);
      final Date endUtc = CalendarUtils.getUTCMidnightDate(endDate);
      final net.fortuna.ical4j.model.Date fortunaStartDate = new net.fortuna.ical4j.model.Date(startUtc);
      final org.joda.time.DateTime jodaTime = new org.joda.time.DateTime(endUtc);
      // requires plus 1 because one day will be omitted by calendar.
      final net.fortuna.ical4j.model.Date fortunaEndDate = new net.fortuna.ical4j.model.Date(jodaTime.plusDays(1).toDate());
      vEvent = new VEvent(fortunaStartDate, fortunaEndDate, summary);
    } else {
      final net.fortuna.ical4j.model.DateTime fortunaStartDate = new net.fortuna.ical4j.model.DateTime(startDate);
      fortunaStartDate.setTimeZone(timezone);
      final net.fortuna.ical4j.model.DateTime fortunaEndDate = new net.fortuna.ical4j.model.DateTime(endDate);
      fortunaEndDate.setTimeZone(timezone);
      vEvent = new VEvent(fortunaStartDate, fortunaEndDate, summary);
      vEvent.getProperties().add(timezone.getVTimeZone().getTimeZoneId());
    }
    vEvent.getProperties().add(new Uid(uid));
    return vEvent;
  }

  /**
   * 
   * @param rruleString
   * @return null if rruleString is empty, otherwise new RRule object.
   */
  public static RRule calculateRecurrenceRule(final String rruleString)
  {
    if (StringUtils.isBlank(rruleString) == true) {
      return null;
    }
    try {
      final RRule rule = new RRule(rruleString);
      return rule;
    } catch (final ParseException ex) {
      log.error("Exception encountered while parsing rrule '" + rruleString + "': " + ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * @param rruleString
   * @see ICal4JUtils#calculateRecurrenceRule(String)
   * @see RRule#getRecur()
   */
  public static Recur calculateRecurrence(final String rruleString)
  {
    final RRule rule = calculateRecurrenceRule(rruleString);
    return rule != null ? rule.getRecur() : null;
  }

  public static Date calculateRecurrenceUntil(final String rruleString)
  {
    if (StringUtils.isBlank(rruleString) == true) {
      return null;
    }
    final Recur recur = calculateRecurrence(rruleString);
    if (recur == null) {
      return null;
    }
    return recur.getUntil();
  }

  public static String getCal4JFrequencyString(final RecurrenceFrequency interval)
  {
    if (interval == RecurrenceFrequency.DAILY) {
      return Recur.DAILY;
    } else if (interval == RecurrenceFrequency.WEEKLY) {
      return Recur.WEEKLY;
    } else if (interval == RecurrenceFrequency.MONTHLY) {
      return Recur.MONTHLY;
    } else if (interval == RecurrenceFrequency.YEARLY) {
      return Recur.YEARLY;
    }
    return null;
  }

  /**
   * @param recur
   * @return
   */
  public static RecurrenceFrequency getFrequency(final Recur recur)
  {
    if (recur == null) {
      return null;
    }
    final String freq = recur.getFrequency();
    if (Recur.WEEKLY.equals(freq) == true) {
      return RecurrenceFrequency.WEEKLY;
    } else if (Recur.MONTHLY.equals(freq) == true) {
      return RecurrenceFrequency.MONTHLY;
    } else if (Recur.DAILY.equals(freq) == true) {
      return RecurrenceFrequency.DAILY;
    } else if (Recur.YEARLY.equals(freq) == true) {
      return RecurrenceFrequency.YEARLY;
    }
    return null;
  }

  /**
   * @param recur
   * @return
   */
  public static String getFrequency(final RecurrenceFrequency interval)
  {
    if (interval == null) {
      return null;
    }
    if (interval == RecurrenceFrequency.WEEKLY) {
      return Recur.WEEKLY;
    } else if (interval == RecurrenceFrequency.DAILY) {
      return Recur.DAILY;
    } else if (interval == RecurrenceFrequency.MONTHLY) {
      return Recur.MONTHLY;
    } else if (interval == RecurrenceFrequency.YEARLY) {
      return Recur.YEARLY;
    }
    return null;
  }

  public static java.sql.Date getSqlDate(final net.fortuna.ical4j.model.Date ical4jDate)
  {
    if (ical4jDate == null) {
      return null;
    }
    return new java.sql.Date(ical4jDate.getTime());
  }

  public static java.sql.Timestamp getSqlTimestamp(final net.fortuna.ical4j.model.Date ical4jDate)
  {
    if (ical4jDate == null) {
      return null;
    }
    return new java.sql.Timestamp(ical4jDate.getTime());
  }

  public static net.fortuna.ical4j.model.DateTime getICal4jDateTime(final java.util.Date javaDate, final java.util.TimeZone timeZone)
  {
    if (javaDate == null) {
      return null;
    }
    final String dateString = DateHelper.formatIsoTimestamp(javaDate, timeZone);
    final String pattern = DateFormats.ISO_TIMESTAMP_SECONDS;
    try {
      final net.fortuna.ical4j.model.DateTime dateTime = new net.fortuna.ical4j.model.DateTime(dateString, pattern, getTimeZone(timeZone));
      return dateTime;
    } catch (final ParseException ex) {
      log.error("Can't parse date '" + dateString + "' with pattern '" + pattern + "': " + ex.getMessage(), ex);
      return null;
    }
  }

  public static net.fortuna.ical4j.model.Date getICal4jDate(final java.util.Date javaDate, final java.util.TimeZone timeZone)
  {
    if (javaDate == null) {
      return null;
    }
    return new MyIcal4JDate(javaDate, timeZone);
  }

  public static net.fortuna.ical4j.model.Date parseICal4jDate(final String dateString)
  {
    if (dateString == null) {
      return null;
    }
    net.fortuna.ical4j.model.Date date;
    try {
      date = new net.fortuna.ical4j.model.Date(dateString);
    } catch (final ParseException ex) {
      log.error("Unable to parse date string: '" + dateString + "': " + ex.getMessage(), ex);
      return null;
    }
    return date;
  }

  /**
   * Format is '20130327T090000'
   * @param dateString
   * @param timeZone
   * @return
   */
  public static Date parseICalDateString(final String dateString, final java.util.TimeZone timeZone)
  {
    if (StringUtils.isBlank(dateString) == true) {
      return null;
    }
    String pattern;
    java.util.TimeZone tz = timeZone;
    if (dateString.indexOf('T') > 0) {
      pattern = ICAL_DATETIME_FORMAT;
    } else {
      pattern = ICAL_DATE_FORMAT;
      tz = DateHelper.UTC;
    }
    final DateFormat df = new SimpleDateFormat(pattern);
    df.setTimeZone(tz);
    try {
      return df.parse(dateString);
    } catch (final ParseException ex) {
      log.error("Can't parse ical date ('" + pattern + "': " + ex.getMessage(), ex);
      return null;
    }
  }

  public static Date parseISODateString(final String isoDateString)
  {
    if (StringUtils.isBlank(isoDateString) == true) {
      return null;
    }
    String pattern;
    if (isoDateString.indexOf(':') > 0) {
      pattern = DateFormats.ISO_TIMESTAMP_SECONDS;
    } else {
      pattern = DateFormats.ISO_DATE;
    }
    final DateFormat df = new SimpleDateFormat(pattern);
    df.setTimeZone(DateHelper.UTC);
    try {
      return df.parse(isoDateString);
    } catch (final ParseException ex) {
      log.error("Can't parse ISO date ('" + pattern + "': " + ex.getMessage(), ex);
      return null;
    }
  }

  public static String asISODateString(final Date date)
  {
    return asISODateString(date, DateHelper.UTC);
  }

  public static String asISODateString(final Date date, final java.util.TimeZone timeZone)
  {
    if (date == null) {
      return null;
    }
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_DATE);
    df.setTimeZone(timeZone);
    return df.format(date);
  }

  public static String asISODateTimeString(final Date date)
  {
    if (date == null) {
      return null;
    }
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_SECONDS);
    df.setTimeZone(DateHelper.UTC);
    return df.format(date);
  }

  public static String[] splitExDates(final String csv)
  {
    if (StringUtils.isBlank(csv) == true) {
      return null;
    }
    final String[] sa = StringHelper.splitAndTrim(csv, ",;|");
    if (sa == null || sa.length == 0) {
      return null;
    }
    return sa;
  }

  public static List<net.fortuna.ical4j.model.Date> parseISODateStringsAsICal4jDates(final String csv, final TimeZone timeZone)
  {
    final String[] sa = splitExDates(csv);
    if (sa == null) {
      return null;
    }
    final List<net.fortuna.ical4j.model.Date> result = new ArrayList<net.fortuna.ical4j.model.Date>();
    for (final String str : sa) {
      if (str == null) {
        continue;
      }
      Date date = null;
      if (str.matches("\\d{8}.*") == true) {
        date = parseICalDateString(str, timeZone);
      } else {
        date = parseISODateString(str);
      }
      if (date == null) {
        continue;
      }
      result.add(getICal4jDateTime(date, timeZone));
    }
    return result;
  }

  private static class MyIcal4JDate extends net.fortuna.ical4j.model.Date
  {
    private static final long serialVersionUID = 341788808291157447L;

    MyIcal4JDate(final java.util.Date javaDate, final java.util.TimeZone timeZone)
    {
      super(javaDate.getTime(), Dates.PRECISION_DAY, timeZone);
    }
  }

  public static String getICal(final TeamEventDO teamEvent) {
    final StringBuffer buf = new StringBuffer();
    final Calendar calendar = new Calendar();
    calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
    calendar.getProperties().add(Version.VERSION_2_0);
    calendar.getProperties().add(CalScale.GREGORIAN);
    final VEvent vEvent = ICal4JUtils.createVEvent(teamEvent.getStartDate(), teamEvent.getEndDate(), teamEvent.getUid(), teamEvent.getSubject(), teamEvent.isAllDay());
    if (teamEvent.hasRecurrence() == true) {
      vEvent.getProperties().add(new RRule(teamEvent.getRecurrenceObject()));
    }
    if (StringUtils.isNotBlank(teamEvent.getLocation()) == true) {
      vEvent.getProperties().add(new Location(teamEvent.getLocation()));
    }
    if (StringUtils.isNotBlank(teamEvent.getNote()) == true) {
      vEvent.getProperties().add(new Comment(teamEvent.getNote()));
    }
    final PFUserDO user = PFUserContext.getUser();
    final String s = user.getFullname() + "\\, " + user.getOrganization() + "\\, " + user.getPersonalPhoneIdentifiers();
    vEvent.getProperties().add(new Contact(s.intern()));
    try {
      if (StringUtils.isNotBlank(user.getEmail()) == true) {
        final ParameterList organizerParams = new ParameterList();
        organizerParams.add(new Cn(user.getFullname()));
        final Organizer organizer = new Organizer(organizerParams, user.getEmail());
        vEvent.getProperties().add(organizer);
      }
    } catch (final Exception e) {
      log.error("Cant't build organizer " + e.getMessage());
    }
    if (teamEvent.getAttendees() != null) {
      for (final TeamEventAttendeeDO attendee : teamEvent.getAttendees() ) {
        final ParameterList attendeeParams = new ParameterList();
        if (attendee.getUser() != null) {
          try {
            attendeeParams.add(new Cn(attendee.getUser().getFullname()));
            attendeeParams.add(new PartStat(attendee.getStatus().name()));
            vEvent.getProperties().add(new Attendee(attendeeParams, attendee.getUser().getEmail()));
          } catch (final Exception e) {
            log.error("Cant't build attendee " + e.getMessage());
          }
        } else {
          try {
            attendeeParams.add(new PartStat(attendee.getStatus().name()));
            vEvent.getProperties().add(new Attendee(attendeeParams, attendee.getUrl()));
          }  catch (final Exception e) {
            log.error("Cant't build attendee " + e.getMessage());
          }
        }
      }
    }
    calendar.getComponents().add(vEvent);
    buf.append(calendar.toString());
    return buf.toString();
  }
}
