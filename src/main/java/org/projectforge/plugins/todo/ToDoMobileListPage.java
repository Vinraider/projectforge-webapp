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

package org.projectforge.plugins.todo;

import org.apache.wicket.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.mobile.AbstractMobileListPage;

public class ToDoMobileListPage extends AbstractMobileListPage<ToDoMobileListForm, ToDoDao, ToDoDO>
{
  private static final long serialVersionUID = -5695046272840387231L;

  @SpringBean(name = "toDoDao")
  private ToDoDao toDoDao;

  public ToDoMobileListPage(final PageParameters parameters)
  {
    super("toDo", parameters);
  }

  @Override
  protected ToDoDao getBaseDao()
  {
    return toDoDao;
  }

  @Override
  protected ToDoMobileListForm newListForm(AbstractMobileListPage< ? , ? , ? > parentPage)
  {
    return new ToDoMobileListForm(this);
  }

  @Override
  protected String getEntryName(final ToDoDO entry)
  {
    return entry.getSubject();
  }

  @Override
  protected String getEntryComment(ToDoDO entry)
  {
    return entry.getAssignee() != null ? entry.getAssignee().getFullname() : "";
  }
}
