/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.addresses;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.addresses.AddressFilter;
import org.projectforge.plugins.skillmatrix.SkillDao;
import org.projectforge.plugins.skillmatrix.TrainingDao;
import org.projectforge.web.wicket.AbstractListForm;

/**
 * The list formular for the list view.
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class AddressListForm extends AbstractListForm<AddressFilter, AddressListPage> implements Serializable
{

  private static final long serialVersionUID = 2352857862306753080L;

  private static final Logger log = Logger.getLogger(AddressListForm.class);

  @SpringBean(name = "trainingDao")
  private TrainingDao trainingDao;

  @SpringBean(name = "skillDao")
  private SkillDao skillDao;

  /**
   * @param parentPage
   */
  public AddressListForm(final AddressListPage parentPage)
  {
    super(parentPage);
  }


  @Override
  protected void init()
  {
    super.init();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#newSearchFilterInstance()
   */
  @Override
  protected AddressFilter newSearchFilterInstance()
  {
    return new AddressFilter();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

}