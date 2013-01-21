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

package org.projectforge.web.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.NodeBorder;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.NodeModel;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.access.AccessChecker;
import org.projectforge.fibu.AuftragsPositionVO;
import org.projectforge.task.TaskDao;
import org.projectforge.task.TaskFilter;
import org.projectforge.task.TaskNode;
import org.projectforge.task.TaskTree;
import org.projectforge.user.ProjectForgeGroup;
import org.projectforge.user.UserGroupCache;
import org.projectforge.web.calendar.DateTimeFormatter;
import org.projectforge.web.core.PriorityFormatter;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.fibu.OrderPositionsPanel;
import org.projectforge.web.user.UserFormatter;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DatePropertyColumn;
import org.projectforge.web.wicket.ListSelectActionPanel;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TaskTreeBuilder implements Serializable
{
  private static final long serialVersionUID = -2425308275690643856L;

  private final TaskTree taskTree;

  private final Behavior theme = new WindowsTheme();

  private Integer highlightedTaskNodeId;

  private boolean selectMode, showRootNode, showCost, showOrders;

  private AccessChecker accessChecker;

  private TaskFormatter taskFormatter;

  private PriorityFormatter priorityFormatter;

  private UserFormatter userFormatter;

  private DateTimeFormatter dateTimeFormatter;

  private UserGroupCache userGroupCache;

  private TaskDao taskDao;

  private TableTree<TaskNode, String> tree;

  private AbstractSecuredPage parentPage;

  private ISelectCallerPage caller;

  private String selectProperty;

  /**
   * @param id
   */
  public TaskTreeBuilder(final TaskTree taskTree)
  {
    this.taskTree = taskTree;
  }

  @SuppressWarnings("serial")
  public AbstractTree<TaskNode> createTree(final String id, final AbstractSecuredPage parentPage, final TaskFilter taskFilter)
  {
    this.parentPage = parentPage;
    final List<IColumn<TaskNode, String>> columns = createColumns();

    tree = new TableTree<TaskNode, String>(id, columns, new TaskTreeProvider(taskTree, taskDao, taskFilter).setShowRootNode(showRootNode),
        Integer.MAX_VALUE, TaskTreeExpansion.getExpansionModel()) {
      private static final long serialVersionUID = 1L;

      @Override
      protected Component newContentComponent(final String id, final IModel<TaskNode> model)
      {
        return TaskTreeBuilder.this.newContentComponent(id, this, model);
      }

      @Override
      protected Item<TaskNode> newRowItem(final String id, final int index, final IModel<TaskNode> model)
      {
        return new OddEvenItem<TaskNode>(id, index, model);
      }
    };
    tree.getTable().addTopToolbar(new HeadersToolbar<String>(tree.getTable(), null));
    tree.getTable().addBottomToolbar(new NoRecordsToolbar(tree.getTable()));
    tree.add(new Behavior() {
      @Override
      public void onComponentTag(final Component component, final ComponentTag tag)
      {
        theme.onComponentTag(component, tag);
      }

      @Override
      public void renderHead(final Component component, final IHeaderResponse response)
      {
        theme.renderHead(component, response);
      }
    });
    tree.getTable().add(AttributeModifier.append("class", "tableTree"));
    return tree;
  }

  /**
   * @return
   */
  @SuppressWarnings("serial")
  private List<IColumn<TaskNode, String>> createColumns()
  {
    final CellItemListener<TaskNode> cellItemListener = new CellItemListener<TaskNode>() {
      public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId, final IModel<TaskNode> rowModel)
      {
        final TaskNode taskNode = rowModel.getObject();
        final String cssClasses = TaskListPage.getCssClasses(taskNode.getTask(), highlightedTaskNodeId);
        if (cssClasses != null) {
          item.add(AttributeModifier.append("class", cssClasses));
        }
      }
    };
    final List<IColumn<TaskNode, String>> columns = new ArrayList<IColumn<TaskNode, String>>();

    columns.add(new CellItemListenerPropertyColumn<TaskNode>(Model.of(""), null, "id", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId, final IModel<TaskNode> rowModel)
      {
        final TaskNode taskNode = rowModel.getObject();
        if (selectMode == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, TaskEditPage.class, taskNode.getId(), parentPage, ""));
        } else {
          item.add(new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, taskNode.getId(), ""));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        AbstractListPage.addRowClick(item);
      }
    });
    columns.add(new TreeColumn<TaskNode, String>(new ResourceModel("task")) {
      @Override
      public void populateItem(final Item<ICellPopulator<TaskNode>> cellItem, final String componentId, final IModel<TaskNode> rowModel)
      {
        final NodeModel<TaskNode> nodeModel = (NodeModel<TaskNode>) rowModel;
        final Component nodeComponent = getTree().newNodeComponent(componentId, nodeModel.getWrappedModel());
        nodeComponent.add(new NodeBorder(nodeModel.getBranches()));
        cellItem.add(nodeComponent);
        cellItemListener.populateItem(cellItem, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("task.consumption"), null, "task", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId, final IModel<TaskNode> rowModel)
      {
        final TaskNode node = rowModel.getObject();
        item.add(TaskListPage.getConsumptionBarPanel(tree, componentId, taskTree, selectMode, node));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    if (showCost == true) {
      columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("fibu.kost2"), null, "task.kost2", cellItemListener) {
        @Override
        public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId, final IModel<TaskNode> rowModel)
        {
          final Label label = TaskListPage.getKostLabel(componentId, taskTree, rowModel.getObject().getTask());
          item.add(label);
          cellItemListener.populateItem(item, componentId, rowModel);
        }
      });
    }
    if (taskTree.hasOrderPositionsEntries() == true && showOrders == true) {
      columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("fibu.auftrag.auftraege"), null, null, cellItemListener) {
        @Override
        public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId, final IModel<TaskNode> rowModel)
        {
          final TaskNode taskNode = rowModel.getObject();
          final Set<AuftragsPositionVO> orderPositions = taskTree.getOrderPositionEntries(taskNode.getId());
          if (CollectionUtils.isEmpty(orderPositions) == true) {
            final Label label = new Label(componentId, ""); // Empty label.
            item.add(label);
          } else {
            final OrderPositionsPanel orderPositionsPanel = new OrderPositionsPanel(componentId) {
              @Override
              protected void onBeforeRender()
              {
                super.onBeforeRender();
                // Lazy initialization because getString(...) of OrderPositionsPanel fails if panel.init(orderPositions) is called directly
                // after instantiation.
                init(orderPositions);
              };
            };
            item.add(orderPositionsPanel);
          }
          cellItemListener.populateItem(item, componentId, rowModel);
        }
      });
    }
    columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("shortDescription"), null, "task.shortDescription",
        cellItemListener));
    if (accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP) == true) {
      columns.add(new DatePropertyColumn<TaskNode>(dateTimeFormatter, parentPage.getString("task.protectTimesheetsUntil.short"), null,
          "task.protectTimesheetsUntil", cellItemListener));
    }
    columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("task.reference"), null, "reference", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("priority"), null, "priority", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId, final IModel<TaskNode> rowModel)
      {
        final Label label = TaskListPage.getPriorityLabel(componentId, priorityFormatter, rowModel.getObject().getTask());
        item.add(label);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TaskNode>(new ResourceModel("task.status"), null, "status", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<TaskNode>> item, final String componentId, final IModel<TaskNode> rowModel)
      {
        final Label label = TaskListPage.getStatusLabel(componentId, taskFormatter, rowModel.getObject().getTask());
        item.add(label);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    final UserPropertyColumn<TaskNode> userPropertyColumn = new UserPropertyColumn<TaskNode>(parentPage.getString("task.assignedUser"),
        null, "task.responsibleUserId", cellItemListener).withUserFormatter(userFormatter).setUserGroupCache(userGroupCache);
    columns.add(userPropertyColumn);
    return columns;
  }

  protected void addColumn(final WebMarkupContainer parent, final Component component, final String cssStyle)
  {
    if (cssStyle != null) {
      component.add(AttributeModifier.append("style", new Model<String>(cssStyle)));
    }
    parent.add(component);
  }

  /**
   * @param id
   * @param model
   * @return
   */
  @SuppressWarnings("serial")
  protected Component newContentComponent(final String id, final TableTree<TaskNode, String> tree, final IModel<TaskNode> model)
  {
    return new Folder<TaskNode>(id, tree, model) {

      @Override
      protected IModel< ? > newLabelModel(final IModel<TaskNode> model)
      {
        return new PropertyModel<String>(model, "task.title");
      }
    };
  }

  /**
   * @param selectMode the selectMode to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setSelectMode(final boolean selectMode)
  {
    this.selectMode = selectMode;
    return this;
  }

  /**
   * @param showRootNode the showRootNode to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setShowRootNode(final boolean showRootNode)
  {
    this.showRootNode = showRootNode;
    return this;
  }

  /**
   * @param showCost the showCost to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setShowCost(final boolean showCost)
  {
    this.showCost = showCost;
    return this;
  }

  /**
   * @param showOrders the showOrders to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setShowOrders(final boolean showOrders)
  {
    this.showOrders = showOrders;
    return this;
  }

  public TaskTreeBuilder set(final AccessChecker accessChecker, final TaskDao taskDao, final TaskFormatter taskFormatter,
      final PriorityFormatter priorityFormatter, final UserFormatter userFormatter, final DateTimeFormatter dateTimeFormatter,
      final UserGroupCache userGroupCache)
  {
    this.accessChecker = accessChecker;
    this.taskDao = taskDao;
    this.taskFormatter = taskFormatter;
    this.priorityFormatter = priorityFormatter;
    this.userFormatter = userFormatter;
    this.dateTimeFormatter = dateTimeFormatter;
    this.userGroupCache = userGroupCache;
    this.dateTimeFormatter = dateTimeFormatter;
    return this;
  }

  /**
   * @param caller the caller to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setCaller(final ISelectCallerPage caller)
  {
    this.caller = caller;
    return this;
  }

  /**
   * @param selectProperty the selectProperty to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setSelectProperty(final String selectProperty)
  {
    this.selectProperty = selectProperty;
    return this;
  }

  /**
   * @param highlightedTaskNodeId the highlightedTaskNodeId to set
   * @return this for chaining.
   */
  public TaskTreeBuilder setHighlightedTaskNodeId(final Integer highlightedTaskNodeId)
  {
    this.highlightedTaskNodeId = highlightedTaskNodeId;
    return this;
  }
}
