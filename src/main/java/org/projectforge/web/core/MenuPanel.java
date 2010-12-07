/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2010 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.web.core;

import org.apache.wicket.PageParameters;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.common.NumberHelper;
import org.projectforge.user.PFUserContext;
import org.projectforge.web.Menu;
import org.projectforge.web.MenuBuilder;
import org.projectforge.web.MenuEntry;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.WicketUtils;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MenuPanel extends Panel
{
  private static final long serialVersionUID = -7858806882044188339L;

  private static final String USER_PREF_MENU_KEY = "usersMenu";

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MenuPanel.class);

  @SpringBean(name = "menuBuilder")
  private MenuBuilder menuBuilder;

  private Menu menu;

  public MenuPanel(String id)
  {
    super(id);
    add(CSSPackageResource.getHeaderContribution("scripts/jquery-ui-1-2.8.2.custom/css/custom-theme/jquery-ui-.custom.css"));
    add(JavascriptPackageResource.getHeaderContribution("scripts/jquery-ui-1-2.8.2.custom/js/jquery-ui-1.8.2.custom.min.js"));
    add(JavascriptPackageResource.getHeaderContribution("scripts/menu.js"));
    // should be included in jqueryui, uncomment if something is missing
    // add(JavascriptPackageResource.getHeaderContribution("scripts/jquery.dimensions.min.js"));
  }

  public void init()
  {
    final RepeatingView menuAreaRepeater = new RepeatingView("menuAreaRepeater");
    add(menuAreaRepeater);

    getMenu();
    int counter = 0;
    for (final MenuEntry menuArea : menu.getMenuEntries()) {
      if (menuArea.getSubMenuEntries() == null) {
        log.error("Oups: menu without sub menus not supported: " + menuArea.getId());
        continue;
      }
      final WebMarkupContainer menuAreaContainer = new WebMarkupContainer(menuAreaRepeater.newChildId());
      menuAreaRepeater.add(menuAreaContainer);
      final WebMarkupContainer li = new WebMarkupContainer("li");
      menuAreaContainer.add(li);
      li.add(new Label("areaTitle", getString(menuArea.getI18nKey())));
      final Label areaSuffixLabel = getSuffixLabel(menuArea);
      li.add(areaSuffixLabel);
      final RepeatingView menuEntryRepeater = new RepeatingView("menuEntryRepeater");
      li.add(menuEntryRepeater);
      for (final MenuEntry menuEntry : menuArea.getSubMenuEntries()) {
        if (menuEntry.getSubMenuEntries() != null) {
          log.error("Oups: sub sub menus not supported: " + menuArea.getId() + " has child menus which are ignored.");
        }
        final WebMarkupContainer subLi = new WebMarkupContainer(menuEntryRepeater.newChildId());
        menuEntryRepeater.add(subLi);
        subLi.add(new SimpleAttributeModifier("id", menuEntry.getId()));
        if (menuEntry.isFirst() == true) {
          subLi.add(new SimpleAttributeModifier("class", "first"));
        }
        final AbstractLink link;
        if (menuEntry.isWicketPage() == true) {
          if (menuEntry.getParams() == null) {
            link = new BookmarkablePageLink<String>("link", menuEntry.getPageClass());
          } else {
            final PageParameters params = WicketUtils.getPageParameters(menuEntry.getParams());
            link = new BookmarkablePageLink<String>("link", menuEntry.getPageClass(), params);
          }
        } else {
          link = new ExternalLink("link", WicketUtils.getUrl(getResponse(), menuEntry.getUrl(), true));
        }
        if (menuEntry.isNewWindow() == true) {
          link.add(new SimpleAttributeModifier("target", "_blank"));
        }
        subLi.add(link);
        link.add(new Label("label", getString(menuEntry.getI18nKey())));
        final Label menuSuffixLabel = getSuffixLabel(menuEntry);
        link.add(menuSuffixLabel);
      }
      if (counter++ == 3) {
        final WebMarkupContainer tipRow = new WebMarkupContainer("tipRow");
        menuAreaContainer.add(tipRow);
        tipRow.add(new Label("tip", getString("menu.main.tip1")).setEscapeModelStrings(false));
      } else {
        menuAreaContainer.add(new Label("tipRow", "invisible").setVisible(false));
      }
    }
    final WebMarkupContainer container = new WebMarkupContainer(menuAreaRepeater.newChildId());
    menuAreaRepeater.add(container);
    final WebMarkupContainer li = new WebMarkupContainer("li");
    container.add(li.setVisible(false));
    final WebMarkupContainer tipRow = new WebMarkupContainer("tipRow");
    container.add(tipRow);
    tipRow.add(new Label("tip", getString("menu.main.tip2")).setEscapeModelStrings(false));
  }

  @SuppressWarnings("serial")
  private Label getSuffixLabel(final MenuEntry menuEntry)
  {
    final Label suffixLabel;
    if (menuEntry.getNewCounterModel() != null) {
      suffixLabel = new Label("suffix", new Model<String>() {
        @Override
        public String getObject()
        {
          final Integer counter = menuEntry.getNewCounterModel().getObject();
          if (NumberHelper.greaterZero(counter) == true) {
            return String.valueOf(counter);
          } else {
            return "";
          }
        }
      }) {
        @Override
        public boolean isVisible()
        {
          final Integer counter = menuEntry.getNewCounterModel().getObject();
          return NumberHelper.greaterZero(counter) == true;
        }
      };
    } else {
      suffixLabel = new Label("suffix");
      suffixLabel.setVisible(false);
    }
    if (menuEntry.getNewCounterTooltip() != null) {
      WicketUtils.addTooltip(suffixLabel, getString(menuEntry.getNewCounterTooltip()));
    }
    return suffixLabel;
  }

  private Menu getMenu()
  {
    AbstractSecuredPage securedPage = null;
    if (getPage() instanceof AbstractSecuredPage) {
      securedPage = ((AbstractSecuredPage) getPage());
      menu = (Menu) securedPage.getUserPrefEntry(USER_PREF_MENU_KEY);
      if (menu != null) {
        return menu;
      }
    }
    if (menu != null) { // After getting menu from user pref entry, because otherwise resetMenu() doesn't work if menu is stored in this
      // panel.
      return menu;
    }
    if (log.isDebugEnabled() == true) {
      log.debug("Build new menu.");
    }
    menu = menuBuilder.getMenu(PFUserContext.getUser());
    if (securedPage != null) {
      securedPage.putUserPrefEntry(USER_PREF_MENU_KEY, menu, false);
    }
    return menu;
  }

}
