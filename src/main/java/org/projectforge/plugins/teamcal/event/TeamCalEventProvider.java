/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.plugins.teamcal.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.ftlines.wicket.fullcalendar.Event;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.projectforge.plugins.teamcal.admin.TeamCalDO;
import org.projectforge.plugins.teamcal.admin.TeamCalDao;
import org.projectforge.plugins.teamcal.admin.TeamCalRight;
import org.projectforge.plugins.teamcal.integration.TeamCalCalendarFilter;
import org.projectforge.plugins.teamcal.integration.TemplateEntry;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.PFUserDO;
import org.projectforge.user.UserGroupCache;
import org.projectforge.web.calendar.MyFullCalendarEventsProvider;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class TeamCalEventProvider extends MyFullCalendarEventsProvider
{

  private static final long serialVersionUID = -5609599079385073490L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalEventProvider.class);

  private final TeamEventDao teamEventDao;

  private int days;

  private final TeamCalCalendarFilter filter;

  /**
   * the name of the event class.
   */
  public static final String EVENT_CLASS_NAME = "teamEvent";

  private final TeamEventRight eventRight;

  /**
   * @param parent component for i18n
   */
  public TeamCalEventProvider(final Component parent, final TeamEventDao teamEventDao, final UserGroupCache userGroupCache,
      final TeamCalCalendarFilter filter)
  {
    super(parent);
    this.filter = filter;
    this.teamEventDao = teamEventDao;
    this.eventRight = new TeamEventRight();
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#getEvents(org.joda.time.DateTime, org.joda.time.DateTime)
   */
  @Override
  public Collection<Event> getEvents(final DateTime start, final DateTime end)
  {
    final Collection<Event> events = super.getEvents(start, end);
    return events;
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime, org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end)
  {
    final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
    if (activeTemplateEntry == null) {
      // Nothing to build.
      return;
    }
    final Set<Integer> selectedCalendars = activeTemplateEntry.getVisibleCalendarIds();
    if (CollectionUtils.isEmpty(selectedCalendars) == true) {
      // Nothing to build.
      return;
    }
    final TeamEventFilter eventFilter = new TeamEventFilter();
    eventFilter.setStartDate(start.toDate());
    eventFilter.setEndDate(end.toDate());
    eventFilter.setUser(PFUserContext.getUser());

    final List<List<TeamEventDO>> eventLists = new ArrayList<List<TeamEventDO>>();
    if (selectedCalendars != null) {
      for (final Integer calendarId : selectedCalendars) {
        eventFilter.setTeamCalId(calendarId);
        eventLists.add(teamEventDao.getList(eventFilter));
      }
    }

    boolean longFormat = false;
    days = Days.daysBetween(start, end).getDays();
    if (days < 10) {
      // Week or day view:
      longFormat = true;
    }

    final TeamCalRight right = new TeamCalRight();
    final PFUserDO user = PFUserContext.getUser();
    if (CollectionUtils.isNotEmpty(eventLists) == true) {
      for (final List<TeamEventDO> teamEvents : eventLists) {
        for (final TeamEventDO teamEvent : teamEvents) {
          final DateTime startDate = new DateTime(teamEvent.getStartDate(), PFUserContext.getDateTimeZone());
          final DateTime endDate = new DateTime(teamEvent.getEndDate(), PFUserContext.getDateTimeZone());
          if (endDate.isBefore(start) == true || startDate.isAfter(end) == true) {
            // Event doesn't match time period start - end.
            continue;
          }

          final Event event = new Event();
          event.setClassName(EVENT_CLASS_NAME);
          event.setId("" + teamEvent.getId());
          event.setColor(activeTemplateEntry.getColorCode(teamEvent.getCalendarId()));

          if (eventRight.hasUpdateAccess(PFUserContext.getUser(), teamEvent, null)) {
            event.setEditable(true);
          } else {
            event.setEditable(false);
          }

          if (teamEvent.isAllDay()) {
            event.setAllDay(true);
          }

          event.setStart(startDate);
          event.setEnd(endDate);

          final String title;
          String durationString = "";
          if (longFormat == true) {
            final Period duration = new Period(startDate, endDate);
            // String day = duration.getDays() + "";

            int hourInt = duration.getHours();
            if (duration.getDays() > 0) {
              hourInt += duration.getDays() * 24;
            }
            final String hour = hourInt < 10 ? "0" + hourInt : "" + hourInt;

            final int minuteInt = duration.getMinutes();
            final String minute = minuteInt < 10 ? "0" + minuteInt : "" + minuteInt;

            if (event.isAllDay() == false)
              durationString = "\n" + getString("plugins.teamevent.duration") + ": " + hour + ":" + minute;
            final StringBuffer buf = new StringBuffer();
            buf.append(getString("plugins.teamevent.subject")).append(": ").append(teamEvent.getSubject());
            if (StringUtils.isNotBlank(teamEvent.getNote()) == true) {
              buf.append("\n").append(getString("plugins.teamevent.note")).append(": ").append(teamEvent.getNote());
            }
            buf.append(durationString);
            title = buf.toString();
          } else {
            title = teamEvent.getSubject();
          }
          if (right.hasMinimalAccess(teamEvent.getCalendar(), user.getId()) == true) {
            // for minimal access
            event.setTitle("");
            event.setEditable(false);
          } else {
            event.setTitle(title);
          }
          events.put(teamEvent.getId() + "", event);
        }
      }
    }
  }

  /**
   * @param selectedCalendar
   * @return
   */
  public static TeamCalDO getTeamCalForEncodedId(final TeamCalDao teamCalDao, String selectedCalendar)
  {
    if (selectedCalendar == null) {
      return null;
    }
    if (selectedCalendar.contains("-")) {
      selectedCalendar = selectedCalendar.substring(selectedCalendar.indexOf("-") + 1);
    }
    try {
      return teamCalDao.getById(Integer.valueOf(selectedCalendar));
    } catch (final NumberFormatException ex) {
      log.warn("Unable to get teamCalDao for id " + selectedCalendar);
    }
    return null;
  }

}
