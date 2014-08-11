/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.teamcal.event;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class TeamEventMailValue
{
  private final Integer id;

  private final TeamEventMailType type;

  private final Integer orgId;

  public TeamEventMailValue(final Integer id, final TeamEventMailType type, final Integer orgId) {
    this.id = id;
    this.type = type;
    this.orgId = orgId;
  }

  public Integer getId()
  {
    return id;
  }

  public TeamEventMailType getType()
  {
    return type;
  }

  public Integer getOrgId()
  {
    return orgId;
  }
}
