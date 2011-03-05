/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.user;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.Query;
import org.projectforge.access.AccessChecker;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class UserXmlPreferencesMigrationDao extends HibernateDaoSupport
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserXmlPreferencesMigrationDao.class);

  private AccessChecker accessChecker;

  private UserGroupCache userGroupCache;

  private UserXmlPreferencesDao userXmlPreferencesDao;

  private UserXmlPreferencesCache userXmlPreferencesCache;

  public void setAccessChecker(AccessChecker accessChecker)
  {
    this.accessChecker = accessChecker;
  }

  public void setUserGroupCache(UserGroupCache userGroupCache)
  {
    this.userGroupCache = userGroupCache;
  }

  public void setUserXmlPreferencesCache(UserXmlPreferencesCache userXmlPreferencesCache)
  {
    this.userXmlPreferencesCache = userXmlPreferencesCache;
  }

  public void setUserXmlPreferencesDao(UserXmlPreferencesDao userXmlPreferencesDao)
  {
    this.userXmlPreferencesDao = userXmlPreferencesDao;
  }

  @SuppressWarnings("unchecked")
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public String migrateAllUserPrefs()
  {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    final StringBuffer buf = new StringBuffer();
    final List<UserXmlPreferencesDO> list = getHibernateTemplate().find(
        "from " + UserXmlPreferencesDO.class.getSimpleName() + " t order by userId, key");
    int versionNumber = Integer.MAX_VALUE;
    for (final UserXmlPreferencesDO userPrefs : list) {
      buf.append(migrateUserPrefs(userPrefs));
      if (userPrefs.getVersion() < versionNumber) {
        versionNumber = userPrefs.getVersion();
      }
    }
    migrate(versionNumber);
    userXmlPreferencesCache.refresh();
    return buf.toString();
  }

  /**
   * Here you can insert update or delete statements for all user xml pref entries (e. g. delete all entries with an unused key).
   * @param version Version number of oldest entry.
   */
  protected void migrate(final int version)
  {
    if (version < 4) {
      // deleteOldKeys("org.projectforge.web.humanresources.HRViewForm:Filter");
      // deleteOldKeys("org.projectforge.web.fibu.AuftragListAction:Filter");
      // deleteOldKeys("OLD-VERSION-1.1");
      // getHibernateTemplate().flush();
    }
  }

  /**
   * Unsupported or unused keys should be deleted. This method deletes all entries with the given key.
   * @param key Key of the entries to delete.
   */
  protected void deleteOldKeys(final String key)
  {
    final Query query = getHibernateTemplate().getSessionFactory().getCurrentSession().createQuery(
        "delete from " + UserXmlPreferencesDO.class.getSimpleName() + " where key = '" + key + "'");
    final int numberOfUpdatedEntries = query.executeUpdate();
    log.info(numberOfUpdatedEntries + " '" + key + "' entries deleted.");
  }

  protected String migrateUserPrefs(final UserXmlPreferencesDO userPrefs)
  {
    final Integer userId = userPrefs.getUserId();
    Validate.notNull(userId);
    final StringBuffer buf = new StringBuffer();
    buf.append("Checking user preferences for user '");
    final PFUserDO user = userGroupCache.getUser(userPrefs.getUserId());
    if (user != null) {
      buf.append(user.getUsername());
    } else {
      buf.append(userPrefs.getUserId());
    }
    buf.append("': " + userPrefs.getKey() + " ... ");
    if (userPrefs.getVersion() >= UserXmlPreferencesDO.CURRENT_VERSION) {
      buf.append("version ").append(userPrefs.getVersion()).append(" (up to date)\n");
      return buf.toString();
    }
    migrate(userPrefs);
    final Object data = userXmlPreferencesDao.deserialize(userPrefs, true);
    buf.append("version ");
    buf.append(userPrefs.getVersion());
    if (data != null || "<null/>".equals(userPrefs.getSerializedSettings()) == true) {
      buf.append(" OK ");
    } else {
      buf.append(" ***not re-usable*** ");
    }
    buf.append("\n");
    if (data == null) {
      return buf.toString();
    }
    return buf.toString();
  }

  /**
   * Fixes incompatible versions of user preferences before de-serialization.
   * @param userPrefs
   */
  protected static void migrate(final UserXmlPreferencesDO userPrefs)
  {
    String s = userPrefs.getSerializedSettings();
    if (userPrefs.getVersion() < 4) {
      boolean modified = false;
      if (s.contains("de.micromata.projectforge") == true) {
        s = StringUtils.replace(s, "de.micromata.projectforge", "org.projectforge");
        modified = true;
      }
      if (s.contains("de.micromata.fibu") == true) {
        s = StringUtils.replace(s, "de.micromata.fibu", "org.projectforge.fibu");
        modified = true;
      }
      if (modified == true) {
        userPrefs.setSerializedSettings(s);
      }
      s = userPrefs.getKey();
      if (s.contains("de.micromata.projectforge") == true) {
        s = StringUtils.replace(s, "de.micromata.projectforge", "org.projectforge");
        userPrefs.setKey(s);
        modified = true;
      } else if (s.contains("de.micromata.fibu") == true) {
        s = StringUtils.replace(s, "de.micromata.fibu", "org.projectforge.fibu");
        userPrefs.setKey(s);
        modified = true;
      }
      if (modified == true) {
        log.info("User settings migrated for user " + userPrefs.getUserId() + ": " + userPrefs.getKey());
      }
      userPrefs.setVersion(4);
    }
  }
}
