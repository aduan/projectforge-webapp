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

package org.projectforge.web.core;

import java.util.Collection;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.access.AccessChecker;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.UserRights;
import org.projectforge.user.UserXmlPreferencesCache;
import org.projectforge.web.CustomizeMenuPage;
import org.projectforge.web.FavoritesMenu;
import org.projectforge.web.LayoutSettingsPage;
import org.projectforge.web.LoginPage;
import org.projectforge.web.MenuEntry;
import org.projectforge.web.mobile.MenuMobilePage;
import org.projectforge.web.user.ChangePasswordPage;
import org.projectforge.web.user.MyAccountEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.FeedbackPage;
import org.projectforge.web.wicket.MySession;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DialogPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * Displays the favorite menu.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class NavTopPanel extends NavAbstractPanel
{
  private static final long serialVersionUID = -7858806882044188339L;

  private static final String BOOKMARK_DIALOG_ID = "bookmarkModalWindow";

  private final FavoritesMenu favoritesMenu;

  private ModalWindow bookmarkModalWindow;

  private final AccessChecker accessChecker;

  private final UserXmlPreferencesCache userXmlPreferencesCache;

  public NavTopPanel(final String id, final UserXmlPreferencesCache userXmlPreferencesCache, final AccessChecker accessChecker)
  {
    super(id);
    this.userXmlPreferencesCache = userXmlPreferencesCache;
    this.accessChecker = accessChecker;
    this.favoritesMenu = FavoritesMenu.get(userXmlPreferencesCache, accessChecker);
  }

  public void init(final AbstractSecuredPage page)
  {
    if (page.getMySession().isMobileUserAgent() == true) {
      add(new BookmarkablePageLink<Void>("goMobile", MenuMobilePage.class));
    } else {
      add(new WebMarkupContainer("goMobile").setVisible(false));
    }
    final BookmarkablePageLink<Void> customizeMenuLink = new BookmarkablePageLink<Void>("customizeMenuLink", CustomizeMenuPage.class);
    final BookmarkablePageLink<Void> layoutSettingsMenuLink = new BookmarkablePageLink<Void>("layoutSettingsMenuLink",
        LayoutSettingsPage.class);
    if (UserRights.getAccessChecker().isRestrictedUser() == true) {
      // Not visibible for restricted users:
      customizeMenuLink.setVisible(false);
      layoutSettingsMenuLink.setVisible(false);
    }
    add(customizeMenuLink);
    add(layoutSettingsMenuLink);
    add(new BookmarkablePageLink<Void>("feedbackLink", FeedbackPage.class));
    {
      @SuppressWarnings("serial")
      final AjaxLink<Void> showBookmarkLink = new AjaxLink<Void>("showBookmarkLink") {
        /**
         * @see org.apache.wicket.ajax.markup.html.AjaxLink#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
         */
        @Override
        public void onClick(final AjaxRequestTarget target)
        {
          showBookmarkModalWindow(target);
        }
      };
      add(showBookmarkLink);
      bookmarkModalWindow = new ModalWindow(BOOKMARK_DIALOG_ID);
      bookmarkModalWindow.setInitialHeight(200);
      add(bookmarkModalWindow);
    }
    {
      add(new Label("user", PFUserContext.getUser().getFullname()));
      if (accessChecker.isRestrictedUser() == true) {
        // Show ChangePaswordPage as my account for restricted users.
        final BookmarkablePageLink<Void> changePasswordLink = new BookmarkablePageLink<Void>("myAccountLink", ChangePasswordPage.class);
        add(changePasswordLink);
      } else {
        final BookmarkablePageLink<Void> myAccountLink = new BookmarkablePageLink<Void>("myAccountLink", MyAccountEditPage.class);
        add(myAccountLink);
      }
      @SuppressWarnings("serial")
      final Link<String> logoutLink = new Link<String>("logoutLink") {
        @Override
        public void onClick()
        {
          LoginPage.logout((MySession) getSession(), (WebRequest) getRequest(), (WebResponse) getResponse(), userXmlPreferencesCache);
          setResponsePage(LoginPage.class);
        };
      };
      add(logoutLink);
    }
    getMenu();

    // Main menu:
    final RepeatingView menuRepeater = new RepeatingView("menuRepeater");
    add(menuRepeater);
    final Collection<MenuEntry> menuEntries = favoritesMenu.getMenuEntries();
    if (menuEntries != null) {
      for (final MenuEntry menuEntry : menuEntries) {
        // Now we add a new menu area (title with sub menus):
        final WebMarkupContainer menuItem = new WebMarkupContainer(menuRepeater.newChildId());
        menuRepeater.add(menuItem);
        final AbstractLink link = getMenuEntryLink(menuEntry, menuItem);
        if (link == null) {
          menuItem.setVisible(false);
          continue;
        }
        menuItem.add(link);

        final WebMarkupContainer subMenuContainer = new WebMarkupContainer("subMenu");
        menuItem.add(subMenuContainer);
        final WebMarkupContainer caret = new WebMarkupContainer("caret");
        link.add(caret);
        if (menuEntry.hasSubMenuEntries() == false) {
          subMenuContainer.setVisible(false);
          caret.setVisible(false);
          continue;
        }
        menuItem.add(AttributeModifier.append("class", "dropdown"));
        link.add(AttributeModifier.append("class", "dropdown-toggle"));
        link.add(AttributeModifier.append("data-toggle", "dropdown"));
        final RepeatingView subMenuRepeater = new RepeatingView("subMenuRepeater");
        subMenuContainer.add(subMenuRepeater);
        for (final MenuEntry subMenuEntry : menuEntry.getSubMenuEntries()) {
          // Now we add the next menu entry to the area:
          if (subMenuEntry.hasSubMenuEntries() == false) {
            final WebMarkupContainer subMenuItem = new WebMarkupContainer(subMenuRepeater.newChildId());
            subMenuRepeater.add(subMenuItem);
            // Subsubmenu entries aren't yet supported, show only the sub entries without children, otherwise only the children are
            // displayed.
            final AbstractLink subLink = getMenuEntryLink(subMenuEntry, subMenuItem);
            if (subLink == null) {
              subMenuItem.setVisible(false);
              continue;
            }
            subMenuItem.add(subLink);
            continue;
          }

          // final WebMarkupContainer subsubMenuContainer = new WebMarkupContainer("subsubMenu");
          // subMenuItem.add(subsubMenuContainer);
          // if (subMenuEntry.hasSubMenuEntries() == false) {
          // subsubMenuContainer.setVisible(false);
          // continue;
          // }
          // final RepeatingView subsubMenuRepeater = new RepeatingView("subsubMenuRepeater");
          // subsubMenuContainer.add(subsubMenuRepeater);
          for (final MenuEntry subsubMenuEntry : subMenuEntry.getSubMenuEntries()) {
            // Now we add the next menu entry to the sub menu:
            final WebMarkupContainer subMenuItem = new WebMarkupContainer(subMenuRepeater.newChildId());
            subMenuRepeater.add(subMenuItem);
            // Subsubmenu entries aren't yet supported, show only the sub entries without children, otherwise only the children are
            // displayed.
            final AbstractLink subLink = getMenuEntryLink(subsubMenuEntry, subMenuItem);
            if (subLink == null) {
              subMenuItem.setVisible(false);
              continue;
            }
            subMenuItem.add(subLink);
            // final WebMarkupContainer subsubMenuItem = new WebMarkupContainer(subsubMenuRepeater.newChildId());
            // subsubMenuRepeater.add(subsubMenuItem);
            // final AbstractLink subsubLink = getMenuEntryLink(subsubMenuEntry, subsubMenuItem);
            // subsubMenuItem.add(subsubLink);
          }
        }
      }
    }
  }

  @SuppressWarnings("serial")
  protected void showBookmarkModalWindow(final AjaxRequestTarget target)
  {
    // Close dialog
    final DialogPanel closeDialog = new DialogPanel(bookmarkModalWindow, getString("bookmark.title"));
    bookmarkModalWindow.setContent(closeDialog);

    final DivPanel content = new DivPanel(closeDialog.newChildId());
    closeDialog.add(content);
    FieldsetPanel fs = new FieldsetPanel(content, getString("bookmark.directPageLink")).setLabelSide(false);
    final AbstractSecuredPage page = (AbstractSecuredPage) getPage();
    fs.add(new TextArea<String>(fs.getTextAreaId(), new Model<String>(page.getPageAsLink())));
    final PageParameters params = page.getBookmarkableInitialParameters();
    if (params.isEmpty() == false) {
      fs = new FieldsetPanel(content, getString(page.getTitleKey4BookmarkableInitialParameters())).setLabelSide(false);
      fs.add(new TextArea<String>(fs.getTextAreaId(), new Model<String>(page.getPageAsLink(params))));
      bookmarkModalWindow.setInitialHeight(400);
    }

    final AjaxButton closeButton = new AjaxButton(SingleButtonPanel.WICKET_ID, new Model<String>("close")) {

      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form< ? > form)
      {
        bookmarkModalWindow.close(target);
      }

      /**
       * @see org.apache.wicket.ajax.markup.html.form.AjaxButton#onError(org.apache.wicket.ajax.AjaxRequestTarget,
       *      org.apache.wicket.markup.html.form.Form)
       */
      @Override
      protected void onError(final AjaxRequestTarget target, final Form< ? > form)
      {
      }
    };
    closeButton.setDefaultFormProcessing(false); // No validation
    final SingleButtonPanel closeButtonPanel = new SingleButtonPanel(closeDialog.newButtonChildId(), closeButton, getString("close"),
        SingleButtonPanel.DEFAULT_SUBMIT);
    closeDialog.addButton(closeButtonPanel);

    bookmarkModalWindow.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
      public boolean onCloseButtonClicked(final AjaxRequestTarget target)
      {
        return true;
      }
    });
    bookmarkModalWindow.show(target);
  }
}
